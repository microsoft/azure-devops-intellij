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

package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.GetOperation;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.RecursionType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.WorkingFolderInfo;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.Workstation;
import org.jetbrains.tfsIntegration.core.tfs.operations.ApplyGetOperations;
import org.jetbrains.tfsIntegration.core.tfs.operations.ApplyProgress;
import org.jetbrains.tfsIntegration.core.tfs.version.LatestVersionSpec;
import org.jetbrains.tfsIntegration.exceptions.OperationFailedException;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.ui.checkoutwizard.*;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class TFSCheckoutProvider implements CheckoutProvider {

  public void doCheckout(@NotNull final Project project, @Nullable final Listener listener) {
    final CheckoutWizardModel model = new CheckoutWizardModel();
    List<CheckoutWizardStep> steps = Arrays
      .asList(new ChooseModeStep(model), new ChooseWorkspaceStep(project, model), new ChooseLocalAndServerPathsStep(model),
              new ChooseServerPathStep(model), new SummaryStep(model));
    CheckoutWizard w = new CheckoutWizard(project, steps, model);
    if (w.showAndGet()) {
      doCheckout(model, listener);
    }
  }

  private static void doCheckout(final CheckoutWizardModel model, final Listener listener) {
    final Collection<VcsException> errors = new ArrayList<VcsException>();
    final Ref<FilePath> localRoot = new Ref<FilePath>();

    Runnable checkoutRunnable = new Runnable() {
      public void run() {
        ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
        try {
          final WorkspaceInfo workspace;
          if (model.getMode() == CheckoutWizardModel.Mode.Auto) {
            workspace = createWorkspace(model);
          }
          else {
            workspace = model.getWorkspace();
          }
          localRoot.set(workspace.findLocalPathByServerPath(model.getServerPath(), true, null));

          // TODO when checking out after working folder mappings were changed, GetOps may contain inappropriate 'move' operations

          final List<GetOperation> operations = workspace.getServer().getVCS()
            .get(workspace.getName(), workspace.getOwnerName(), model.getServerPath(), LatestVersionSpec.INSTANCE, RecursionType.Full, null,
                 null);

          final Collection<VcsException> applyErrors = ApplyGetOperations
            .execute(ProjectManager.getInstance().getDefaultProject(), workspace, operations,
                     new ApplyProgress.ProgressIndicatorWrapper(progressIndicator), null, ApplyGetOperations.DownloadMode.ALLOW);
          // TODO: DownloadMode.FORCE?
          errors.addAll(applyErrors);
        }
        catch (TfsException e) {
          errors.add(new VcsException(e.getMessage(), e));
        }
      }
    };

    ProgressManager.getInstance()
      .runProcessWithProgressSynchronously(checkoutRunnable, "Checkout from TFS", true, ProjectManager.getInstance().getDefaultProject());

    if (errors.isEmpty()) {
      final Runnable listenerNotificationRunnable = new Runnable() {
        public void run() {
          if (listener != null) {
            if (errors.isEmpty()) {
              listener.directoryCheckedOut(new File(localRoot.get().getPath()), TFSVcs.getKey());
            }
            listener.checkoutCompleted();
          }
        }
      };

      VirtualFile vf = VcsUtil.getVirtualFile(localRoot.get().getPath());
      if (vf != null) {
        vf.refresh(true, true, new Runnable() {
          @Override
          public void run() {
            final ModalityState current = ModalityState.current();
            ApplicationManager.getApplication().invokeLater(listenerNotificationRunnable, current);
          }
        });
      }
      else {
        listenerNotificationRunnable.run();
      }
    }
    else {
      StringBuilder errorMessage = new StringBuilder("The following errors occurred during checkout:\n\n");
      for (VcsException e : errors) {
        errorMessage.append(e.getMessage()).append("\n");
      }
      Messages.showErrorDialog(errorMessage.toString(), TFSBundle.message("checkout.from.tfs.error.dialog.title"));
    }
  }


  @NonNls
  public String getVcsName() {
    return "_TFS";
  }

  private static WorkspaceInfo createWorkspace(CheckoutWizardModel model) throws TfsException {
    WorkspaceInfo workspace = new WorkspaceInfo(model.getServer(), model.getServer().getQualifiedUsername(), Workstation.getComputerName());
    workspace.setName(model.getNewWorkspaceName());
    workspace.setComment(TFSBundle.message("automatic.workspace.comment",
                                           ApplicationNamesInfo.getInstance().getFullProductName(), model.getServerPath()));
    FilePath localPath = VcsUtil.getFilePath(model.getDestinationFolder(), true);
    WorkingFolderInfo workingFolder = new WorkingFolderInfo(WorkingFolderInfo.Status.Active, localPath, model.getServerPath());
    workspace.addWorkingFolderInfo(workingFolder);
    try {
      workspace.saveToServer(null, null);
    }
    catch (TfsException e) {
      String errorMessage = MessageFormat.format("Cannot create workspace ''{0}''. {1}", workspace.getName(), e.getMessage());
      throw new OperationFailedException(errorMessage);
    }
    return workspace;
  }

}
