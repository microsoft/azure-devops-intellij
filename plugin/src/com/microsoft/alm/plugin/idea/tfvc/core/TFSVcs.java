// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.google.common.util.concurrent.SettableFuture;
import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.util.BackgroundTaskUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.CommittedChangesProvider;
import com.intellij.openapi.vcs.EditFileProvider;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsConfiguration;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsKey;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vcs.VcsRoot;
import com.intellij.openapi.vcs.VcsShowConfirmationOption;
import com.intellij.openapi.vcs.VcsShowSettingOption;
import com.intellij.openapi.vcs.VcsVFSListener;
import com.intellij.openapi.vcs.changes.ChangeProvider;
import com.intellij.openapi.vcs.diff.DiffProvider;
import com.intellij.openapi.vcs.history.VcsHistoryProvider;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vcs.rollback.RollbackEnvironment;
import com.intellij.openapi.vcs.roots.VcsRootDetector;
import com.intellij.openapi.vcs.update.UpdateEnvironment;
import com.intellij.openapi.vcs.versionBrowser.ChangeBrowserSettings;
import com.intellij.vcsUtil.VcsUtil;
import com.microsoft.alm.plugin.context.RepositoryContext;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.external.exceptions.SyncException;
import com.microsoft.alm.plugin.external.exceptions.ToolException;
import com.microsoft.alm.plugin.external.tools.TfTool;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.services.LocalizationServiceImpl;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import com.microsoft.alm.plugin.idea.common.utils.VcsHelper;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsRevisionNumber;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.event.HyperlinkEvent;
import javax.ws.rs.NotAuthorizedException;
import java.util.Collection;

/**
 * Class that sets up the TFS version control extension.
 */
public class TFSVcs extends AbstractVcs {
    public static final Logger logger = LoggerFactory.getLogger(TFSVcs.class);
    private static boolean hasVersionBeenVerified = false;

    @NonNls
    public static final String TFVC_NAME = "TFVC";
    public static final String TFVC_ONLINE_HELP_URL = "https://docs.microsoft.com/en-us/azure/devops/java/intellij-faq?view=azure-devops#does-the-intellij-plug-in-support-tfvc";
    public static final String SETTINGS_URL_EVENT = "settings";
    private static final VcsKey ourKey = createKey(TFVC_NAME);

    private final VcsShowConfirmationOption myAddConfirmation;
    private final VcsShowConfirmationOption myDeleteConfirmation;
    private final VcsShowSettingOption myCheckoutOptions;

    private VcsHistoryProvider myHistoryProvider;
    private DiffProvider myDiffProvider;
    private TFSCheckinEnvironment myCheckinEnvironment;
    private UpdateEnvironment myUpdateEnvironment;
    private VcsVFSListener fileListener;
    private TFSFileSystemListener tfsFileSystemListener;
    private CommittedChangesProvider<TFSChangeList, ChangeBrowserSettings> committedChangesProvider;
    private EditFileProvider myEditFileProvider;

    public TFSVcs(@NotNull Project project) {
        super(project, TFVC_NAME);
        final ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
        myAddConfirmation = vcsManager.getStandardConfirmation(VcsConfiguration.StandardConfirmation.ADD, this);
        myDeleteConfirmation = vcsManager.getStandardConfirmation(VcsConfiguration.StandardConfirmation.REMOVE, this);
        myCheckoutOptions = vcsManager.getStandardOption(VcsConfiguration.StandardOption.CHECKOUT, this);
    }

    public static TFSVcs getInstance(Project project) {
        return (TFSVcs) ProjectLevelVcsManager.getInstance(project).findVcsByName(TFVC_NAME);
    }

    @NonNls
    public String getDisplayName() {
        return TFVC_NAME;
    }

    public Configurable getConfigurable() {
        return new TFSProjectConfigurable(myProject);
    }

    @Override
    public void activate() {
        fileListener = new TFSFileListener(getProject(), this);
        if (tfsFileSystemListener == null) {
            tfsFileSystemListener = new TFSFileSystemListener(myProject);
        }

        checkCommandLineVersion();
    }

    @Override
    public void deactivate() {
        Disposer.dispose(fileListener);
        tfsFileSystemListener.dispose();
        tfsFileSystemListener = null;
    }

    @Override
    public void enableIntegration() {
        BackgroundTaskUtil.executeOnPooledThread(myProject, () -> {
            Collection<VcsRoot> roots = ServiceManager.getService(myProject, VcsRootDetector.class).detect();
            new TfvcIntegrationEnabler(this).enable(roots);
        });
    }

    public VcsShowConfirmationOption getAddConfirmation() {
        return myAddConfirmation;
    }

    public VcsShowConfirmationOption getDeleteConfirmation() {
        return myDeleteConfirmation;
    }

    public VcsShowSettingOption getCheckoutOptions() {
        return myCheckoutOptions;
    }

    public ChangeProvider getChangeProvider() {
        return new TFSChangeProvider(this);
    }

    @NotNull
    public TFSCheckinEnvironment createCheckinEnvironment() {
        if (myCheckinEnvironment == null) {
            myCheckinEnvironment = new TFSCheckinEnvironment(this);
        }
        return myCheckinEnvironment;
    }

    @NotNull
    public UpdateEnvironment createUpdateEnvironment() {
        if (myUpdateEnvironment == null) {
            myUpdateEnvironment = new TFSUpdateEnvironment(myProject, this);
        }
        return myUpdateEnvironment;
    }

    public RollbackEnvironment createRollbackEnvironment() {
        return new TFSRollbackEnvironment(this, myProject);
    }

    public boolean fileIsUnderVcs(final FilePath filePath) {
        return isVersionedDirectory(filePath.getVirtualFile());
    }

    /**
     * Overrides method from IDEA 2019.2 that will allow us to work without "new" root mappings.
     */
    public boolean needsLegacyDefaultMappings() {
        return true;
    }

    @NotNull
    public CommittedChangesProvider<TFSChangeList, ChangeBrowserSettings> getCommittedChangesProvider() {
        if (committedChangesProvider == null) {
            committedChangesProvider = new TFSCommittedChangesProvider(myProject);
        }
        return committedChangesProvider;
    }

    @Override
    public VcsHistoryProvider getVcsHistoryProvider() {
        if (myHistoryProvider == null) {
            myHistoryProvider = new TFSHistoryProvider(myProject);
        }
        return myHistoryProvider;
    }

    @Override
    public DiffProvider getDiffProvider() {
        if (myDiffProvider == null) {
            myDiffProvider = new TFSDiffProvider(myProject);
        }
        return myDiffProvider;
    }

    @NotNull
    @Override
    public EditFileProvider getEditFileProvider() {
        if (myEditFileProvider == null)
            myEditFileProvider = new TfvcEditFileProvider(this);
        return myEditFileProvider;
    }

    @Nullable
    public VcsRevisionNumber parseRevisionNumber(final String revisionNumberString) {
        return TfsRevisionNumber.tryParse(revisionNumberString);
    }

    @Nullable
    public String getRevisionPattern() {
        return ourIntegerPattern;
    }


    public static VcsKey getKey() {
        return ourKey;
    }

    public static boolean isUnderTFS(FilePath path, Project project) {
        AbstractVcs vcs = VcsUtil.getVcsFor(project, path);
        return vcs != null && TFVC_NAME.equals(vcs.getName());
    }

    public static VcsException convertToVcsException(final Throwable throwable) {
        if (throwable instanceof VcsException) {
            return (VcsException) throwable;
        }

        final VcsException exception = new VcsException(throwable.getMessage(), throwable);
        if (throwable instanceof SyncException) {
            exception.setIsWarning(((SyncException) throwable).isWarning());
        }

        return exception;
    }

    @Override
    public CheckoutProvider getCheckoutProvider() {
        return null; ///TODO: new TFSCheckoutProvider();
    }


    /**
     * This method is used by the environment classes to get the ServerContext.
     * We do not cache it here because it should already be cached in the ServerContextManager.
     */
    public ServerContext getServerContext(boolean throwIfNotFound) {
        final RepositoryContext repositoryContext = VcsHelper.getRepositoryContext(getProject());
        logger.info("TFSVcs.getServerContext repositoryContext is null: " + (repositoryContext == null));

        final ServerContext serverContext = repositoryContext != null
                && StringUtils.isNotEmpty(repositoryContext.getTeamProjectName())
                && StringUtils.isNotEmpty(repositoryContext.getUrl()) ?
                ServerContextManager.getInstance().createContextFromTfvcServerUrl(
                        repositoryContext.getUrl(), repositoryContext.getTeamProjectName(), true)
                : null;

        if (serverContext == null && throwIfNotFound) {
            // TODO: throw a better error b/c this is what the user sees and it's confusing
            throw new NotAuthorizedException(repositoryContext != null ? repositoryContext.getUrl() : "");
        }
        return serverContext;
    }

    private void checkCommandLineVersion() {
        if (hasVersionBeenVerified) {
            // No need to check the version again if we have already checked it once this session
            logger.info("Skipping the attempt to check the version of the TF command line.");
            return;
        }

        hasVersionBeenVerified = true;

        // We want to start a background thread to check the version, but that can only be done
        // form the UI thread.
        IdeaHelper.runOnUIThread(() -> {
            final SettableFuture<String> versionMessage = SettableFuture.create();
            (new Task.Backgroundable(getProject(), TfPluginBundle.message(TfPluginBundle.KEY_TFVC_TF_VERSION_WARNING_PROGRESS),
                    false, PerformInBackgroundOption.ALWAYS_BACKGROUND) {
                public void run(@NotNull final ProgressIndicator indicator) {
                    try {
                        logger.info("Attempting to check the version of the TF command line.");
                        TfTool.checkVersion();
                        versionMessage.set(StringUtils.EMPTY);
                    } catch (final ToolException ex) {
                        String error = ToolException.KEY_TF_EXE_NOT_FOUND.equals(ex.getMessageKey())
                            ? TfPluginBundle.message(TfPluginBundle.KEY_TOOLEXCEPTION_TF_HOME_NOT_SET) // more suitable for notification than the default message
                            : LocalizationServiceImpl.getInstance().getExceptionMessage(ex);
                        logger.warn(error);
                        versionMessage.set(error);
                    } catch (final Throwable t) {
                        // Don't let unknown errors bubble out here
                        logger.warn("Unexpected error when checking the version of the command line.", t);
                        versionMessage.set(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_TF_CANNOT_DETERMINE_VERSION_TEXT));
                    }
                }

                public void onSuccess() {
                    try {
                        final String error = versionMessage.get();
                        if (StringUtils.isNotEmpty(error)) {
                            logger.info("Notifying the user of the min version problem.");
                            // Notify the user that they should upgrade their version of the TF command line
                            VcsNotifier.getInstance(getProject()).notifyImportantWarning(
                                    TfPluginBundle.message(TfPluginBundle.KEY_TFVC_TF_VERSION_WARNING_TITLE),
                                    error, new NotificationListener.Adapter() {

                                        @Override
                                        protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent hyperlinkEvent) {
                                            if (SETTINGS_URL_EVENT.equals(hyperlinkEvent.getDescription())) {
                                                ShowSettingsUtil.getInstance().showSettingsDialog(myProject, TFVC_NAME);
                                            } else {
                                                BrowserUtil.browse(TFVC_ONLINE_HELP_URL);
                                            }
                                        }
                                    });
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to warn user about min version of TF command line.", e);
                    }
                }
            }).queue();
        });
    }
}
