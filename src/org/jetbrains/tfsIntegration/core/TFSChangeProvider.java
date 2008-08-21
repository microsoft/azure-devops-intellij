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

package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.ChangeProvider;
import com.intellij.openapi.vcs.changes.ChangelistBuilder;
import com.intellij.openapi.vcs.changes.VcsDirtyScope;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.*;

import java.util.*;

/**
 * TODO important cases
 * 1. when folder1 is unversioned and folder1/file1 is scheduled for addition, team explorer effectively shows folder1 as scheduled for addition
 */

public class TFSChangeProvider implements ChangeProvider {

  private Project myProject;

  public TFSChangeProvider(final Project project) {
    myProject = project;
  }

  public boolean isModifiedDocumentTrackingRequired() {
    return true;
  }

  public void getChanges(final VcsDirtyScope dirtyScope, final ChangelistBuilder builder, final ProgressIndicator progress)
    throws VcsException {
    if (myProject.isDisposed()) {
      return;
    }
    if (builder == null) {
      return;
    }

    progress.setText("Processing changes");

    RootsCollection.FilePathRootsCollection roots = new RootsCollection.FilePathRootsCollection();
    roots.addAll(dirtyScope.getRecursivelyDirtyDirectories());
    roots.addAll(dirtyScope.getDirtyFiles());

    try {
      // unwrap child workspaces
      // TODO: is it always correct to use RootsCollection.FilePathRootsCollection instead of HashSet?
      Set<FilePath> mappedRoots = new HashSet<FilePath>();
      for (FilePath root : roots) {
        Set<FilePath> mappedPaths = Workstation.getInstance().findChildMappedPaths(root);
        if (!mappedPaths.isEmpty()) {
          mappedRoots.addAll(mappedPaths);
        }
        else {
          mappedRoots.add(root);
        }
      }

      // ingore orphan roots here
      WorkstationHelper.processByWorkspaces(mappedRoots, new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          processWorkspace(workspace, paths, builder, progress);
        }
      });
    }
    catch (TfsException e) {
      throw new VcsException(e.getMessage(), e);
    }
  }

  //  TODO FIXME respect nearest mapping here!!!
  private static void processWorkspace(final @NotNull WorkspaceInfo workspace,
                                       final List<ItemPath> roots,
                                       ChangelistBuilder builder,
                                       final @Nullable ProgressIndicator progress) throws TfsException {
    TFSProgressUtil.checkCanceled(progress);

    List<ItemSpec> itemSpecs = new ArrayList<ItemSpec>(roots.size());
    for (ItemPath root : roots) {
      final VirtualFile file = root.getLocalPath().getVirtualFile();
      RecursionType recursionType = (file != null && file.exists() && !file.isDirectory()) ? RecursionType.None : RecursionType.Full;
      itemSpecs.add(VersionControlServer.createItemSpec(root.getServerPath(), recursionType));
    }

    List<List<ExtendedItem>> extendedItemsResult = workspace.getServer().getVCS()
      .getExtendedItems(workspace.getName(), workspace.getOwnerName(), itemSpecs, DeletedState.Any, ItemType.Any);

    Map<ItemPath, ExtendedItem> local2ExtendedItem = new HashMap<ItemPath, ExtendedItem>();
    for (int i = 0; i < roots.size(); i++) {
      ItemPath path = roots.get(i);

      Collection<ExtendedItem> serverItems = extendedItemsResult.get(i);
      Collection<FilePath> localItems = new TreeSet<FilePath>(TfsFileUtil.PATH_COMPARATOR);
      localItems.add(path.getLocalPath());
      addExistingFilesRecursively(localItems, path.getLocalPath().getVirtualFile());

      // find 'downloaded' server items for existing local files
      for (FilePath localItem : localItems) {
        if (workspace.isWorkingFolder(localItem) ) {
          // report mapping root as up to date
          continue;
        }
        ExtendedItem serverItem = null;
        for (ExtendedItem candidate : serverItems) {
          if (VersionControlPath.toTfsRepresentation(localItem.getPath()).equals(candidate.getLocal())) {
            serverItem = candidate;
            break;
          }
        }

        if (serverItem != null) {
          serverItems.remove(serverItem);
        }
        local2ExtendedItem.put(new ItemPath(localItem, workspace.findServerPathByLocalPath(localItem)), serverItem);
      }

      // process locally missing items
      for (ExtendedItem serverItem : serverItems) {
        if (serverItem.getLocal() != null || !EnumMask.fromString(ChangeType.class, serverItem.getChg()).isEmpty()) {
          final String localPath;
          if (serverItem.getLocal() != null) {
            localPath = serverItem.getLocal();
          }
          else {
            localPath = workspace.findLocalPathByServerPath(serverItem.getTitem()).getPath();
          }
          local2ExtendedItem.put(
            new ItemPath(VcsUtil.getFilePathForDeletedFile(localPath, serverItem.getType() == ItemType.Folder), serverItem.getTitem()),
            serverItem);
        }
      }
    }

    StatusVisitor statusVisitor = new ChangelistBuilderStatusVisitor(builder, workspace);
    for (Map.Entry<ItemPath, ExtendedItem> entry : local2ExtendedItem.entrySet()) {
      ItemPath itemPath = entry.getKey();
      ExtendedItem serverItem = entry.getValue();
      ServerStatus status = StatusProvider.determineServerStatus(serverItem);

      VirtualFile file = entry.getKey().getLocalPath().getVirtualFile();
      boolean localItemExists = file != null && file.exists();
      if (!localItemExists && serverItem != null) {
        // if path is the original one from dirtyScope, it may have invalid 'isDirectory' status
        itemPath = new ItemPath(
          VcsUtil.getFilePathForDeletedFile(itemPath.getLocalPath().getPath(), serverItem.getType() == ItemType.Folder),
          itemPath.getServerPath());
      }
      status.visitBy(itemPath, statusVisitor, localItemExists);
    }
  }

  private static void addExistingFilesRecursively(final @NotNull Collection<FilePath> result, final @Nullable VirtualFile root) {
    if (root != null && root.exists()) {
      result.add(TfsFileUtil.getFilePath(root));
      if (root.isDirectory()) {
        for (VirtualFile child : root.getChildren()) {
          addExistingFilesRecursively(result, child);
        }
      }
    }
  }

}
