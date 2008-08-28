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

  private final ServerInfo myServer;

  private String myServerContent;

  protected TFSContentRevision(final ServerInfo server) {
    myServer = server;
  }

  @Nullable
  protected abstract Item getItem() throws TfsException;

  public static TFSContentRevision create(final @NotNull WorkspaceInfo workspace, final int itemId, final int changeset)
    throws TfsException {
    final Item item = workspace.getServer().getVCS().queryItemById(itemId, changeset, true);

    return new TFSContentRevision(workspace.getServer()) {
      @Nullable
      protected Item getItem() throws TfsException {
        return item;
      }

      @NotNull
      public FilePath getFile() {
        try {
          //noinspection ConstantConditions
          return workspace.findLocalPathByServerPath(item.getItem(), item.getType() == ItemType.Folder);
        }
        catch (TfsException e) {
          //noinspection ConstantConditions
          return null;
        }
      }

      @NotNull
      public VcsRevisionNumber getRevisionNumber() {
        return new VcsRevisionNumber.Int(changeset);
      }
    };
  }

  public static TFSContentRevision create(final @NotNull WorkspaceInfo workspace,
                                          final @NotNull ExtendedItem extendedItem,
                                          final @Nullable FilePath pathToOverride) {
    return new TFSContentRevision(workspace.getServer()) {
      @Nullable
      protected Item getItem() throws TfsException {
        int version = extendedItem.getLver() != Integer.MIN_VALUE ? extendedItem.getLver() : extendedItem.getLatest();
        return workspace.getServer().getVCS().queryItemById(extendedItem.getItemid(), version, true);
      }

      @NotNull
      public FilePath getFile() {
        if (pathToOverride != null) {
          return pathToOverride;
        }
        else {
          try {
            //noinspection ConstantConditions
            return workspace.findLocalPathByServerPath(extendedItem.getSitem(), extendedItem.getType() == ItemType.Folder);
          }
          catch (TfsException e) {
            //noinspection ConstantConditions
            return null;
          }
        }
      }

      @NotNull
      public VcsRevisionNumber getRevisionNumber() {
        return new VcsRevisionNumber.Int(extendedItem.getLver());
      }
    };
  }

  public static TFSContentRevision create(final @NotNull FilePath path, final int changeset) throws TfsException {
    final WorkspaceInfo workspace = Workstation.getInstance().findWorkspace(path);
    if (workspace == null) {
      throw new OperationFailedException("Cannot find mapping for item " + path.getPresentableUrl());
    }

    return new TFSContentRevision(workspace.getServer()) {
      @Nullable
      protected Item getItem() throws TfsException {
        return workspace.getServer().getVCS().queryItem(workspace.getName(), workspace.getOwnerName(),
                                                        VersionControlPath.toTfsRepresentation(path), new ChangesetVersionSpec(changeset),
                                                        DeletedState.Any, true);
      }

      @NotNull
      public VcsRevisionNumber getRevisionNumber() {
        return new VcsRevisionNumber.Int(changeset);
      }

      @NotNull
      public FilePath getFile() {
        return path;
      }
    };
  }

  @Nullable
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

  @Nullable
  private String loadContent() throws TfsException, IOException {
    Item item = getItem();
    if (item == null) {
      return null;
    }

    final String downloadUrl = item.getDurl();
    TFSVcs.assertTrue(item.getType() == ItemType.File, "Item: " + item.getItem() + " is not a file!");
    TFSVcs.assertTrue(downloadUrl != null, "No download url for item: " + item.getItem());
    VcsRevisionNumber.Int revisionNumber = new VcsRevisionNumber.Int(item.getCs());
    TFSContentStore store = TFSContentStoreFactory.find(myServer.getUri().toASCIIString(), item.getItemid(), revisionNumber);
    if (store == null) {
      store = TFSContentStoreFactory.create(myServer.getUri().toASCIIString(), item.getItemid(), revisionNumber);
      final Ref<TfsException> exception = new Ref<TfsException>();
      store.saveContent(new TfsFileUtil.ContentWriter() {
        public void write(final OutputStream outputStream) {
          try {
            VersionControlServer.downloadItem(myServer, downloadUrl, outputStream);
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

  @NonNls
  public String toString() {
    return "TFSContentRevision [file=" + getFile() + ", revision=" + getRevisionNumber().asString() + "]";
  }
}
