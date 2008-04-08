package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.checkin.CheckinEnvironment;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.io.ReadOnlyAttributeUtil;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Failure;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.PendingChange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TFSCheckinEnvironment implements CheckinEnvironment {

  @Nullable
  public RefreshableOnComponent createAdditionalOptionsPanel(final CheckinProjectPanel panel) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Nullable
  public String getDefaultMessageFor(final FilePath[] filesToCheckin) {
    // TODO: correct default checkin message
    String message = "Check in:\n";
    for (FilePath filePath : filesToCheckin) {
      message += (filePath + "\n");
    }
    return message;
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
            // 1. get pending changes for given items
            Map<ItemPath, PendingChange> pendingChanges =
              workspace.getServer().getVCS().queryPendingSets(workspace.getName(), workspace.getOwnerName(), paths);
            // 2. upload files
            for (ItemPath path : pendingChanges.keySet()) {
              PendingChange pendingChange = pendingChanges.get(path);
              workspace.getServer().getVCS().uploadItem(workspace, pendingChange);
            }
            // 3. call check in
            if (!pendingChanges.isEmpty()) {
              workspace.getServer().getVCS()
                .checkIn(workspace.getName(), workspace.getOwnerName(), new ArrayList<ItemPath>(pendingChanges.keySet()), preparedComment);
              // TODO: check that CheckIn was successfull
            }
            // 4. set readonly status for files
            for (ItemPath path : pendingChanges.keySet()) {
              VirtualFile file = VcsUtil.getVirtualFile(path.getLocalPath().getPath());
              if (file != null && file.isValid() && !file.isDirectory()) {
                ReadOnlyAttributeUtil.setReadOnlyAttribute(file, true);
              }
            }
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
    return exceptions;
  }

  @Nullable
  public List<VcsException> scheduleMissingFileForDeletion(final List<FilePath> files) {
    final List<VcsException> exceptions = new ArrayList<VcsException>();
    try {
      WorkstationHelper.processByWorkspaces(files, new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          try {
            List<Failure> failures = OperationHelper.scheduleForDeletion(workspace, paths);
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
    return exceptions;
  }

  @Nullable
  public List<VcsException> scheduleUnversionedFilesForAddition(final List<VirtualFile> files) {
    final List<VcsException> exceptions = new ArrayList<VcsException>();
    try {
      WorkstationHelper.processByWorkspaces(TfsFileUtil.getFilePaths(files), new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          try {
            List<Failure> failures = OperationHelper.scheduleForAddition(workspace, paths);
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
    return exceptions;
  }

}
