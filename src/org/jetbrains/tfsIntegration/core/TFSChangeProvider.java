package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
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
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.DeletedState;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ItemType;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.RecursionType;

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

    Set<FilePath> paths = new HashSet<FilePath>();
    for (FilePath p : dirtyScope.getRecursivelyDirtyDirectories()) {
      addPathSmart(paths, p);
    }
    for (FilePath p : dirtyScope.getDirtyFiles()) {
      addPathSmart(paths, p);
    }

    try {
      // ingore orphan paths here
      WorkstationHelper.processByWorkspaces(paths, new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          processWorkspace(workspace, paths, builder, progress);
        }
      });
    }
    catch (TfsException e) {
      throw new VcsException("Failed to determine items status", e);
    }
  }

  private static void processWorkspace(final WorkspaceInfo workspace,
                                       final List<ItemPath> paths,
                                       ChangelistBuilder builder,
                                       final @Nullable ProgressIndicator progress) throws TfsException {
    StatusVisitor statusVisitor = new ChangelistBuilderStatusVisitor(builder);
    Map<ItemPath, ExtendedItem> extendedItems = new HashMap<ItemPath, ExtendedItem>();

    TFSProgressUtil.checkCanceled(progress);
    List<List<ExtendedItem>> extendedItemsResult = workspace.getServer().getVCS()
      .getExtendedItems(workspace.getName(), workspace.getOwnerName(), paths, DeletedState.NonDeleted, RecursionType.Full, ItemType.Any);
    for (int i = 0; i < paths.size(); i++) {
      ItemPath path = paths.get(i);

      Collection<ExtendedItem> serverItems = extendedItemsResult.get(i);
      Collection<FilePath> localItems = new HashSet<FilePath>();
      localItems.add(path.getLocalPath());
      addExistingFilesRecursively(localItems, path.getLocalPath().getVirtualFile());

      // find 'downloaded' server items for existing local files
      for (FilePath localItem : localItems) {
        if (workspace.isWorkingFolder(localItem)) {
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
        extendedItems.put(new ItemPath(localItem, workspace.findServerPathByLocalPath(localItem)), serverItem);
      }

      // find locally missing items
      for (ExtendedItem serverItem : serverItems) {
        if (serverItem.getLocal() != null) {
          extendedItems.put(new ItemPath(VcsUtil.getFilePathForDeletedFile(serverItem.getLocal(), serverItem.getType() == ItemType.Folder),
                                         serverItem.getTitem()), serverItem);
        }
      }
    }

    for (Map.Entry<ItemPath, ExtendedItem> entry : extendedItems.entrySet()) {
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
      //System.out
      //  .println(entry.getKey().getLocalPath().getPath() + ": " + status + (localItemExists ? ", exists locally" : ", missing locally"));
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

  private static void addPathSmart(Collection<FilePath> existingPaths, FilePath newPath) {
    Collection<FilePath> toRemove = new ArrayList<FilePath>();
    for (FilePath existing : existingPaths) {
      if (FileUtil.pathsEqual(newPath.toString(), existing.toString()) || newPath.isUnder(existing, false)) {
        return;
      }
      if (existing.isUnder(newPath, false)) {
        toRemove.add(existing);
      }
    }
    existingPaths.removeAll(toRemove);
    existingPaths.add(newPath);
  }


}
