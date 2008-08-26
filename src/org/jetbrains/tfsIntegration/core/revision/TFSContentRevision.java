/*
 * Copyright 2000-2008 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.tfsIntegration.core.revision;

import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.core.tfs.version.ChangesetVersionSpec;
import org.jetbrains.tfsIntegration.exceptions.OperationFailedException;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.DeletedState;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Item;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ItemType;

import java.io.IOException;
import java.io.OutputStream;

public abstract class TFSContentRevision implements ContentRevision {

  private static class Data {
    public final ServerInfo server;
    public final Item item;
    public final int revisionNumber;
    public final FilePath localPath;

    public Data(final ServerInfo server, final Item item, final int revisionNumber, final FilePath localPath) {
      this.server = server;
      this.item = item;
      this.revisionNumber = revisionNumber;
      this.localPath = localPath;
    }
  }

  private Data myData;

  private String myServerContent;

  protected abstract Data loadData() throws TfsException;

  public static TFSContentRevision create(final @NotNull WorkspaceInfo workspace, final int itemId, final int changeset)
    throws TfsException {
    return new TFSContentRevision() {
      protected Data loadData() throws TfsException {
        final Item item = workspace.getServer().getVCS().queryItemById(itemId, changeset, true);
        TFSVcs.assertTrue(item != null, "No item found with id= " + itemId + " at revision " + changeset);

        //noinspection ConstantConditions
        final FilePath localPath = workspace.findLocalPathByServerPath(item.getItem(), item.getType() == ItemType.Folder);
        TFSVcs.assertTrue(localPath != null, "No mapping found for item :" + item.getItem());
        return new Data(workspace.getServer(), item, changeset, localPath);
      }
    };
  }

  public static TFSContentRevision create(final @NotNull WorkspaceInfo workspace,
                                          final @NotNull ExtendedItem extendedItem,
                                          final boolean useBasePath) throws TfsException {
    return new TFSContentRevision() {
      protected Data loadData() throws TfsException {
        final Item item = workspace.getServer().getVCS().queryItemById(extendedItem.getItemid(), extendedItem.getLver(), true);
        TFSVcs.assertTrue(item != null, "No item found with id= " + extendedItem.getItemid() + " at revision " + extendedItem.getLver());

        //noinspection ConstantConditions
        final FilePath localPath;
        if (useBasePath) {
          localPath = workspace.findLocalPathByServerPath(item.getItem(), item.getType() == ItemType.Folder);
        }
        else {
          localPath = VcsUtil.getFilePath(extendedItem.getLocal());
        }
        TFSVcs.assertTrue(localPath != null, "No mapping found for item :" + item.getItem());
        return new Data(workspace.getServer(), item, extendedItem.getLver(), localPath);
      }
    };
  }

  public static TFSContentRevision create(final @NotNull FilePath path, final int changeset) throws TfsException {
    return new TFSContentRevision() {
      protected Data loadData() throws TfsException {
        WorkspaceInfo workspace = Workstation.getInstance().findWorkspace(path);
        if (workspace == null) {
          throw new OperationFailedException("Cannot find mapping for item " + path.getPath());
        }
        Item item = workspace.getServer().getVCS().queryItem(workspace.getName(), workspace.getOwnerName(),
                                                             VersionControlPath.toTfsRepresentation(path),
                                                             new ChangesetVersionSpec(changeset), DeletedState.Any, true);
        return new Data(workspace.getServer(), item, changeset, path);
      }

      @NotNull
      public FilePath getFile() {
        return path;
      }
    };
  }

  public String getContent() throws VcsException {
    if (myServerContent == null) {
      try {
        myServerContent = loadContent();
      }
      catch (TfsException e) {
        throw new VcsException(e);
      }
      catch (IOException e) {
        throw new VcsException(e);
      }
    }
    return myServerContent;
  }

  private Data getData() throws TfsException {
    if (myData == null) {
      myData = loadData();
    }
    return myData;
  }

  @Nullable
  private String loadContent() throws TfsException, IOException {
    final Data data = getData();

    if (data.item == null) {
      return null;
    }

    final String downloadUrl = data.item.getDurl();
    TFSVcs.assertTrue(data.item.getType() == ItemType.File, "Item: " + data.item.getItem() + " is not a file!");
    TFSVcs.assertTrue(downloadUrl != null, "No download url for file item: " + data.item.getItem());
    VcsRevisionNumber.Int revisionNumber = new VcsRevisionNumber.Int(data.revisionNumber);
    TFSContentStore store = TFSContentStoreFactory.find(data.server.getUri().toASCIIString(), data.item.getItemid(), revisionNumber);
    if (store == null) {
      store = TFSContentStoreFactory.create(data.server.getUri().toASCIIString(), data.item.getItemid(), revisionNumber);
      final Ref<TfsException> exception = new Ref<TfsException>();
      store.saveContent(new TfsFileUtil.ContentWriter() {
        public void write(final OutputStream outputStream) {
          try {
            VersionControlServer.downloadItem(data.server, downloadUrl, outputStream);
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

  @NotNull
  public VcsRevisionNumber getRevisionNumber() {
    try {
      return new VcsRevisionNumber.Int(getData().revisionNumber);
    }
    catch (TfsException e) {
      TFSVcs.LOG.error(e);
      return VcsRevisionNumber.NULL;
    }
  }

  @NotNull
  public FilePath getFile() {
    try {
      return getData().localPath;
    }
    catch (TfsException e) {
      TFSVcs.LOG.error(e);
      //noinspection ConstantConditions
      return null;
    }
  }

  @NonNls
  public String toString() {
    return "TFSContentRevision [file=" + getFile() + ", revision=" + getRevisionNumber().asString() + "]";
  }
}
