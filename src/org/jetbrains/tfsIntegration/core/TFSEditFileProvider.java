package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.vcs.EditFileProvider;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;

public class TFSEditFileProvider implements EditFileProvider {


  public void editFiles(final VirtualFile[] files) throws VcsException {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public String getRequestText() {
    return "Would you like to invoke 'CheckOut' command?";

  }
}
