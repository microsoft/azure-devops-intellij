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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.core.tfs.operations.ScheduleForAddition;
import org.jetbrains.tfsIntegration.core.tfs.operations.ScheduleForDeletion;
import org.jetbrains.tfsIntegration.core.tfs.operations.UndoPendingChanges;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Failure;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.GetOperation;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.SeverityType;

import java.util.*;

public class TFSFileListener extends TFSFileListenerBase {

  public TFSFileListener(Project project, TFSVcs vcs) {
    super(project, vcs);
  }

  protected String getAddTitle() {
    return "Do you want to schedule the following items for addition to TFS?";
  }

  protected String getSingleFileAddTitle() {
    return null;
  }

  protected String getSingleFileAddPromptTemplate() {
    return null;
  }

  protected void executeAdd() {
    try {
      WorkstationHelper.processByWorkspaces(TfsFileUtil.getFilePaths(myAddedFiles), new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {

          StatusProvider.visitByStatus(workspace, paths, null, new StatusVisitor() {
            public void unversioned(final @NotNull ItemPath path, final @Nullable ExtendedItem extendedItem, final boolean localItemExists)
              throws TfsException {
              // ignore
            }

            public void checkedOutForEdit(final @NotNull ItemPath path,
                                          final @NotNull ExtendedItem extendedItem,
                                          final boolean localItemExists) {
              // TODO: add local conflict
            }

            public void scheduledForAddition(@NotNull final ItemPath path,
                                             @NotNull final ExtendedItem extendedItem,
                                             final boolean localItemExists) {
              myAddedFiles.remove(path.getLocalPath().getVirtualFile());
            }

            public void scheduledForDeletion(final @NotNull ItemPath path,
                                             final @NotNull ExtendedItem extendedItem,
                                             final boolean localItemExists) {
              // TODO: add local conflict
            }

            public void outOfDate(final @NotNull ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists)
              throws TfsException {
              // TODO: add local conflict
            }

            public void deleted(final @NotNull ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists) {
              // ignore
            }

            public void upToDate(final @NotNull ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists)
              throws TfsException {
              // TODO: add local conflict
            }

            public void renamed(final @NotNull ItemPath path, @NotNull final ExtendedItem extendedItem, final boolean localItemExists)
              throws TfsException {
              // TODO: add local conflict
            }

            public void renamedCheckedOut(final @NotNull ItemPath path, @NotNull final ExtendedItem extendedItem, final boolean localItemExists)
              throws TfsException {
              // TODO: add local conflict
            }
            
          });
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
    List<FilePath> deletedFiles = new ArrayList<FilePath>(myDeletedFiles);
    deletedFiles.addAll(myDeletedWithoutConfirmFiles);

    try {
      WorkstationHelper.processByWorkspaces(deletedFiles, new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          final List<ItemPath> revertScheduledForAdditionImmediately = new ArrayList<ItemPath>();

          StatusProvider.visitByStatus(workspace, paths, null, new StatusVisitor() {
            public void unversioned(@NotNull final ItemPath path,
                                    final @Nullable ExtendedItem extendedItem,
                                    final boolean localItemExists) {
              excludeFromFurtherProcessing(path);
            }

            public void checkedOutForEdit(@NotNull final ItemPath path,
                                          @NotNull final ExtendedItem extendedItem,
                                          final boolean localItemExists) {
            }

            public void scheduledForAddition(@NotNull final ItemPath path,
                                             @NotNull final ExtendedItem extendedItem,
                                             final boolean localItemExists) {
              revertScheduledForAdditionImmediately.add(path);
              excludeFromFurtherProcessing(path);
            }

            public void scheduledForDeletion(@NotNull final ItemPath path,
                                             @NotNull final ExtendedItem extendedItem,
                                             final boolean localItemExists) {
              excludeFromFurtherProcessing(path);
            }

            public void outOfDate(final @NotNull ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists)
              throws TfsException {
              // keep for further processing
            }

            public void deleted(@NotNull final ItemPath path, @NotNull final ExtendedItem extendedItem, final boolean localItemExists) {
              excludeFromFurtherProcessing(path);
            }

            public void upToDate(final @NotNull ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists)
              throws TfsException {
              // keep for further processing
            }

            public void renamed(@NotNull final ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists)
              throws TfsException {
              // keep for further processing
            }

            public void renamedCheckedOut(@NotNull final ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists)
              throws TfsException {
              // keep for further processing
            }
          });

          if (!revertScheduledForAdditionImmediately.isEmpty()) {
            UndoPendingChanges.UndoPendingChangesResult undoResult = UndoPendingChanges.execute(workspace, revertScheduledForAdditionImmediately, false);
            if (!undoResult.errors.isEmpty()) {
              // TODO list -> collection
              AbstractVcsHelper.getInstance(myProject).showErrors(new ArrayList<VcsException>(undoResult.errors), TFSVcs.TFS_NAME);
            }
          }
        }

        private void excludeFromFurtherProcessing(final ItemPath path) {
          if (!myDeletedFiles.remove(path.getLocalPath())) {
            myDeletedWithoutConfirmFiles.remove(path.getLocalPath());
          }
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

  protected void performAdding(final Collection<VirtualFile> addedFiles, final Map<VirtualFile, VirtualFile> copyFromMap) {
    final List<VcsException> errors = new ArrayList<VcsException>();
    try {
      WorkstationHelper.processByWorkspaces(TfsFileUtil.getFilePaths(addedFiles), new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) {
          Collection<VcsException> schedulingErrors = ScheduleForAddition.execute(myProject, workspace, paths);
          errors.addAll(schedulingErrors);
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

  protected void performDeletion(final List<FilePath> filesToDelete) {
    final List<VcsException> errors = new ArrayList<VcsException>();
    try {
      WorkstationHelper.processByWorkspaces(filesToDelete, new WorkstationHelper.VoidProcessDelegate() {
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


  protected String getDeleteTitle() {
    return "Do you want to schedule the following items for deletion from TFS?";
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
    final Map<ItemPath, FilePath> scheduleMove = new HashMap<ItemPath, FilePath>();
    final List<ItemPath> checkoutForEditFirst = new ArrayList<ItemPath>();
    try {
      WorkstationHelper.processByWorkspaces(movedPaths.keySet(), new WorkstationHelper.VoidProcessDelegate() {

        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          StatusProvider.visitByStatus(workspace, paths, null, new StatusVisitor() {

            public void unversioned(final @NotNull ItemPath path, final @Nullable ExtendedItem extendedItem, final boolean localItemExists)
              throws TfsException {
              // ignore
            }

            public void checkedOutForEdit(final @NotNull ItemPath path,
                                          final @NotNull ExtendedItem extendedItem,
                                          final boolean localItemExists) {
              scheduleMove.put(path, movedPaths.get(path.getLocalPath()));
            }

            public void scheduledForAddition(final @NotNull ItemPath path,
                                             final @NotNull ExtendedItem extendedItem,
                                             final boolean localItemExists) {
              scheduleMove.put(path, movedPaths.get(path.getLocalPath()));
            }

            public void scheduledForDeletion(final @NotNull ItemPath path,
                                             final @NotNull ExtendedItem extendedItem,
                                             final boolean localItemExists) {
              TFSVcs.error("Rename of locally unexisting file: " + path.getLocalPath());
            }

            public void outOfDate(final @NotNull ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists)
              throws TfsException {
              checkoutForEditFirst.add(path);
              scheduleMove.put(path, movedPaths.get(path.getLocalPath()));
            }

            public void deleted(final @NotNull ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists) {
              // ignore
            }

            public void upToDate(final @NotNull ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists)
              throws TfsException {
              scheduleMove.put(path, movedPaths.get(path.getLocalPath()));
            }

            public void renamed(final @NotNull ItemPath path, @NotNull final ExtendedItem extendedItem, final boolean localItemExists)
              throws TfsException {
              scheduleMove.put(path, movedPaths.get(path.getLocalPath()));
            }

            public void renamedCheckedOut(final @NotNull ItemPath path, @NotNull final ExtendedItem extendedItem, final boolean localItemExists)
              throws TfsException {
              scheduleMove.put(path, movedPaths.get(path.getLocalPath()));
            }
          });

          final ResultWithFailures<GetOperation> checkoutResult =
            workspace.getServer().getVCS().checkoutForEdit(workspace.getName(), workspace.getOwnerName(), checkoutForEditFirst);
          errors.addAll(BeanHelper.getVcsExceptions(checkoutResult.getFailures()));


          for (Failure failure : checkoutResult.getFailures()) {
            if (failure.getSev() != SeverityType.Warning) {
              FilePath path = VcsUtil.getFilePath(failure.getLocal());
              scheduleMove.remove(new ItemPath(path, null));
            }
          }

          final ResultWithFailures<GetOperation> renameResult =
            workspace.getServer().getVCS().rename(workspace.getName(), workspace.getOwnerName(), scheduleMove);
          errors.addAll(BeanHelper.getVcsExceptions(renameResult.getFailures()));

          workspace.getServer().getVCS().updateLocalVersions(workspace.getName(), workspace.getOwnerName(), renameResult.getResult());
          Collection<FilePath> invalidate = new ArrayList<FilePath>(renameResult.getResult().size());
          for (GetOperation getOperation : renameResult.getResult()) {
            invalidate.add(VcsUtil.getFilePath(getOperation.getTlocal()));
            invalidate.add(VcsUtil.getFilePath(getOperation.getSlocal()));
          }
          TfsFileUtil.invalidateFiles(myProject, invalidate);
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
