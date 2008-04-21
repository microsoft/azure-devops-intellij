package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.checkin.CheckinEnvironment;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Failure;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ItemType;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.PendingChange;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.CheckinResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collection;

public class TFSCheckinEnvironment implements CheckinEnvironment {

  private @NotNull final Project myProject;

  public TFSCheckinEnvironment(@NotNull final Project project) {
    myProject = project;
  }

  @Nullable
  public RefreshableOnComponent createAdditionalOptionsPanel(final CheckinProjectPanel panel) {
    return null;  // TODO: implement
  }

  @Nullable
  public String getDefaultMessageFor(final FilePath[] filesToCheckin) {
    // TODO: correct default checkin message
    return "";
  }

  public String prepareCheckinMessage(final String text) {
    return text;
  }

  @Nullable
  @NonNls
  public String getHelpId() {
    return null;  // TODO: help id for check in
  }

  public String getCheckinOperationName() {
    return "Check In";
  }

  public boolean showCheckinDialogInAnyCase() {
    return false;
  }

  @Nullable
  public List<VcsException> commit(final List<Change> changes, final String preparedComment) {
    // TODO: add parent folders
    final List<FilePath> files = new ArrayList<FilePath>();
    for (Change change : changes) {
      FilePath path = null;
      ContentRevision beforeRevision = change.getBeforeRevision();
      ContentRevision afterRevision = change.getAfterRevision();
      if (afterRevision != null) {
        path = afterRevision.getFile();
      }
      else if (beforeRevision != null) {
        path = beforeRevision.getFile();
      }
      if (path != null) {
        files.add(path);
      }
    }
    final List<VcsException> exceptions = new ArrayList<VcsException>();
    try {
      WorkstationHelper.processByWorkspaces(files, new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          try {
            // get pending changes for given items
            Map<ItemPath, PendingChange> pendingChanges =
              workspace.getServer().getVCS().queryPendingSets(workspace.getName(), workspace.getOwnerName(), paths);
            // upload files
            for (Map.Entry<ItemPath, PendingChange> entry : pendingChanges.entrySet()) {
              if (entry.getValue().getType() == ItemType.File) {
                workspace.getServer().getVCS().uploadItem(workspace, entry.getValue());
              }
            }
            // call check in
            if (!pendingChanges.isEmpty()) {
              ResultWithFailures<CheckinResult> result = workspace.getServer().getVCS()
                .checkIn(workspace.getName(), workspace.getOwnerName(), new ArrayList<ItemPath>(pendingChanges.keySet()), preparedComment);
              // check that CheckIn was successfull
              if (!result.getFailures().isEmpty()) {
                exceptions.addAll(BeanHelper.getVcsExceptions("Failed to check in", result.getFailures()));
              }
              else {
                // set readonly status for files
                for (ItemPath path : pendingChanges.keySet()) {
                  VirtualFile file = VcsUtil.getVirtualFile(path.getLocalPath().getPath());
                  if (file != null && file.isValid() && !file.isDirectory()) {
                    TfsFileUtil.setReadOnlyInEventDispathThread(file, true);
                  }
                }
              }
            }
          }
          catch (IOException e) {
            //noinspection ThrowableInstanceNeverThrown
            exceptions.add(new VcsException(e));
          }
        }
      });
    }
    catch (TfsException e) {
      //noinspection ThrowableInstanceNeverThrown
      exceptions.add(new VcsException(e));
    }
    return exceptions;
  }

  @Nullable
  public List<VcsException> scheduleMissingFileForDeletion(final List<FilePath> files) {
    final List<VcsException> exceptions = new ArrayList<VcsException>();
    try {
      WorkstationHelper.processByWorkspaces(files, new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          try {
            Collection<Failure> failures = OperationHelper.scheduleForDeletion(myProject, workspace, paths, true);
            exceptions.addAll(BeanHelper.getVcsExceptions("Failed to schedule for deletion", failures));
          }
          catch (IOException e) {
            //noinspection ThrowableInstanceNeverThrown
            exceptions.add(new VcsException(e));
          }
        }
      });
    }
    catch (TfsException e) {
      //noinspection ThrowableInstanceNeverThrown
      exceptions.add(new VcsException(e));
    }
    return exceptions;
  }

  @Nullable
  public List<VcsException> scheduleUnversionedFilesForAddition(final List<VirtualFile> files) {
    // TODO: schedule parent folders?
    final List<VcsException> exceptions = new ArrayList<VcsException>();
    try {
      WorkstationHelper.processByWorkspaces(TfsFileUtil.getFilePaths(files), new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          try {
            Collection<Failure> failures = OperationHelper.scheduleForAddition(myProject, workspace, paths);
            exceptions.addAll(BeanHelper.getVcsExceptions("Failed to schedule for addition", failures));
          }
          catch (IOException e) {
            //noinspection ThrowableInstanceNeverThrown
            exceptions.add(new VcsException(e));
          }
        }
      });
    }
    catch (TfsException e) {
      //noinspection ThrowableInstanceNeverThrown
      exceptions.add(new VcsException(e));
    }
    return exceptions;
  }

}
