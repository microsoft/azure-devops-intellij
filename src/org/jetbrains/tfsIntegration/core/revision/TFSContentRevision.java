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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.FilePath;
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

public class TFSContentRevision implements ContentRevision {
  private static final Logger LOG = Logger.getInstance(TFSContentRevision.class.getName());

  private String myServerContent;
  private final FilePath myPath;
  private final ServerInfo myServer;
  private final Item myItem;
  private final VcsRevisionNumber.Int myRevisionNumber;

  public TFSContentRevision(final @NotNull WorkspaceInfo workspace, final @NotNull ExtendedItem extendedItem, final boolean useBasePath)
    throws TfsException {
    myServer = workspace.getServer();
    myRevisionNumber = new VcsRevisionNumber.Int(extendedItem.getLver());
    myPath = workspace.findLocalPathByServerPath(useBasePath ? extendedItem.getSitem() : extendedItem.getTitem(),
                                                 extendedItem.getType() == ItemType.Folder);
    myItem = workspace.getServer().getVCS().queryItemById(extendedItem.getItemid(), extendedItem.getLver(), true);
  }

  public TFSContentRevision(final @NotNull WorkspaceInfo workspace, int itemId, int changeset) throws TfsException {
    myServer = workspace.getServer();
    myRevisionNumber = new VcsRevisionNumber.Int(changeset);
    myItem = workspace.getServer().getVCS().queryItemById(itemId, changeset, true);
    TFSVcs.assertTrue(myItem != null, "No item found with id= " + itemId + " at revision " + changeset);

    myPath = workspace.findLocalPathByServerPath(myItem.getItem(), myItem.getType() == ItemType.Folder);
    TFSVcs.assertTrue(myPath != null, "No mapping found for item :" + myItem.getItem());
  }

  public TFSContentRevision(final @NotNull FilePath path, int changeset) throws TfsException {
    myPath = path;
    myRevisionNumber = new VcsRevisionNumber.Int(changeset);
    WorkspaceInfo workspace = Workstation.getInstance().findWorkspace(myPath);
    if (workspace == null) {
      throw new OperationFailedException("Cannot find mapping for item " + path.getPath());
    }
    myServer = workspace.getServer();
    myItem = workspace.getServer().getVCS().queryItem(workspace.getName(), workspace.getOwnerName(),
                                                        VersionControlPath.toTfsRepresentation(myPath), new ChangesetVersionSpec(changeset),
                                                        DeletedState.Any, true);
  }

  public String getContent() {
    if (myServerContent == null) {
      myServerContent = getServerContent();
    }
    return myServerContent;
  }

  @Nullable
  private String getServerContent() {
    if (myItem == null) {
      return null;
    }
    try {
      final String downloadUrl = myItem.getDurl();
      TFSVcs.assertTrue(myItem.getType() == ItemType.File, "Item: " + myItem.getItem() + " is not a file!");
      TFSVcs.assertTrue(downloadUrl != null, "No download url for file item: " + myItem.getItem());
      TFSContentStore store =
        TFSContentStoreFactory.find(myServer.getUri().toASCIIString(), myItem.getItemid(), myRevisionNumber);
      if (store == null) {
        store = TFSContentStoreFactory.create(myServer.getUri().toASCIIString(), myItem.getItemid(), myRevisionNumber);
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

  @NonNls
  public String toString() {
    return "TFSContentRevision [file=" + getFile() + ", revision=" + getRevisionNumber().asString() + "]";
  }
}
