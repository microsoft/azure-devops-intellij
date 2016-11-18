// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.ui.CommitChangeListDialog;
import com.intellij.openapi.vcs.ui.Refreshable;
import com.microsoft.alm.plugin.idea.common.resources.Icons;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.workitem.SelectWorkItemsDialog;
import com.microsoft.alm.plugin.idea.git.utils.TfGitHelper;
import com.microsoft.alm.plugin.idea.tfvc.core.TFSVcs;
import org.apache.commons.lang.StringUtils;


public class SelectWorkItemsAction extends InstrumentedAction {

    public SelectWorkItemsAction() {
        super(TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_SELECT_WORK_ITEMS_TITLE),
                TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_SELECT_WORK_ITEMS_MSG),
                Icons.WIT_ADD);
    }


    @Override
    public void doUpdate(AnActionEvent anActionEvent) {
        anActionEvent.getPresentation().setVisible(true);

        // only enable button if the repo is TFS
        final Project project = CommonDataKeys.PROJECT.getData(anActionEvent.getDataContext());
        if (TfGitHelper.getTfGitRepository(project) == null && TFSVcs.getInstance(project) == null) {
            anActionEvent.getPresentation().setEnabled(false);
            // change hover text to explain why button is disabled
            anActionEvent.getPresentation().setText(TfPluginBundle.message(TfPluginBundle.KEY_ERRORS_NOT_TFS_REPO,
                    TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_SELECT_WORK_ITEMS_ACTION)));
        } else {
            anActionEvent.getPresentation().setEnabled(true);
            // update hover text in case it was disabled before
            anActionEvent.getPresentation().setText(TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_SELECT_WORK_ITEMS_TITLE));
        }
    }

    @Override
    public void doActionPerformed(AnActionEvent anActionEvent) {
        final DataContext dc = anActionEvent.getDataContext();
        final Project project = CommonDataKeys.PROJECT.getData(dc);
        final Refreshable panel = CheckinProjectPanel.PANEL_KEY.getData(dc);
        final CommitMessageI commitMessageI = (panel instanceof CommitMessageI) ? (CommitMessageI) panel : VcsDataKeys.COMMIT_MESSAGE_CONTROL.getData(dc);

        if (commitMessageI != null && project != null) {
            String commitMessage = "";
            // Attempt to append the message instead of overwriting it
            if (commitMessageI instanceof CommitChangeListDialog) {
                commitMessage = ((CommitChangeListDialog) commitMessageI).getCommitMessage();
            }

            SelectWorkItemsDialog dialog = new SelectWorkItemsDialog(project);
            if (dialog.showAndGet()) {
                if (StringUtils.isNotEmpty(commitMessage)) {
                    commitMessage += "\n" + dialog.getComment();
                } else {
                    commitMessage = dialog.getComment();
                }

                commitMessageI.setCommitMessage(commitMessage);
            }
        }
    }
}
