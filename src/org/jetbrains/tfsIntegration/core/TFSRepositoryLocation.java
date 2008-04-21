package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.vcs.RepositoryLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.tfs.ItemPath;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;

public class TFSRepositoryLocation implements RepositoryLocation {

  private final @NotNull WorkspaceInfo myWorkspace;
  private final @NotNull ItemPath myItemPath;

  public TFSRepositoryLocation(final @NotNull ItemPath itemPath, final @NotNull WorkspaceInfo workspace) {
    myItemPath = itemPath;
    myWorkspace = workspace;
  }

  public String toPresentableString() {
    return myItemPath.getServerPath();
  }

  @NotNull
  public WorkspaceInfo getWorkspace() {
    return myWorkspace;
  }

  @NotNull
  public ItemPath getItemPath() {
    return myItemPath;
  }

  public String toString() {
    // IDEA needs this!
    return myItemPath.getServerPath();
  }
}
