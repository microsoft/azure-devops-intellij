package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.vcs.EditFileProvider;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Failure;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.GetOperation;

import java.util.List;

public class TFSEditFileProvider implements EditFileProvider {

  public void editFiles(final VirtualFile[] files) throws VcsException {
    try {
      WorkstationHelper.ProcessResult<ResultWithFailures<GetOperation>> processResult =
        WorkstationHelper.processByWorkspaces(TfsFileUtil.getFilePaths(files), new WorkstationHelper.ProcessDelegate<ResultWithFailures<GetOperation>>() {
          public ResultWithFailures<GetOperation> executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
            return workspace.getServer().getVCS().checkoutForEdit(workspace.getName(), workspace.getOwnerName(), paths);
          }
        });
      for (Failure failure : processResult.results.getFailures()) {
        VcsException exception = BeanHelper.getVcsException("Failed to checkout", failure);
        if (exception != null) {
          throw exception;
        }
      }

      for (GetOperation getOp : processResult.results.getResult()) {
        String localPath = getOp.getSlocal(); // TODO determine GetOperation local path
        VirtualFile file = VcsUtil.getVirtualFile(localPath);
        if (file != null && file.isValid() && !file.isDirectory()) {
          TfsFileUtil.setReadOnlyInEventDispathThread(file, false);
        }
      }
    }
    catch (TfsException e) {
      throw new VcsException(e);
    }
  }

  public String getRequestText() {
    return "Would you like to invoke 'CheckOut' command?";

  }
}
