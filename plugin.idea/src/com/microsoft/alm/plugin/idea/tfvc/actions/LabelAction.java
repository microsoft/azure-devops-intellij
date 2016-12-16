// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.actions;


import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.actions.VcsContextFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import com.microsoft.alm.helpers.Path;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.models.ItemInfo;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.common.actions.InstrumentedAction;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.tfvc.core.TFSVcs;
import com.microsoft.alm.plugin.idea.tfvc.ui.ApplyLabelDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class LabelAction extends InstrumentedAction {
    public static final Logger logger = LoggerFactory.getLogger(LabelAction.class);

    public LabelAction() {
        super(TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_LABEL_TITLE),
                TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_LABEL_MSG),
                null);
    }

    @Override
    public void doUpdate(final AnActionEvent anActionEvent) {
        final Project project = anActionEvent.getData(CommonDataKeys.PROJECT);
        final VirtualFile[] files = VcsUtil.getVirtualFiles(anActionEvent);
        anActionEvent.getPresentation().setEnabled(isEnabled(project, files));
    }

    @Override
    public void doActionPerformed(final AnActionEvent anActionEvent) {
        final Project project = anActionEvent.getData(CommonDataKeys.PROJECT);
        final VirtualFile[] files = VcsUtil.getVirtualFiles(anActionEvent);

        // Find the list of selected files and get itemInfos for each one
        final List<VcsException> errors = new ArrayList<VcsException>();
        final List<ItemInfo> itemInfos = new ArrayList<ItemInfo>(files.length);

        ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
            public void run() {
                try {
                    ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
                    final ServerContext context = TFSVcs.getInstance(project).getServerContext(true);
                    for (final VirtualFile file : files) {
                        final FilePath localPath = VcsContextFactory.SERVICE.getInstance().createFilePathOn(file);
                        final ItemInfo info = CommandUtils.getItemInfo(context, localPath.getPath());
                        itemInfos.add(info);
                    }
                } catch (Throwable t) {
                    logger.warn("Errors occurred trying to get info for files in ApplyLabel", t);
                    errors.add(TFSVcs.convertToVcsException(t));
                }
            }
        }, TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_LABEL_PROGRESS_GATHERING_INFORMATION), false, project);

        if (!errors.isEmpty()) {
            AbstractVcsHelper.getInstance(project).showErrors(errors, TFSVcs.TFVC_NAME);
            return;
        }

        if (itemInfos.size() == 0) {
            // Somehow we got here without items selected or we couldn't find the info for them.
            // This shouldn't happen, but just in case we won't continue
            return;
        }

        // Open the Apply Label dialog and allow the user to enter label name and description
        final ApplyLabelDialog d = new ApplyLabelDialog(project, itemInfos);
        if (!d.showAndGet()) {
            return;
        }

        //TODO Handle updating existing label
//                    try {
//                        List<VersionControlLabel> labels = myWorkspace.getServer().getVCS()
//                                .queryLabels(getLabelName(), null, null, false, null, null, false, form.getContentPane(),
//                                        TFSBundle.message("checking.existing.labels"));
//                        if (!labels.isEmpty()) {
//                            String message = MessageFormat.format("Label ''{0}'' already exists.\nDo you want to update it?", getLabelName());
//                            if (Messages.showOkCancelDialog(project, message, getTitle(), "Update Label", "Cancel", Messages.getQuestionIcon()) !=
//                                    Messages.OK) {
//                                return;
//                            }
//                        }
//                    }
//                    catch (TfsException e) {
//                        Messages.showErrorDialog(project, e.getMessage(), getTitle());
//                        return;
//                    }

        final StringBuilder successMessage = new StringBuilder();
        ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
            public void run() {
                try {
                    // TODO create a common class to hold this logic (repeated in several places)
                    final String defaultLocalPath = itemInfos.get(0).getLocalItem();
                    final boolean isFolder = Path.directoryExists(defaultLocalPath);
                    final String workingFolder = isFolder ?
                            defaultLocalPath :
                            Path.getDirectoryName(defaultLocalPath);
                    final ServerContext context = TFSVcs.getInstance(project).getServerContext(true);

                    final boolean labelCreated = CommandUtils.createLabel(context, workingFolder, d.getLabelName(), d.getLabelComment(), d.isRecursiveChecked(), d.getLabelItemSpecs());
                    if (labelCreated) {
                        successMessage.append(TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_LABEL_SUCCESS_CREATED, d.getLabelName()));
                    } else {
                        successMessage.append(TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_LABEL_SUCCESS_UPDATED, d.getLabelName()));
                    }
                } catch (Throwable t) {
                    logger.warn("Errors occurred trying to get info for files in ApplyLabel", t);
                    errors.add(TFSVcs.convertToVcsException(t));
                }
            }
        }, TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_LABEL_PROGRESS_CREATING_LABEL), false, project);

        if (!errors.isEmpty()) {
            AbstractVcsHelper.getInstance(project).showErrors(errors, TFSVcs.TFVC_NAME);
        } else {
            Messages.showInfoMessage(project, successMessage.toString(), TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_LABEL_TITLE));
        }
    }

    private static boolean isEnabled(final Project project, final VirtualFile[] files) {
        if (files.length == 0) {
            return false;
        }

        final FileStatusManager fileStatusManager = FileStatusManager.getInstance(project);
        for (VirtualFile file : files) {
            final FileStatus fileStatus = fileStatusManager.getStatus(file);
            if (fileStatus != FileStatus.NOT_CHANGED && fileStatus != FileStatus.MODIFIED && fileStatus != FileStatus.HIJACKED) {
                return false;
            }
        }

        return true;
    }
}
