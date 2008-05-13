package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.vcs.EditFileProvider;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.GetOperation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TFSEditFileProvider implements EditFileProvider {

  public void editFiles(final VirtualFile[] files) throws VcsException {
    Collection<VcsException> errors = new ArrayList<VcsException>();
    try {
      WorkstationHelper.ProcessResult<ResultWithFailures<GetOperation>> processResult = WorkstationHelper
        .processByWorkspaces(TfsFileUtil.getFilePaths(files), new WorkstationHelper.ProcessDelegate<ResultWithFailures<GetOperation>>() {
          public ResultWithFailures<GetOperation> executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths)
            throws TfsException {
            return workspace.getServer().getVCS().checkoutForEdit(workspace.getName(), workspace.getOwnerName(), paths);
          }
        });

      errors.addAll(BeanHelper.getVcsExceptions(processResult.results.getFailures()));

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
    if (!errors.isEmpty()) {
      Collection<String> messages = new ArrayList<String>(errors.size());
      for (VcsException error : errors) {
        messages.add(error.getMessage());
      }
      throw new VcsException(messages);
    }
  }

  public String getRequestText() {
    return "Would you like to invoke 'CheckOut' command?";

  }
}
