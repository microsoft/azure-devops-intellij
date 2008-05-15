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

import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.rollback.RollbackEnvironment;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.core.tfs.operations.UndoPendingChanges;
import org.jetbrains.tfsIntegration.core.tfs.operations.ApplyGetOperations;
import org.jetbrains.tfsIntegration.core.tfs.version.ChangesetVersionSpec;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.GetOperation;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.RecursionType;

import java.util.*;

public class TFSRollbackEnvironment implements RollbackEnvironment {

  private final @NotNull Project myProject;

  public TFSRollbackEnvironment(final Project project) {
    myProject = project;
  }

  public String getRollbackOperationName() {
    return "Undo Pending Changes";
  }

  @SuppressWarnings({"ConstantConditions"})
  public List<VcsException> rollbackChanges(final List<Change> changes) {
    List<FilePath> localPaths = new ArrayList<FilePath>();
    for (Change change : changes) {
      ContentRevision revision = change.getType() == Change.Type.DELETED ? change.getBeforeRevision() : change.getAfterRevision();
      localPaths.add(revision.getFile());
    }
    return undoPendingChanges(localPaths, true);
  }

  public List<VcsException> rollbackMissingFileDeletion(final List<FilePath> files) {
    final List<VcsException> errors = new ArrayList<VcsException>();
    try {
      WorkstationHelper.processByWorkspaces(files, new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          Map<ItemPath, ServerStatus> local2serverStatus = StatusProvider.determineServerStatus(workspace, paths);
          final List<VersionControlServer.GetRequestParams> download = new ArrayList<VersionControlServer.GetRequestParams>();
          final List<ItemPath> undo = new ArrayList<ItemPath>();
          for (Map.Entry<ItemPath, ServerStatus> e : local2serverStatus.entrySet()) {
            e.getValue().visitBy(e.getKey(), new StatusVisitor() {

              public void unversioned(@NotNull final ItemPath path,
                                      final @Nullable ExtendedItem extendedItem,
                                      final boolean localItemExists) throws TfsException {
                TFSVcs.error("Server status Unversioned when rolling back missing file deletion: " + path.getLocalPath().getPath());
              }

              public void checkedOutForEdit(@NotNull final ItemPath path,
                                            final @NotNull ExtendedItem extendedItem,
                                            final boolean localItemExists) {
                undo.add(path);
              }

              public void scheduledForAddition(@NotNull final ItemPath path,
                                               final @NotNull ExtendedItem extendedItem,
                                               final boolean localItemExists) {
                undo.add(path);
              }

              public void scheduledForDeletion(@NotNull final ItemPath path,
                                               final @NotNull ExtendedItem extendedItem,
                                               final boolean localItemExists) {
                TFSVcs
                  .error("Server status ScheduledForDeletion when rolling back missing file deletion: " + path.getLocalPath().getPath());
                //undo.add(path);
                //addForDownload(extendedItem);
              }

              public void outOfDate(final @NotNull ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists)
                throws TfsException {
                addForDownload(extendedItem);
              }

              public void deleted(final @NotNull ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists) {
                TFSVcs.error("Server status Deleted when rolling back missing file deletion: " + path.getLocalPath().getPath());
              }

              public void upToDate(final @NotNull ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists)
                throws TfsException {
                addForDownload(extendedItem);
              }

              public void renamed(final @NotNull ItemPath path, @NotNull final ExtendedItem extendedItem, final boolean localItemExists)
                throws TfsException {
                undo.add(path);
              }

              public void renamedCheckedOut(final @NotNull ItemPath path, @NotNull final ExtendedItem extendedItem, final boolean localItemExists)
                throws TfsException {
                undo.add(path);
              }

              private void addForDownload(final @NotNull ExtendedItem extendedItem) {
                download.add(new VersionControlServer.GetRequestParams(extendedItem.getSitem(), RecursionType.None,
                                                                       new ChangesetVersionSpec(extendedItem.getLver())));
              }

            }, false);
          }

          final UndoPendingChanges.UndoPendingChangesResult undoResult = UndoPendingChanges.execute(workspace, undo, true);
          errors.addAll(undoResult.errors);

          List<List<GetOperation>> getOperations =
            workspace.getServer().getVCS().get(workspace.getName(), workspace.getOwnerName(), download);

          for (List<GetOperation> list : getOperations) {
            VersionControlServer.downloadItem(workspace, list.get(0), true, true, true);
          }
        }
      });
    }
    catch (TfsException e) {
      return Collections.singletonList(new VcsException("Failed to rollback file", e));
    }
    return errors;
  }

  public List<VcsException> rollbackModifiedWithoutCheckout(final List<VirtualFile> files) {
    final List<VcsException> errors = new ArrayList<VcsException>();
    try {
      WorkstationHelper.processByWorkspaces(TfsFileUtil.getFilePaths(files), new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          // query extended items to determine base (local) version
          Map<ItemPath, ExtendedItem> extendedItems = workspace.getExtendedItems(paths);

          // query GetOperation-s
          List<VersionControlServer.GetRequestParams> requests = new ArrayList<VersionControlServer.GetRequestParams>(extendedItems.size());
          for (Map.Entry<ItemPath, ExtendedItem> e : extendedItems.entrySet()) {
            requests.add(new VersionControlServer.GetRequestParams(e.getKey().getServerPath(), RecursionType.None,
                                                                   new ChangesetVersionSpec(e.getValue().getLver())));
          }
          List<List<GetOperation>> operations = workspace.getServer().getVCS().get(workspace.getName(), workspace.getOwnerName(), requests);
          Collection<GetOperation> allOperations = new ArrayList<GetOperation>();
          for (List<GetOperation> ops : operations) {
            allOperations.addAll(ops);
          }

          final Collection<VcsException> applyingErrors = ApplyGetOperations.execute(workspace, allOperations, true);
          errors.addAll(applyingErrors);
        }
      });

      return errors;
    }
    catch (TfsException e) {
      return Collections.singletonList(new VcsException("Failed to undo pending changes", e));
    }
  }

  public void rollbackIfUnchanged(final VirtualFile file) {
    final List<VcsException> errors = undoPendingChanges(Collections.singletonList(TfsFileUtil.getFilePath(file)), false);
    if (!errors.isEmpty()) {
      AbstractVcsHelper.getInstance(myProject).showErrors(errors, TFSVcs.TFS_NAME);
    }
  }

  private static List<VcsException> undoPendingChanges(final List<FilePath> localPaths, final boolean updateToBaseVersion) {
    final List<VcsException> errors = new ArrayList<VcsException>();
    try {
      WorkstationHelper.processByWorkspaces(localPaths, new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          UndoPendingChanges.UndoPendingChangesResult undoResult = UndoPendingChanges.execute(workspace, paths, true);
          errors.addAll(undoResult.errors);
        }
      });
      return errors;
    }
    catch (TfsException e) {
      return Collections.singletonList(new VcsException("Failed to undo pending changes", e));
    }
  }


}
