package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.VersionControlPath;
import org.jetbrains.tfsIntegration.core.tfs.VersionControlServer;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.version.LatestVersionSpec;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.GetOperation;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ItemType;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.RecursionType;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.LocalVersionUpdate;
import org.jetbrains.tfsIntegration.ui.ItemTreeNode;
import org.jetbrains.tfsIntegration.ui.ServerItemSelectDialog;
import org.jetbrains.tfsIntegration.ui.WorkspacesDialog;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class TFSCheckoutProvider implements CheckoutProvider {
  public void doCheckout(@Nullable final Listener listener) {
    WorkspacesDialog workspacesDialog = new WorkspacesDialog(WorkspacesDialog.Mode.Choose);
    workspacesDialog.show();
    if (!workspacesDialog.isOK()) {
      return;
    }
    WorkspaceInfo workspaceInfo = workspacesDialog.getSelectedWorkspaceInfo();
    ServerItemSelectDialog itemSelectDialog = new ServerItemSelectDialog(workspaceInfo, VersionControlPath.ROOT_FOLDER, false);
    itemSelectDialog.show();
    if (!itemSelectDialog.isOK()) {
      return;
    }
    Object selection = itemSelectDialog.getSelectedItem();
    if (!(selection instanceof ItemTreeNode)) {
      return;
    }
    String item = ((ItemTreeNode)selection).getFullPath();
    doCheckout(workspaceInfo, item, listener);
  }

  private static void doCheckout(final WorkspaceInfo workspaceInfo, final String serverRoot, final Listener listener) {
    final Ref<Boolean> checkoutSuccessful = new Ref<Boolean>();
    final Ref<File> localRoot = new Ref<File>();
    final Ref<Exception> exception = new Ref<Exception>();

    Runnable checkoutRunnable = new Runnable() {
      public void run() {
        ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
        try {
          final List<GetOperation> operations = workspaceInfo.getServer().getVCS()
            .get(workspaceInfo.getName(), workspaceInfo.getOwnerName(), serverRoot, LatestVersionSpec.getLatest(), RecursionType.Full);
          List<LocalVersionUpdate> localVersions = new ArrayList<LocalVersionUpdate>();
          localRoot.set(new File(operations.get(0).getTlocal()));
          for (GetOperation operation : operations) {
            progressIndicator.setText("Checkout from TFS: " + operation.getTitem());
            File file = new File(operation.getTlocal());
            if (operation.getType() == ItemType.Folder) {
              file.mkdirs();
            }
            else if (operation.getType() == ItemType.File) {
              if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
              }
              VersionControlServer.downloadItem(workspaceInfo, operation.getDurl(), file);
              file.setReadOnly();
            }
            LocalVersionUpdate localVersionUpdate = new LocalVersionUpdate();
            localVersionUpdate.setItemid(operation.getItemid());
            localVersionUpdate.setTlocal(operation.getTlocal());
            localVersionUpdate.setLver(operation.getLver());
            localVersions.add(localVersionUpdate);
            progressIndicator.checkCanceled();
          }
          workspaceInfo.getServer().getVCS().updateLocalVersions(workspaceInfo.getName(), workspaceInfo.getOwnerName(), localVersions);
          checkoutSuccessful.set(true);
        }
        catch (Exception e) {
          exception.set(e);
        }
      }
    };

    ProgressManager.getInstance()
      .runProcessWithProgressSynchronously(checkoutRunnable, "Checkout from TFS", true, ProjectManager.getInstance().getDefaultProject());

    if (!exception.isNull()) {
      String errorMessage = MessageFormat.format("Checkout failed.\n{0}", exception.get().getLocalizedMessage());
      Messages.showErrorDialog("Checkout from TFS", errorMessage);
      TFSVcs.LOG.error(exception.get());
    }

    Runnable listenerNotificationRunnable = new Runnable() {
      public void run() {
        if (listener != null) {
          if (!checkoutSuccessful.isNull() && !localRoot.isNull() && localRoot.get().isDirectory()) {
            listener.directoryCheckedOut(localRoot.get());
          }
          listener.checkoutCompleted();
        }
      }
    };

    String fileURL = VfsUtil.pathToUrl(serverRoot.replace(File.separatorChar, '/'));
    VirtualFile vf = VirtualFileManager.getInstance().findFileByUrl(fileURL);
    if (vf != null) {
      vf.refresh(true, true, listenerNotificationRunnable);
    }
    else {
      listenerNotificationRunnable.run();
    }
  }


  @NonNls
  public String getVcsName() {
    return TFSVcs.TFS_NAME;
  }
}
