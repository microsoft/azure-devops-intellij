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

  private final @NotNull FilePath myPath;
  private String myServerContent;
  private final @NotNull VcsRevisionNumber.Int myRevisionNumber;

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
      TFSVcs.assertTrue(downloadUrl != null, "Null download url for item :" + item.getItem() + "'");
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

  @NonNls
  public String toString() {
    return "TFSContentRevision [file=" + getFile() + ", revision=" + getRevisionNumber().asString() + "]";
  }
}
