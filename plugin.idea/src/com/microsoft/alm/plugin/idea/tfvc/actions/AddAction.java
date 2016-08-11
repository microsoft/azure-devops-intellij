// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.tfvc.core.TFSVcs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Action to add an unversioned file
 */
public class AddAction extends AnAction implements DumbAware {

    public void actionPerformed(AnActionEvent e) {
        final Project project = e.getData(CommonDataKeys.PROJECT);
        final VirtualFile[] files = VcsUtil.getVirtualFiles(e);

        final List<VcsException> errors = new ArrayList<VcsException>();
        ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
            public void run() {
                ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
                errors.addAll(TFSVcs.getInstance(project).getCheckinEnvironment().scheduleUnversionedFilesForAddition(Arrays.asList(files)));
            }
        }, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_ADD_SCHEDULING), false, project);

        if (!errors.isEmpty()) {
            AbstractVcsHelper.getInstance(project).showErrors(errors, TFSVcs.TFVC_NAME);
        }
    }

    public void update(final AnActionEvent e) {
        final Project project = e.getData(CommonDataKeys.PROJECT);
        final VirtualFile[] files = VcsUtil.getVirtualFiles(e);
        e.getPresentation().setEnabled(isEnabled(project, files));
    }

    private static boolean isEnabled(Project project, VirtualFile[] files) {
        if (files.length == 0) {
            return false;
        }

        FileStatusManager fileStatusManager = FileStatusManager.getInstance(project);
        for (VirtualFile file : files) {
            final FileStatus fileStatus = fileStatusManager.getStatus(file);
            if (fileStatus != FileStatus.UNKNOWN) {
                return false;
            }
        }

        return true;
    }
}
