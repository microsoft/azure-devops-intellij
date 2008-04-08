package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.vcs.EditFileProvider;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.io.ReadOnlyAttributeUtil;
import com.intellij.vcsUtil.VcsUtil;
import org.apache.axis2.databinding.ADBBean;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Failure;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.GetOperation;

import java.io.IOException;
import java.util.List;

public class TFSEditFileProvider implements EditFileProvider {

  public void editFiles(final VirtualFile[] files) throws VcsException {
    try {
      WorkstationHelper.ProcessResult<ADBBean> processResult =
        WorkstationHelper.processByWorkspaces(TfsFileUtil.getFilePaths(files), new WorkstationHelper.ProcessDelegate<ADBBean>() {
          public List<ADBBean> executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
            return workspace.getServer().getVCS().checkoutForEdit(workspace.getName(), workspace.getOwnerName(), paths);
          }
        });
      for (ADBBean resultBean : processResult.results) {
        if (resultBean instanceof GetOperation) {
          GetOperation getOp = (GetOperation)resultBean;
          String localPath = getOp.getSlocal(); // TODO determine GetOperation local path
          VirtualFile file = VcsUtil.getVirtualFile(localPath);
          if (file != null && file.isValid() && !file.isDirectory()) {
            ReadOnlyAttributeUtil.setReadOnlyAttribute(file, false);
          }
        }
        else {
          Failure failure = (Failure)resultBean;
          VcsException exception = BeanHelper.getVcsException("Failed to checkout", failure);
          if (exception != null) {
            throw exception;
          }
        }
      }
    }
    catch (TfsException e) {
      throw new VcsException(e);
    }
    catch (IOException e) {
      throw new VcsException(e);
    }
  }

  public String getRequestText() {
    return "Would you like to invoke 'CheckOut' command?";

  }
}
