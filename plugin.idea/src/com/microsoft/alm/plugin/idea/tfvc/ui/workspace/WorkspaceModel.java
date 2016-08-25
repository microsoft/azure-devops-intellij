// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.workspace;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.RepositoryContext;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.external.utils.WorkspaceHelper;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.services.LocalizationServiceImpl;
import com.microsoft.alm.plugin.idea.common.ui.common.AbstractModel;
import com.microsoft.alm.plugin.idea.common.ui.common.ModelValidationInfo;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import com.microsoft.alm.plugin.idea.common.utils.VcsHelper;
import com.microsoft.alm.plugin.operations.OperationExecutor;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.event.HyperlinkEvent;
import javax.ws.rs.NotAuthorizedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WorkspaceModel extends AbstractModel {
    private final Logger logger = LoggerFactory.getLogger(WorkspaceModel.class);

    public static final String PROP_NAME = "name";
    public static final String PROP_COMPUTER = "computer";
    public static final String PROP_OWNER = "owner";
    public static final String PROP_COMMENT = "comment";
    public static final String PROP_SERVER = "server";
    public static final String PROP_MAPPINGS = "mappings";
    public static final String PROP_LOADING = "loading";

    private boolean loading;
    private String name;
    private String computer;
    private String owner;
    private String comment;
    private String server;
    private List<Workspace.Mapping> mappings;

    private Workspace oldWorkspace;
    private ServerContext currentServerContext;

    public WorkspaceModel() {
    }

    public boolean isLoading() {
        return loading;
    }

    private void setLoading(final boolean loading) {
        this.loading = loading;
        setChangedAndNotify(PROP_LOADING);
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        if (!StringUtils.equals(this.name, name)) {
            this.name = name;
            super.setChangedAndNotify(PROP_NAME);
        }
    }

    public String getComputer() {
        return computer;
    }

    public void setComputer(final String computer) {
        if (!StringUtils.equals(this.computer, computer)) {
            this.computer = computer;
            super.setChangedAndNotify(PROP_COMPUTER);
        }
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(final String owner) {
        if (!StringUtils.equals(this.owner, owner)) {
            this.owner = owner;
            super.setChangedAndNotify(PROP_OWNER);
        }
    }

    public String getComment() {
        return comment;
    }

    public void setComment(final String comment) {
        if (!StringUtils.equals(this.comment, comment)) {
            this.comment = comment;
            super.setChangedAndNotify(PROP_COMMENT);
        }
    }

    public String getServer() {
        return server;
    }

    public void setServer(final String server) {
        if (!StringUtils.equals(this.server, server)) {
            this.server = server;
            super.setChangedAndNotify(PROP_SERVER);
        }
    }

    public List<Workspace.Mapping> getMappings() {
        if (mappings == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(mappings);
    }

    public void setMappings(@NotNull final List<Workspace.Mapping> mappings) {
        if (WorkspaceHelper.areMappingsDifferent(this.mappings, mappings)) {
            this.mappings = mappings;
            super.setChangedAndNotify(PROP_MAPPINGS);
        }
    }

    public ModelValidationInfo validate() {
        if (StringUtils.isEmpty(getName())) {
            return ModelValidationInfo.createWithResource(PROP_NAME,
                    TfPluginBundle.KEY_WORKSPACE_DIALOG_ERRORS_NAME_EMPTY);
        }
        if (getMappings().size() == 0) {
            return ModelValidationInfo.createWithResource(PROP_MAPPINGS,
                    TfPluginBundle.KEY_WORKSPACE_DIALOG_ERRORS_MAPPINGS_EMPTY);
        }

        return ModelValidationInfo.NO_ERRORS;
    }

    public void loadWorkspace(final Project project) {
        logger.info("loadWorkspace starting");
        setLoading(true);
        // Load
        OperationExecutor.getInstance().submitOperationTask(new Runnable() {
            @Override
            public void run() {
                try {
                    logger.info("loadWorkspace: getting repository context");
                    final RepositoryContext repositoryContext = VcsHelper.getRepositoryContext(project);
                    if (repositoryContext == null) {
                        logger.warn("loadWorkspace: Could not determine repositoryContext for project");
                        throw new RuntimeException(TfPluginBundle.message(TfPluginBundle.KEY_WORKSPACE_DIALOG_ERRORS_CONTEXT_FAILED));
                    }

                    logger.info("loadWorkspace: getting server context");
                    currentServerContext = ServerContextManager.getInstance().createContextFromTfvcServerUrl(repositoryContext.getUrl(), repositoryContext.getTeamProjectName(), true);
                    if (currentServerContext == null) {
                        logger.warn("loadWorkspace: Could not get the context for the repository. User may have canceled.");
                        throw new NotAuthorizedException(TfPluginBundle.message(TfPluginBundle.KEY_WORKSPACE_DIALOG_ERRORS_AUTH_FAILED, repositoryContext.getUrl()));
                    }

                    logger.info("loadWorkspace: getting workspace");
                    // It would be a bit more efficient here to pass the workspace name into the getWorkspace command.
                    // However, when you change the name of the workspace that causes errors. So for now, we are not optimal.
                    loadWorkspaceInternal(CommandUtils.getWorkspace(currentServerContext, project));
                } finally {
                    loadWorkspaceComplete();

                }
            }
        });
    }

    public void loadWorkspace(final RepositoryContext repositoryContext, final String workspaceName) {
        logger.info("loadWorkspace starting");
        ArgumentHelper.checkNotNull(repositoryContext, "repositoryContext");
        ArgumentHelper.checkNotEmptyString(workspaceName);

        setLoading(true);
        // Load
        OperationExecutor.getInstance().submitOperationTask(new Runnable() {
            @Override
            public void run() {
                try {
                    logger.info("loadWorkspace: getting server context");
                    currentServerContext = ServerContextManager.getInstance().createContextFromTfvcServerUrl(repositoryContext.getUrl(), repositoryContext.getTeamProjectName(), true);
                    if (currentServerContext == null) {
                        logger.warn("loadWorkspace: Could not get the context for the repository. User may have canceled.");
                        throw new NotAuthorizedException(TfPluginBundle.message(TfPluginBundle.KEY_WORKSPACE_DIALOG_ERRORS_AUTH_FAILED, repositoryContext.getUrl()));
                    }

                    logger.info("loadWorkspace: getting workspace by name");
                    loadWorkspaceInternal(CommandUtils.getWorkspace(currentServerContext, workspaceName));
                } finally {
                    // Make sure to fire events only on the UI thread
                    loadWorkspaceComplete();
                }
            }
        });
    }

    private void loadWorkspaceInternal(final Workspace workspace) {
        oldWorkspace = workspace;
        if (oldWorkspace != null) {
            logger.info("loadWorkspace: got workspace, setting fields");
            server = oldWorkspace.getServer();
            owner = oldWorkspace.getOwner();
            computer = oldWorkspace.getComputer();
            name = oldWorkspace.getName();
            comment = oldWorkspace.getComment();
            mappings = new ArrayList<Workspace.Mapping>(oldWorkspace.getMappings());
        } else {
            // This shouldn't happen, so we will log this case, but not throw
            logger.warn("loadWorkspace: workspace was returned as null");
        }
    }

    private void loadWorkspaceComplete() {
        // Make sure to fire events only on the UI thread
        IdeaHelper.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                // Update all fields
                setChangedAndNotify(null);
                // Set loading to false
                setLoading(false);
                logger.info("loadWorkspace: done loading");
            }
        });
    }

    public void saveWorkspace(final Project project, final boolean syncFiles, final Runnable onSuccess) {
        final ServerContext serverContext = currentServerContext;
        final Workspace oldWorkspace = this.oldWorkspace;
        final Workspace newWorkspace = new Workspace(server, name, computer, owner, comment, mappings);

        // Using IntelliJ's background framework here so the user can choose to wait or continue working
        final Task.Backgroundable createPullRequestTask = new Task.Backgroundable(project,
                TfPluginBundle.message(TfPluginBundle.KEY_WORKSPACE_DIALOG_PROGRESS_TITLE),
                true, PerformInBackgroundOption.DEAF) {
            @Override
            public void run(@NotNull final ProgressIndicator indicator) {
                try {
                    IdeaHelper.setProgress(indicator, 0.10,
                            TfPluginBundle.message(TfPluginBundle.KEY_WORKSPACE_DIALOG_SAVE_PROGRESS_UPDATING));

                    // provide some indication of progress (setting indeterminate didn't do anything
                    CommandUtils.updateWorkspace(serverContext, oldWorkspace, newWorkspace);

                    if (syncFiles) {
                        IdeaHelper.setProgress(indicator, 0.30,
                                TfPluginBundle.message(TfPluginBundle.KEY_WORKSPACE_DIALOG_SAVE_PROGRESS_SYNCING));
                        syncWorkspace(project);
                    }

                    if (onSuccess != null) {
                        // Trigger the onSuccess callback on the UI thread
                        IdeaHelper.runOnUIThread(onSuccess);
                    }

                    IdeaHelper.setProgress(indicator, 1.00,
                            TfPluginBundle.message(TfPluginBundle.KEY_WORKSPACE_DIALOG_SAVE_PROGRESS_DONE), true);

                    // Notify the user of success and provide a link to sync the workspace
                    VcsNotifier.getInstance(project).notifyImportantInfo(
                            TfPluginBundle.message(TfPluginBundle.KEY_WORKSPACE_DIALOG_NOTIFY_SUCCESS_TITLE),
                            TfPluginBundle.message(TfPluginBundle.KEY_WORKSPACE_DIALOG_NOTIFY_SUCCESS_MESSAGE),
                            new NotificationListener() {
                                @Override
                                public void hyperlinkUpdate(@NotNull final Notification n, @NotNull final HyperlinkEvent e) {
                                    syncWorkspace(project);
                                }
                            });
                } catch (final Throwable t) {
                    //TODO on failure we could provide a link that reopened the dialog with the values they tried to save
                    VcsNotifier.getInstance(project).notifyError(
                            TfPluginBundle.message(TfPluginBundle.KEY_WORKSPACE_DIALOG_NOTIFY_FAILURE_TITLE),
                            LocalizationServiceImpl.getInstance().getExceptionMessage(t));
                }
            }
        };

        createPullRequestTask.queue();
    }

    public void syncWorkspace(final Project project) {
        //TODO
    }
}
