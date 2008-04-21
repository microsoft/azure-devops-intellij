package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Failure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class TFSFileListener extends TFSFileListenerBase {

  private List<FilePath> myRevertPendingChanges = new ArrayList<FilePath>();

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

            public void renamed(final @NotNull ItemPath path, final ExtendedItem extendedItem, final boolean localItemExists)
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
              myRevertPendingChanges.add(path.getLocalPath());
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
              // ignore
            }

            public void deleted(@NotNull final ItemPath path, @NotNull final ExtendedItem extendedItem, final boolean localItemExists) {
              excludeFromFurtherProcessing(path);
            }

            public void upToDate(final @NotNull ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists)
              throws TfsException {
              // ingore
            }

            public void renamed(@NotNull final ItemPath path, final ExtendedItem extendedItem, final boolean localItemExists)
              throws TfsException {
              myRevertPendingChanges.add(path.getLocalPath());
            }
          });

          if (!revertScheduledForAdditionImmediately.isEmpty()) {
            List<Failure> failures = OperationHelper.undoPendingChanges(workspace, revertScheduledForAdditionImmediately, false);
            List<VcsException> exceptions = BeanHelper.getVcsExceptions("Failed to undo pending changes", failures);
            if (!exceptions.isEmpty()) {
              AbstractVcsHelper.getInstance(myProject).showErrors(exceptions, TFSVcs.TFS_NAME);
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
    final List<VcsException> exceptions = new ArrayList<VcsException>();
    try {
      WorkstationHelper.processByWorkspaces(TfsFileUtil.getFilePaths(addedFiles), new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          try {
            Collection<Failure> failures = OperationHelper.scheduleForAddition(myProject, workspace, paths);
            exceptions.addAll(BeanHelper.getVcsExceptions("Failed to schedule for addition", failures));
          }
          catch (IOException e) {
            exceptions.add(new VcsException(e));
          }
        }
      });
    }
    catch (TfsException e) {
      exceptions.add(new VcsException(e));
    }
    if (!exceptions.isEmpty()) {
      AbstractVcsHelper.getInstance(myProject).showErrors(exceptions, TFSVcs.TFS_NAME);
    }
  }

  protected void performDeletion(final List<FilePath> filesToDelete) {
    myRevertPendingChanges.retainAll(filesToDelete);

    final List<VcsException> exceptions = new ArrayList<VcsException>();
    if (!myRevertPendingChanges.isEmpty()) {
      try {
        WorkstationHelper.processByWorkspaces(filesToDelete, new WorkstationHelper.VoidProcessDelegate() {
          public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
            List<Failure> failures = OperationHelper.undoPendingChanges(workspace, paths, false);
            exceptions.addAll(BeanHelper.getVcsExceptions("Failed to undo pending changes", failures));
          }
        });
      }
      catch (TfsException e) {
        exceptions.add(new VcsException(e));
      }
      myRevertPendingChanges.clear();
    }

    try {
      WorkstationHelper.processByWorkspaces(filesToDelete, new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          try {
            Collection<Failure> failures = OperationHelper.scheduleForDeletion(myProject, workspace, paths, false);
            exceptions.addAll(BeanHelper.getVcsExceptions("Failed to schedule for deletion", failures));
          }
          catch (IOException e) {
            exceptions.add(new VcsException(e));
          }
        }
      });
    }
    catch (TfsException e) {
      exceptions.add(new VcsException(e));
    }
    if (!exceptions.isEmpty()) {
      AbstractVcsHelper.getInstance(myProject).showErrors(exceptions, TFSVcs.TFS_NAME);
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
    //To change body of implemented methods use File | Settings | File Templates.
  }

  protected boolean isDirectoryVersioningSupported() {
    return true;
  }
}
