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

import com.intellij.openapi.project.Project;
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
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Item;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ItemType;

import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.Collection;

public abstract class TFSContentRevision implements ContentRevision {

  private final Project myProject;

  private final ServerInfo myServer;

  private String myServerContent;

  protected TFSContentRevision(final Project project, final ServerInfo server) {
    myProject = project;
    myServer = server;
  }

  @Nullable
  protected abstract Item getItem() throws TfsException;

  protected abstract int getItemId() throws TfsException;

  protected abstract int getChangeset() throws TfsException;

  public static TFSContentRevision create(final Project project,
                                          final @NotNull WorkspaceInfo workspace,
                                          final int changeset,
                                          final int itemId) throws TfsException {
    final Item item = workspace.getServer().getVCS().queryItemById(itemId, changeset, true);

    return new TFSContentRevision(project, workspace.getServer()) {
      @Nullable
      protected Item getItem() {
        return item;
      }

      protected int getItemId() {
        return itemId;
      }

      protected int getChangeset() throws TfsException {
        return changeset;
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

  public static TFSContentRevision create(final Project project,
                                          final @NotNull WorkspaceInfo workspace,
                                          final @NotNull FilePath localPath,
                                          final int changeset,
                                          final int itemId) {
    return new TFSContentRevision(project, workspace.getServer()) {
      @Nullable
      protected Item getItem() throws TfsException {
        return workspace.getServer().getVCS().queryItemById(itemId, changeset, true);
      }

      protected int getItemId() {
        return itemId;
      }

      protected int getChangeset() {
        return changeset;
      }

      @NotNull
      public FilePath getFile() {
        return localPath;
      }

      @NotNull
      public VcsRevisionNumber getRevisionNumber() {
        return new VcsRevisionNumber.Int(changeset);
      }
    };
  }

  public static TFSContentRevision create(final Project project, final @NotNull FilePath localPath, final int changeset)
    throws TfsException {
    final Collection<WorkspaceInfo> workspaces = Workstation.getInstance().findWorkspaces(localPath, false);
    if (workspaces.isEmpty()) {
      throw new OperationFailedException("Cannot find mapping for item " + localPath.getPresentableUrl());
    }

    final WorkspaceInfo workspace = workspaces.iterator().next();
    return new TFSContentRevision(project, workspace.getServer()) {
      private @Nullable Item myItem;

      @Nullable
      protected Item getItem() throws TfsException {
        if (myItem == null) {
          myItem = workspace.getServer().getVCS()
            .queryItem(workspace.getName(), workspace.getOwnerName(), VersionControlPath.toTfsRepresentation(localPath),
                       new ChangesetVersionSpec(changeset), DeletedState.Any, true);
        }
        return myItem;
      }

      protected int getItemId() throws TfsException {
        Item item = getItem();
        return item != null ? item.getItemid() : Integer.MIN_VALUE;
      }

      protected int getChangeset() throws TfsException {
        Item item = getItem();
        return item != null ? item.getCs() : Integer.MIN_VALUE;
      }

      @NotNull
      public VcsRevisionNumber getRevisionNumber() {
        return new VcsRevisionNumber.Int(changeset);
      }

      @NotNull
      public FilePath getFile() {
        return localPath;
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
    int itemId = getItemId();
    int changeset = getChangeset();

    TFSContentStore store = TFSContentStoreFactory.find(myServer.getUri().toASCIIString(), itemId, changeset);
    if (store == null) {
      Item item = getItem();
      if (item == null) {
        return null;
      }
      if (item.getType() == ItemType.Folder) {
        String message = MessageFormat.format("''{0}'' refers to a folder", getFile().getPresentableUrl());
        throw new OperationFailedException(message);
      }

      final String downloadUrl = item.getDurl();
      TFSVcs.assertTrue(downloadUrl != null, "Item without download URL: " + item.getItem());

      store = TFSContentStoreFactory.create(myServer.getUri().toASCIIString(), itemId, changeset);
      final Ref<TfsException> exception = new Ref<TfsException>();
      store.saveContent(new TfsFileUtil.ContentWriter() {
        public void write(final OutputStream outputStream) {
          try {
            VersionControlServer.downloadItem(myProject, myServer, downloadUrl, outputStream);
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
