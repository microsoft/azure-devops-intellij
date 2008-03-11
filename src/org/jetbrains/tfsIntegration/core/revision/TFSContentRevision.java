package org.jetbrains.tfsIntegration.core.revision;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.VersionControlServer;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.Workstation;
import org.jetbrains.tfsIntegration.stubs.org.jetbrains.tfsIntegration.stubs.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.GetOperation;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * Date: 21.02.2008
 * Time: 14:46:02
 */
public class TFSContentRevision implements ContentRevision {
  private static final Logger LOG = Logger.getInstance(TFSContentRevision.class.getName());

  private FilePath myPath;
  private String myServerContent;
  private VcsRevisionNumber.Int myRevision;

  public TFSContentRevision(@NotNull FilePath path, int revision) {
    myPath = path;
    myRevision = new VcsRevisionNumber.Int(revision);
  }

  public String getContent() {
    if (myServerContent == null) {
      myServerContent = getServerContent();
    }
    return myServerContent;
  }

  @Nullable
  private String getServerContent() {
    try {
      // get workspace
      WorkspaceInfo workspaceInfo = Workstation.getInstance().findWorkspace(myPath.getPath());
      if (workspaceInfo == null) {
        return null;
      }
      String serverPath = workspaceInfo.findServerPathByLocalPath(myPath.getPath());
      // get server item
      GetOperation operation = workspaceInfo.get(serverPath);
      if (operation == null) {
        return null;
      }
      TFSContentStore store = TFSContentStoreFactory.find(myPath, myRevision);
      if (store == null) {
        store = TFSContentStoreFactory.create(myPath, myRevision);
        String content = VersionControlServer.downloadItem(workspaceInfo, operation);
        store.saveContent(content);
      }
      return store.loadContent();
    }
    catch (TfsException e) {
      LOG.info("Failed to get server content", e);
    }
    catch (IOException e) {
      LOG.info("Failed to get store content locally", e);
    }
    return null;
  }

  @NotNull
  public VcsRevisionNumber getRevisionNumber() {
    return myRevision;
  }

  @NotNull
  public FilePath getFile() {
    return myPath;
  }
}
