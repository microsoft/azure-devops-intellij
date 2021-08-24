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

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.checkin.CheckinEnvironment;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.FunctionUtil;
import com.intellij.util.NullableFunction;
import com.intellij.util.PairConsumer;
import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.exceptions.TeamServicesException;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.services.LocalizationServiceImpl;
import com.microsoft.alm.plugin.idea.common.utils.VcsHelper;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.VersionControlPath;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.operations.ScheduleForDeletion;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles VCS checkin features
 * TODO: comment back in the features as needed
 */
public class TFSCheckinEnvironment implements CheckinEnvironment {
    public static final Logger logger = LoggerFactory.getLogger(TFSCheckinEnvironment.class);

    private static final String CHECKIN_OPERATION_NAME = "Checkin";

    @NotNull
    private final TFSVcs myVcs;

    public TFSCheckinEnvironment(final @NotNull TFSVcs vcs) {
        myVcs = vcs;
    }

    @Nullable
    public RefreshableOnComponent createAdditionalOptionsPanel(final CheckinProjectPanel checkinProjectPanel,
                                                               PairConsumer<Object, Object> additionalDataConsumer) {
//        boolean isAffected = false;
//        for (File file : checkinProjectPanel.getFiles()) {
//            if (TFSVcs.isUnderTFS(VcsUtil.getFilePath(file), checkinProjectPanel.getProject())) {
//                isAffected = true;
//                break;
//            }
//        }
//        if (!isAffected) {
//            return null;
//        }
//
//        final JComponent panel = new JPanel();
//        panel.setLayout(new BorderLayout(5, 0));
//
//        myVcs.getCheckinData().messageLabel = new BoldLabel() {
//
//            @Override
//            public JToolTip createToolTip() {
//                JToolTip toolTip = new JToolTip() {{
//                    setUI(new MultiLineTooltipUI());
//                }};
//                toolTip.setComponent(this);
//                return toolTip;
//            }
//
//        };
//
//        panel.add(myVcs.getCheckinData().messageLabel, BorderLayout.WEST);
//
//        final JButton configureButton = new JButton("Configure...");
//        panel.add(configureButton, BorderLayout.EAST);
//
//        configureButton.addActionListener(new ActionListener() {
//
//            public void actionPerformed(final ActionEvent event) {
//                CheckinParameters copy = myVcs.getCheckinData().parameters.createCopy();
//
//                CheckinParametersDialog d = new CheckinParametersDialog(checkinProjectPanel.getProject(), copy);
//                if (d.showAndGet()) {
//                    myVcs.getCheckinData().parameters = copy;
//                    updateMessage(myVcs.getCheckinData());
//                }
//            }
//        });
//
//        return new TFSAdditionalOptionsPanel(panel, checkinProjectPanel, configureButton);
        return null;
    }

//    public static void updateMessage(TFSVcs.CheckinData checkinData) {
//        if (checkinData.parameters == null) {
//            return;
//        }
//
//        final Pair<String, CheckinParameters.Severity> message = checkinData.parameters.getValidationMessage(CheckinParameters.Severity.BOTH);
//        if (message == null) {
//            checkinData.messageLabel.setText("<html>Ready to commit</html>"); // prevent bold
//            checkinData.messageLabel.setIcon(null);
//            checkinData.messageLabel.setToolTipText(null);
//        } else {
//            checkinData.messageLabel.setToolTipText(message.first);
//            if (message.second == CheckinParameters.Severity.ERROR) {
//                checkinData.messageLabel.setText("Errors found");
//                checkinData.messageLabel.setIcon(UIUtil.getBalloonErrorIcon());
//            } else {
//                checkinData.messageLabel.setText("Warnings found");
//                checkinData.messageLabel.setIcon(UIUtil.getBalloonWarningIcon());
//            }
//        }
//    }

    @Nullable
    public String getDefaultMessageFor(final FilePath[] filesToCheckin) {
        return null;
    }

    @Nullable
    @NonNls
    public String getHelpId() {
        return null;  // TODO: help id for check in
    }

    public String getCheckinOperationName() {
        return CHECKIN_OPERATION_NAME;
    }

    @Nullable
    public List<VcsException> commit(final List<Change> changes,
                                     final String preparedComment,
                                     @NotNull NullableFunction<Object, Object> parametersHolder, Set<String> feedback) {
        final List<VcsException> errors = new ArrayList<>();

        // set progress bar status
        final ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
        TFSProgressUtil.setProgressText(progressIndicator, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CHECKIN_STATUS));

        // find files that are to be checked in
        final List<String> files = new ArrayList<>();
        for (final Change change : changes) {
            String path = null;
            final ContentRevision beforeRevision = change.getBeforeRevision();
            final ContentRevision afterRevision = change.getAfterRevision();
            if (afterRevision != null) {
                path = afterRevision.getFile().getPath();
            } else if (beforeRevision != null) {
                path = beforeRevision.getFile().getPath();
            }
            if (path != null) {
                files.add(path);
            }
        }

        try {
            final ServerContext context = myVcs.getServerContext(true);
            final List<Integer> workItemIds = VcsHelper.getWorkItemIdsFromMessage(preparedComment);
            final String changesetNumber = CommandUtils.checkinFiles(context, files, preparedComment, workItemIds);

            // notify user of success
            final String changesetLink = String.format(UrlHelper.SHORT_HTTP_LINK_FORMATTER, UrlHelper.getTfvcChangesetURI(context.getUri().toString(), changesetNumber),
                    TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CHECKIN_LINK_TEXT, changesetNumber));
            VcsNotifier.getInstance(myVcs.getProject()).notifyImportantInfo(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CHECKIN_SUCCESSFUL_TITLE),
                    TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CHECKIN_SUCCESSFUL_MSG, changesetLink), (notification, hyperlinkEvent) -> BrowserUtil.browse(hyperlinkEvent.getURL()));
        } catch (Exception e) {
            // no notification needs to be done by us for errors, IntelliJ handles that
            logger.warn("Error during checkin", e);
            if (e instanceof TeamServicesException) {
                // get localized message in the case of TeamServicesException otherwise the key will print out instead of the error
                errors.add(new VcsException(LocalizationServiceImpl.getInstance().getExceptionMessage(e)));
            } else {
                errors.add(new VcsException(e));
            }
        }

        return errors;
    }

    public List<VcsException> commit(List<Change> changes, String preparedComment) {
        return commit(changes, preparedComment, FunctionUtil.nullConstant(), null);
    }

    @Nullable
    public List<VcsException> scheduleMissingFileForDeletion(final List<FilePath> files) {
        final List<VcsException> errors = new ArrayList<>();

        ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
            errors.addAll(ScheduleForDeletion.execute(myVcs.getProject(), files));
        }, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_DELETE_SCHEDULING), false, myVcs.getProject());

        return errors;
    }

    @Nullable
    public List<VcsException> scheduleUnversionedFilesForAddition(final List<VirtualFile> files) {
        final List<VcsException> exceptions = new ArrayList<>();
        try {
            List<Path> pathsToAdd = files.stream()
                    .map(file -> Paths.get(file.getPath()))
                    .collect(Collectors.toList());
            TfvcClient client = TfvcClient.getInstance();
            List<Path> successfullyAdded = client.addFiles(
                    myVcs.getProject(),
                    myVcs.getServerContext(false),
                    pathsToAdd);

            // mark files as dirty so that they refresh in local changes tab
            for (Path path : successfullyAdded) {
                final VirtualFile file = VersionControlPath.getVirtualFile(path.toString());
                if (file != null && file.isValid()) {
                    TfsFileUtil.markFileDirty(myVcs.getProject(), file);
                }
            }

            //check all files were added
            if (successfullyAdded.size() != pathsToAdd.size()) {
                // remove all added files from original list of files to add to give us which files weren't added
                pathsToAdd.removeAll(successfullyAdded);
                exceptions.add(new VcsException(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_ADD_ERROR, StringUtils.join(pathsToAdd, ", "))));
            }
        } catch (RuntimeException e) {
            logger.warn("Exception during adding the files", e);
            exceptions.add(new VcsException(e));
        }
        return exceptions;
    }

    @Override
    public boolean isRefreshAfterCommitNeeded() {
        return true;
    }

//    // TODO refactor this class
//    private class TFSAdditionalOptionsPanel implements CheckinChangeListSpecificComponent {
//        private final JComponent myPanel;
//        private final CheckinProjectPanel myCheckinProjectPanel;
//        private final JButton myConfigureButton;
//        private LocalChangeList myCurrentList;
//
//        public TFSAdditionalOptionsPanel(JComponent panel, CheckinProjectPanel checkinProjectPanel, JButton configureButton) {
//            myPanel = panel;
//            myCheckinProjectPanel = checkinProjectPanel;
//            myConfigureButton = configureButton;
//        }
//
//        public JComponent getComponent() {
//            return myPanel;
//        }
//
//        public void refresh() {
//        }
//
//        public void saveState() {
//        }
//
//        public void restoreState() {
//        }
//
//        public void onChangeListSelected(LocalChangeList list) {
//            if (myCurrentList == list) {
//                return;
//            }
//            myCurrentList = list;
//
//            if (!myCheckinProjectPanel.hasDiffs()) {
//                myPanel.setVisible(false);
//                return;
//            }
//
//            myPanel.setVisible(true);
//
//            try {
//                myVcs.getCheckinData().parameters = new CheckinParameters(myCheckinProjectPanel, true);
//                myConfigureButton.setEnabled(true);
//                updateMessage(myVcs.getCheckinData());
//            } catch (OperationFailedException e) {
//                myVcs.getCheckinData().parameters = null;
//                myConfigureButton.setEnabled(false);
//                myVcs.getCheckinData().messageLabel.setIcon(UIUtil.getBalloonErrorIcon());
//                myVcs.getCheckinData().messageLabel.setText("Validation failed");
//                myVcs.getCheckinData().messageLabel.setToolTipText(e.getMessage());
//            }
//        }
//
//    }
}
