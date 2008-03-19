package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.vcs.EditFileProvider;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.io.ReadOnlyAttributeUtil;
import com.intellij.vcsUtil.VcsUtil;
import org.apache.axis2.databinding.ADBBean;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.WorkstationHelper;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Failure;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.GetOperation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TFSEditFileProvider implements EditFileProvider {

  public void editFiles(final VirtualFile[] files) throws VcsException {
    List<String> localPaths = new ArrayList<String>(files.length);
    for (VirtualFile f : files) {
      localPaths.add(f.getPath());
    }

    try {
      WorkstationHelper.ProcessResult<ADBBean> processResult =
        WorkstationHelper.processByWorkspaces(localPaths, new WorkstationHelper.Delegate<ADBBean>() {
          public Map<String, ADBBean> executeRequest(final WorkspaceInfo workspace, final List<String> serverPaths) throws TfsException {
            return workspace.getServer().getVCS().checkoutForEdit(workspace.getName(), workspace.getOwnerName(), serverPaths);
          }
        });
      for (String localPath : localPaths) {
        ADBBean resultBean = processResult.results.get(localPath);
        if (resultBean instanceof GetOperation) {
          ReadOnlyAttributeUtil.setReadOnlyAttribute(VcsUtil.getVirtualFile(localPath), false);
        }
        else {
          throw new VcsException("Failed to checkout " + localPath + ": " + ((Failure)resultBean).getMessage());
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
