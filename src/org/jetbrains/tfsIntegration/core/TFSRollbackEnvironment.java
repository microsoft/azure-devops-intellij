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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.rollback.RollbackEnvironment;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.core.tfs.operations.ApplyGetOperations;
import org.jetbrains.tfsIntegration.core.tfs.operations.UndoPendingChanges;
import org.jetbrains.tfsIntegration.core.tfs.version.ChangesetVersionSpec;
import org.jetbrains.tfsIntegration.core.tfs.version.WorkspaceVersionSpec;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.GetOperation;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.RecursionType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
    return undoPendingChanges(localPaths);
  }

  public List<VcsException> rollbackMissingFileDeletion(final List<FilePath> files) {
    final List<VcsException> errors = new ArrayList<VcsException>();
    try {
      WorkstationHelper.processByWorkspaces(files, false, new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          final List<VersionControlServer.GetRequestParams> download = new ArrayList<VersionControlServer.GetRequestParams>();
          final Collection<String> undo = new ArrayList<String>();
          StatusProvider.visitByStatus(workspace, paths, false, null, new StatusVisitor() {

            public void unversioned(final @NotNull FilePath localPath,
                                    final boolean localItemExists,
                                    final @NotNull ServerStatus serverStatus) throws TfsException {
              TFSVcs
                .error("Server status Unversioned when rolling back missing file deletion: " + localPath.getPresentableUrl());
            }

            public void checkedOutForEdit(final @NotNull FilePath localPath,
                                          final boolean localItemExists,
                                          final @NotNull ServerStatus serverStatus) {
              undo.add(serverStatus.targetItem);
            }

            public void scheduledForAddition(final @NotNull FilePath localPath,
                                             final boolean localItemExists,
                                             final @NotNull ServerStatus serverStatus) {
              undo.add(serverStatus.targetItem);
            }

            public void scheduledForDeletion(final @NotNull FilePath localPath,
                                             final boolean localItemExists,
                                             final @NotNull ServerStatus serverStatus) {
              TFSVcs
                .error("Server status ScheduledForDeletion when rolling back missing file deletion: " + localPath.getPresentableUrl());
            }

            public void outOfDate(final @NotNull FilePath localPath,
                                  final boolean localItemExists,
                                  final @NotNull ServerStatus serverStatus) throws TfsException {
              //noinspection ConstantConditions
              addForDownload(localPath, localItemExists, serverStatus.targetItem, serverStatus.localVer);
            }

            public void deleted(final @NotNull FilePath localPath,
                                final boolean localItemExists,
                                final @NotNull ServerStatus serverStatus) {
              TFSVcs.error("Server status Deleted when rolling back missing file deletion: " + localPath.getPath());
            }

            public void upToDate(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
              throws TfsException {
              //noinspection ConstantConditions
              addForDownload(localPath, localItemExists, serverStatus.targetItem, serverStatus.localVer);
            }

            public void renamed(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
              throws TfsException {
              undo.add(serverStatus.targetItem);
            }

            public void renamedCheckedOut(final @NotNull FilePath localPath,
                                          final boolean localItemExists,
                                          final @NotNull ServerStatus serverStatus) throws TfsException {
              undo.add(serverStatus.targetItem);
            }

            private void addForDownload(final @NotNull FilePath localPath,
                                        final boolean localItemExists,
                                        final @NotNull String serverItem,
                                        int version) {
              download.add(new VersionControlServer.GetRequestParams(serverItem, RecursionType.None, new ChangesetVersionSpec(version)));
            }

          });

          List<GetOperation> operations = workspace.getServer().getVCS().get(workspace.getName(), workspace.getOwnerName(), download);
          final Collection<VcsException> downloadErrors = ApplyGetOperations
            .execute(myProject, workspace, operations, null, null, ApplyGetOperations.DownloadMode.FORCE);
          errors.addAll(downloadErrors);

          final UndoPendingChanges.UndoPendingChangesResult undoResult = UndoPendingChanges.execute(myProject, workspace, undo, false);
          errors.addAll(undoResult.errors);
        }
      });
    }
    catch (TfsException e) {
      return Collections.singletonList(new VcsException(e.getMessage(), e));
    }
    return errors;
  }

  public List<VcsException> rollbackModifiedWithoutCheckout(final List<VirtualFile> files) {
    final List<VcsException> errors = new ArrayList<VcsException>();
    try {
      WorkstationHelper.processByWorkspaces(TfsFileUtil.getFilePaths(files), false, new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          // query extended items to determine base (local) version
          //Map<ItemPath, ExtendedItem> extendedItems = workspace.getExtendedItems(paths);

          // query GetOperation-s
          List<VersionControlServer.GetRequestParams> requests = new ArrayList<VersionControlServer.GetRequestParams>(paths.size());
          final WorkspaceVersionSpec versionSpec = new WorkspaceVersionSpec(workspace.getName(), workspace.getOwnerName());
          for (ItemPath e : paths) {
            requests.add(new VersionControlServer.GetRequestParams(e.getServerPath(), RecursionType.None, versionSpec));
          }
          List<GetOperation> operations = workspace.getServer().getVCS().get(workspace.getName(), workspace.getOwnerName(), requests);
          final Collection<VcsException> applyingErrors =
            ApplyGetOperations.execute(myProject, workspace, operations, null, null, ApplyGetOperations.DownloadMode.FORCE);
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
    final List<VcsException> errors = undoPendingChanges(Collections.singletonList(TfsFileUtil.getFilePath(file)));
    if (!errors.isEmpty()) {
      AbstractVcsHelper.getInstance(myProject).showErrors(errors, TFSVcs.TFS_NAME);
    }
  }

  private List<VcsException> undoPendingChanges(final List<FilePath> localPaths) {
    final List<VcsException> errors = new ArrayList<VcsException>();
    try {
      WorkstationHelper.processByWorkspaces(localPaths, false, new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          Collection<String> serverPaths = new ArrayList<String>(paths.size());
          for (ItemPath itemPath : paths) {
            serverPaths.add(itemPath.getServerPath());
          }
          UndoPendingChanges.UndoPendingChangesResult undoResult = UndoPendingChanges.execute(myProject, workspace, serverPaths, false);
          errors.addAll(undoResult.errors);
          List<VirtualFile> refresh = new ArrayList<VirtualFile>(paths.size());
          for (ItemPath path : paths) {
            ItemPath undone = undoResult.undonePaths.get(path);
            FilePath subject = (undone != null ? undone : path).getLocalPath();
            VirtualFile file = subject.getVirtualFileParent();
            if (file != null && file.exists()) {
              refresh.add(file);
            }
          }
          TfsFileUtil.refreshAndInvalidate(myProject, refresh, true);
        }
      });
      return errors;
    }
    catch (TfsException e) {
      return Collections.singletonList(new VcsException("Failed to undo pending changes", e));
    }
  }


}
