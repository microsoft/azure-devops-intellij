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
import com.intellij.openapi.vcs.VcsVFSListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.core.tfs.operations.ApplyProgress;
import org.jetbrains.tfsIntegration.core.tfs.operations.ScheduleForAddition;
import org.jetbrains.tfsIntegration.core.tfs.operations.ScheduleForDeletion;
import org.jetbrains.tfsIntegration.core.tfs.operations.UndoPendingChanges;
import org.jetbrains.tfsIntegration.exceptions.TfsException;

import java.util.*;

public class TFSFileListener extends VcsVFSListener {

  public TFSFileListener(Project project, TFSVcs vcs) {
    super(project, vcs);
  }

  protected String getAddTitle() {
    return "Do you want to schedule addition of these items to TFS?";
  }

  protected String getSingleFileAddTitle() {
    return null;
  }

  protected String getSingleFileAddPromptTemplate() {
    return null;
  }

  protected void executeAdd() {
    try {
      WorkstationHelper.processByWorkspaces(TfsFileUtil.getFilePaths(myAddedFiles), false, myProject,
                                            new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          StatusProvider.visitByStatus(workspace, paths, false, null, new StatusVisitor() {
            public void unversioned(final @NotNull FilePath localPath,
                                    final boolean localItemExists,
                                    final @NotNull ServerStatus serverStatus) throws TfsException {
              // ignore
            }

            public void checkedOutForEdit(final @NotNull FilePath localPath,
                                          final boolean localItemExists,
                                          final @NotNull ServerStatus serverStatus) {
              // TODO: add local conflict
            }

            public void scheduledForAddition(final @NotNull FilePath localPath,
                                             final boolean localItemExists,
                                             final @NotNull ServerStatus serverStatus) {
              myAddedFiles.remove(localPath.getVirtualFile());
            }

            public void scheduledForDeletion(final @NotNull FilePath localPath,
                                             final boolean localItemExists,
                                             final @NotNull ServerStatus serverStatus) {
              // TODO: add local conflict
            }

            public void outOfDate(final @NotNull FilePath localPath,
                                  final boolean localItemExists,
                                  final @NotNull ServerStatus serverStatus) throws TfsException {
              // TODO: add local conflict
            }

            public void deleted(final @NotNull FilePath localPath,
                                final boolean localItemExists,
                                final @NotNull ServerStatus serverStatus) {
              // ignore
            }

            public void upToDate(final @NotNull FilePath localPath,
                                 final boolean localItemExists,
                                 final @NotNull ServerStatus serverStatusm) throws TfsException {
              // TODO: add local conflict
            }

            public void renamed(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
              throws TfsException {
              // TODO: add local conflict
            }

            public void renamedCheckedOut(final @NotNull FilePath localPath,
                                          final boolean localItemExists,
                                          final @NotNull ServerStatus serverStatus) throws TfsException {
              // TODO: add local conflict
            }

            public void undeleted(final @NotNull FilePath localPath,
                                  final boolean localItemExists,
                                  final @NotNull ServerStatus serverStatus) throws TfsException {
              // TODO: add local conflict
            }

          }, myProject);
        }
      });
    }
    catch (TfsException e) {
      AbstractVcsHelper.getInstance(myProject).showError(new VcsException(e), TFSVcs.TFS_NAME);
    }
    if (!myAddedFiles.isEmpty()) {
      super.executeAdd();
    }
  }

  protected void executeDelete() {
    // choose roots
    // revert all pending schedules for addition recursively
    // throw out all the unversioned items

    List<FilePath> deletedFiles = new ArrayList<FilePath>(myDeletedFiles);
    deletedFiles.addAll(myDeletedWithoutConfirmFiles);

    try {
      WorkstationHelper.processByWorkspaces(deletedFiles, false, myProject, new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          RootsCollection.ItemPathRootsCollection roots = new RootsCollection.ItemPathRootsCollection(paths);

          final Collection<PendingChange> pendingChanges = workspace.getServer().getVCS()
            .queryPendingSetsByLocalPaths(workspace.getName(), workspace.getOwnerName(), roots, RecursionType.Full, myProject,
                                          TFSBundle.message("loading.changes"));

          final List<String> revertImmediately = new ArrayList<String>();

          final List<ItemPath> pathsToProcess = new ArrayList<ItemPath>(paths);

          for (PendingChange pendingChange : pendingChanges) {
            final ChangeTypeMask changeType = new ChangeTypeMask(pendingChange.getChg());
            if (changeType.containsAny(ChangeType_type0.Add, ChangeType_type0.Undelete)) {
              // TODO: assert that only Edit, Encoding can be here
              revertImmediately.add(pendingChange.getItem());
              final FilePath localPath =
                VersionControlPath.getFilePath(pendingChange.getLocal(), pendingChange.getType() == ItemType.Folder);
              excludeFromFurtherProcessing(localPath);
              final ItemPath itemPath = new ItemPath(localPath, pendingChange.getItem());
              pathsToProcess.remove(itemPath);
            }
          }

          UndoPendingChanges.UndoPendingChangesResult undoResult =
            UndoPendingChanges.execute(myProject, workspace, revertImmediately, true, ApplyProgress.EMPTY, false);

          if (!undoResult.errors.isEmpty()) {
            // TODO list -> collection
            AbstractVcsHelper.getInstance(myProject).showErrors(new ArrayList<VcsException>(undoResult.errors), TFSVcs.TFS_NAME);
          }

          StatusProvider.visitByStatus(workspace, pathsToProcess, false, null, new StatusVisitor() {
            public void scheduledForAddition(final @NotNull FilePath localPath,
                                             final boolean localItemExists,
                                             final @NotNull ServerStatus serverStatus) {
              TFSVcs.error("Cannot revert an item scheduled for addition: " + localPath.getPresentableUrl());
            }

            public void unversioned(final @NotNull FilePath localPath,
                                    final boolean localItemExists,
                                    final @NotNull ServerStatus serverStatus) {
              excludeFromFurtherProcessing(localPath);
            }

            public void deleted(final @NotNull FilePath localPath,
                                final boolean localItemExists,
                                final @NotNull ServerStatus serverStatus) {
              excludeFromFurtherProcessing(localPath);
            }

            public void scheduledForDeletion(final @NotNull FilePath localPath,
                                             final boolean localItemExists,
                                             final @NotNull ServerStatus serverStatus) {
              excludeFromFurtherProcessing(localPath);
            }

            public void checkedOutForEdit(final @NotNull FilePath localPath,
                                          final boolean localItemExists,
                                          final @NotNull ServerStatus serverStatus) {
              // keep for further processing
            }

            public void outOfDate(final @NotNull FilePath localPath,
                                  final boolean localItemExists,
                                  final @NotNull ServerStatus serverStatus) throws TfsException {
              // keep for further processing
            }

            public void upToDate(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
              throws TfsException {
              // keep for further processing
            }

            public void renamed(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
              throws TfsException {
              // keep for further processing
            }

            public void renamedCheckedOut(final @NotNull FilePath localPath,
                                          final boolean localItemExists,
                                          final @NotNull ServerStatus serverStatus) throws TfsException {
              // keep for further processing
            }

            public void undeleted(final @NotNull FilePath localPath,
                                  final boolean localItemExists,
                                  final @NotNull ServerStatus serverStatus) throws TfsException {
              TFSVcs.error("Cannot revert undeleted: " + localPath.getPresentableUrl());
            }
          }, myProject);
        }
      });
    }
    catch (TfsException e) {
      AbstractVcsHelper.getInstance(myProject).showError(new VcsException(e), TFSVcs.TFS_NAME);
    }

    if (!myDeletedFiles.isEmpty() || !myDeletedWithoutConfirmFiles.isEmpty()) {
      super.executeDelete();
    }
  }

  protected void performDeletion(final List<FilePath> filesToDelete) {
    final List<VcsException> errors = new ArrayList<VcsException>();
    try {
      WorkstationHelper.processByWorkspaces(filesToDelete, false, myProject, new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) {
          Collection<VcsException> scheduleErrors = ScheduleForDeletion.execute(myProject, workspace, paths);
          errors.addAll(scheduleErrors);
        }
      });
    }
    catch (TfsException e) {
      errors.add(new VcsException(e));
    }
    if (!errors.isEmpty()) {
      AbstractVcsHelper.getInstance(myProject).showErrors(errors, TFSVcs.TFS_NAME);
    }
  }


  private void excludeFromFurtherProcessing(final FilePath localPath) {
    if (!myDeletedFiles.remove(localPath)) {
      myDeletedWithoutConfirmFiles.remove(localPath);
    }
  }

  protected void performAdding(final Collection<VirtualFile> addedFiles, final Map<VirtualFile, VirtualFile> copyFromMap) {
    final List<VcsException> errors = new ArrayList<VcsException>();
    try {
      final List<FilePath> orphans =
        WorkstationHelper.processByWorkspaces(TfsFileUtil.getFilePaths(addedFiles), false, myProject,
                                              new WorkstationHelper.VoidProcessDelegate() {
          public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) {
            Collection<VcsException> schedulingErrors = ScheduleForAddition.execute(myProject, workspace, paths);
            errors.addAll(schedulingErrors);
          }
        });

      if (!orphans.isEmpty()) {
        StringBuilder s = new StringBuilder();
        for (FilePath orpan : orphans) {
          if (s.length() > 0) {
            s.append("\n");
          }
          s.append(orpan.getPresentableUrl());
        }
        errors.add(new VcsException("Team Foundation Server mappings not found for: " + s.toString()));
      }
    }
    catch (TfsException e) {
      errors.add(new VcsException(e));
    }
    if (!errors.isEmpty()) {
      AbstractVcsHelper.getInstance(myProject).showErrors(errors, TFSVcs.TFS_NAME);
    }
  }

  protected String getDeleteTitle() {
    return "Do you want to schedule these items for deletion from TFS?";
  }

  protected String getSingleFileDeleteTitle() {
    return null;
  }

  protected String getSingleFileDeletePromptTemplate() {
    return null;
  }

  protected void performMoveRename(final List<MovedFileInfo> movedFiles) {
    final Map<FilePath, FilePath> movedPaths = new HashMap<FilePath, FilePath>(movedFiles.size());
    for (MovedFileInfo movedFileInfo : movedFiles) {
      movedPaths.put(VcsUtil.getFilePath(movedFileInfo.myOldPath), VcsUtil.getFilePath(movedFileInfo.myNewPath));
    }
    final List<VcsException> errors = new ArrayList<VcsException>();
    final Map<FilePath, FilePath> scheduleMove = new HashMap<FilePath, FilePath>();
    try {
      WorkstationHelper.processByWorkspaces(movedPaths.keySet(), false, myProject, new WorkstationHelper.VoidProcessDelegate() {

        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          // TODO simplify this
          StatusProvider.visitByStatus(workspace, paths, false, null, new StatusVisitor() {

            public void unversioned(final @NotNull FilePath localPath,
                                    final boolean localItemExists,
                                    final @NotNull ServerStatus serverStatus) throws TfsException {
              // ignore
            }

            public void checkedOutForEdit(final @NotNull FilePath localPath,
                                          final boolean localItemExists,
                                          final @NotNull ServerStatus serverStatus) {
              scheduleMove.put(localPath, movedPaths.get(localPath));
            }

            public void scheduledForAddition(final @NotNull FilePath localPath,
                                             final boolean localItemExists,
                                             final @NotNull ServerStatus serverStatus) {
              scheduleMove.put(localPath, movedPaths.get(localPath));
            }

            public void scheduledForDeletion(final @NotNull FilePath localPath,
                                             final boolean localItemExists,
                                             final @NotNull ServerStatus serverStatus) {
              TFSVcs.error("Cannot rename a file that does not exist on local machine: " + localPath.getPresentableUrl());
            }

            public void outOfDate(final @NotNull FilePath localPath,
                                  final boolean localItemExists,
                                  final @NotNull ServerStatus serverStatus) throws TfsException {
              scheduleMove.put(localPath, movedPaths.get(localPath));
            }

            public void deleted(final @NotNull FilePath localPath,
                                final boolean localItemExists,
                                final @NotNull ServerStatus serverStatus) {
              // ignore
            }

            public void upToDate(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
              throws TfsException {
              scheduleMove.put(localPath, movedPaths.get(localPath));
            }

            public void renamed(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
              throws TfsException {
              scheduleMove.put(localPath, movedPaths.get(localPath));
            }

            public void renamedCheckedOut(final @NotNull FilePath localPath,
                                          final boolean localItemExists,
                                          final @NotNull ServerStatus serverStatus) throws TfsException {
              scheduleMove.put(localPath, movedPaths.get(localPath));
            }

            public void undeleted(final @NotNull FilePath localPath,
                                  final boolean localItemExists,
                                  final @NotNull ServerStatus serverStatus) throws TfsException {
              scheduleMove.put(localPath, movedPaths.get(localPath));
            }
          }, myProject);

          final ResultWithFailures<GetOperation> renameResult =
            workspace.getServer().getVCS()
              .renameAndUpdateLocalVersion(workspace.getName(), workspace.getOwnerName(), scheduleMove, myProject,
                                           TFSBundle.message("renaming"));
          errors.addAll(TfsUtil.getVcsExceptions(renameResult.getFailures()));

          Collection<FilePath> invalidate = new ArrayList<FilePath>(renameResult.getResult().size());
          for (GetOperation getOperation : renameResult.getResult()) {
            invalidate.add(VersionControlPath.getFilePath(getOperation.getTlocal(), getOperation.getType() == ItemType.Folder));
            //invalidate.add(VcsUtil.getFilePath(getOperation.getSlocal()));
          }
          TfsFileUtil.markDirtyRecursively(myProject, invalidate);
        }
      });
    }
    catch (TfsException e) {
      errors.add(new VcsException(e));
    }
    if (!errors.isEmpty()) {
      AbstractVcsHelper.getInstance(myProject).showErrors(errors, TFSVcs.TFS_NAME);
    }
  }

  protected boolean isDirectoryVersioningSupported() {
    return true;
  }
}
