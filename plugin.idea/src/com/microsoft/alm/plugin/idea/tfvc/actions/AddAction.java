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

package com.microsoft.alm.plugin.idea.tfvc.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import com.microsoft.alm.plugin.idea.common.actions.InstrumentedAction;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.tfvc.core.TFSVcs;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Action to add an unversioned file
 */
public class AddAction extends InstrumentedAction {
    private static final Logger logger = LoggerFactory.getLogger(AddAction.class);

    protected AddAction() {
        super(false);
    }

    @Override
    public void doActionPerformed(final AnActionEvent anActionEvent) {
        final Project project = anActionEvent.getData(CommonDataKeys.PROJECT);
        final VirtualFile[] files = VcsUtil.getVirtualFiles(anActionEvent);

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

    @Override
    public void doUpdate(final AnActionEvent anActionEvent) {
        final Project project = anActionEvent.getProject();
        final VirtualFile[] files = VcsUtil.getVirtualFiles(anActionEvent);
        anActionEvent.getPresentation().setEnabled(isEnabled(project, files));
    }

    private static boolean isEnabled(final Project project, final VirtualFile[] files) {
        if (files.length == 0) {
            return false;
        }

        if (project == null) {
            logger.warn("Cannot enable AddAction because project is unknown (null)");
            return false;
        }

        final FileStatusManager fileStatusManager = FileStatusManager.getInstance(project);
        return TfsFileUtil.findUnknownFiles(files, fileStatusManager);
    }
}