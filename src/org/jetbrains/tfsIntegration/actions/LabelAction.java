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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.tfs.ResultWithFailures;
import org.jetbrains.tfsIntegration.core.tfs.TfsUtil;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.LabelResult;
import org.jetbrains.tfsIntegration.ui.ApplyLabelDialog;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class LabelAction extends SingleItemAction {

  protected void execute(final @NotNull Project project,
                         final @NotNull WorkspaceInfo workspace,
                         final @NotNull FilePath localPath,
                         final @NotNull ExtendedItem extendedItem) {

    final ApplyLabelDialog d = new ApplyLabelDialog(project, workspace, extendedItem.getSitem());
    d.show();
    if (!d.isOK()) {
      return;
    }

    final List<VcsException> errors = new ArrayList<VcsException>();
    try {
      ResultWithFailures<LabelResult> resultWithFailures =
        workspace.getServer().getVCS().labelItem(d.getLabelName(), d.getLabelComment(), d.getLabelItemSpecs());

      errors.addAll(TfsUtil.getVcsExceptions(resultWithFailures.getFailures()));

      StringBuffer buffer = new StringBuffer();
      for (LabelResult labelResult : resultWithFailures.getResult()) {
        if (buffer.length() > 0) {
          buffer.append("\n");
        }
        String message = MessageFormat.format("Label ''{0}@{1}'' {2}", labelResult.getLabel(), labelResult.getScope(),
                                              labelResult.getStatus().getValue().toLowerCase());
        buffer.append(message);
      }
      if (buffer.length() > 0) {
        TfsUtil.showBalloon(project, MessageType.INFO, buffer.toString());
      }
    }
    catch (TfsException e) {
      errors.add(new VcsException(e));
    }

    if (!errors.isEmpty()) {
      AbstractVcsHelper.getInstance(project).showErrors(errors, "TFS: Apply Label");
    }
  }

}
