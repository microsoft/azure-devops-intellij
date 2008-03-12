package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.vcs.EditFileProvider;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.io.ReadOnlyAttributeUtil;
import org.jetbrains.tfsIntegration.core.tfs.Workstation;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.stubs.org.jetbrains.tfsIntegration.stubs.exceptions.TfsException;

import java.io.IOException;

public class TFSEditFileProvider implements EditFileProvider {


  public void editFiles(final VirtualFile[] files) throws VcsException {
    try {
      for (VirtualFile f : files) {
        WorkspaceInfo workspace = Workstation.getInstance().findWorkspace(f.getPath());
        if (workspace != null) {
          //workspace.getServer().getVCS().
          ReadOnlyAttributeUtil.setReadOnlyAttribute(f, false);
        } else {
          TFSVcs.LOG.info("No workspace found for " + f.getPath());
        }
      }
    }
    catch (IOException e) {
      throw new VcsException(e);
    }
    catch (TfsException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  public String getRequestText() {
    return "Would you like to invoke 'CheckOut' command?";

  }
}
