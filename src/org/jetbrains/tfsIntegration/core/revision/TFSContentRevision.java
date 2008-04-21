package org.jetbrains.tfsIntegration.core.revision;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.VersionControlServer;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.Workstation;
import org.jetbrains.tfsIntegration.core.tfs.version.ChangesetVersionSpec;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.DeletedState;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Item;

import java.io.IOException;
import java.io.OutputStream;

public class TFSContentRevision implements ContentRevision {
  private static final Logger LOG = Logger.getInstance(TFSContentRevision.class.getName());

  private FilePath myPath;
  private String myServerContent;
  private VcsRevisionNumber.Int myRevisionNumber;

  public TFSContentRevision(@NotNull FilePath path, int changeset) {
    myPath = path;
    myRevisionNumber = new VcsRevisionNumber.Int(changeset);
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
      final WorkspaceInfo workspace = Workstation.getInstance().findWorkspace(myPath);
      if (workspace == null) {
        return null;
      }
      String serverPath = workspace.findServerPathByLocalPath(myPath);
      // get server item
      final Item item = workspace.getServer().getVCS().queryItem(workspace.getName(), workspace.getOwnerName(), serverPath, new ChangesetVersionSpec(
        myRevisionNumber.getValue()), DeletedState.NonDeleted, true);
      if (item == null) {
        return null;
      }
      final String downloadUrl = item.getDurl();
      TFSContentStore store = TFSContentStoreFactory.find(myPath, myRevisionNumber);
      if (store == null) {
        store = TFSContentStoreFactory.create(myPath, myRevisionNumber);
        final Ref<TfsException> exception = new Ref<TfsException>();
        store.saveContent(new TFSContentStore.ContentWriter() {
          public void write(final OutputStream outputStream) {
            try {
              VersionControlServer.downloadItem(workspace, downloadUrl, outputStream);
            }
            catch (TfsException e) {
              exception.set(e);
            }
          }
        });
        if (!exception.isNull()) {
          throw exception.get();
        }
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
    return myRevisionNumber;
  }

  @NotNull
  public FilePath getFile() {
    return myPath;
  }
}
