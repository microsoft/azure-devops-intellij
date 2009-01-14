/*
 * Copyright 2000-2008 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.tfsIntegration.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.tfsIntegration.core.TFSVcs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AddAction extends AnAction {

  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getData(PlatformDataKeys.PROJECT);
    final VirtualFile[] files = VcsUtil.getVirtualFiles(e);

    final List<VcsException> errors = new ArrayList<VcsException>();
    ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
      public void run() {
        ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
        errors.addAll(TFSVcs.getInstance(project).getCheckinEnvironment().scheduleUnversionedFilesForAddition(Arrays.asList(files)));
      }
    }, "Scheduling for addition...", false, project);

    if (!errors.isEmpty()) {
      AbstractVcsHelper.getInstance(project).showErrors(errors, TFSVcs.TFS_NAME);
    }
  }

  public void update(final AnActionEvent e) {
    final Project project = e.getData(PlatformDataKeys.PROJECT);
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
