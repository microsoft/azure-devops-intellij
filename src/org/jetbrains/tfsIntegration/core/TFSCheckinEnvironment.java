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
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
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
import org.jetbrains.tfsIntegration.core.tfs.operations.ScheduleForAddition;
import org.jetbrains.tfsIntegration.core.tfs.operations.ScheduleForDeletion;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
    return null;
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
    final List<VcsException> errors = new ArrayList<VcsException>();
    try {
      WorkstationHelper.processByWorkspaces(files, false, new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          try {
            // get pending changes for given items
            Collection<PendingChange> pendingChanges = workspace.getServer().getVCS()
              .queryPendingSetsByPaths(workspace.getName(), workspace.getOwnerName(), paths, RecursionType.None);

            if (pendingChanges.isEmpty()) {
              return;
            }

            Collection<String> checkIn = new ArrayList<String>();
            // upload files
            for (PendingChange pendingChange : pendingChanges) {
              if (pendingChange.getType() == ItemType.File) {
                EnumMask<ChangeType> changeType = EnumMask.fromString(ChangeType.class, pendingChange.getChg());
                if (changeType.contains(ChangeType.Edit) || changeType.contains(ChangeType.Add)) {
                  workspace.getServer().getVCS().uploadItem(workspace, pendingChange);
                }
              }
              checkIn.add(pendingChange.getItem());
            }

            ResultWithFailures<CheckinResult> result = workspace.getServer().getVCS()
              .checkIn(workspace.getName(), workspace.getOwnerName(), checkIn, preparedComment);
            errors.addAll(BeanHelper.getVcsExceptions(result.getFailures()));

            Collection<String> commitFailed = new ArrayList<String>(result.getFailures().size());
            for (Failure failure : result.getFailures()) {
              TFSVcs.assertTrue(failure.getItem() != null);
              commitFailed.add(failure.getItem());
            }

            Collection<FilePath> invalidateRoots = new ArrayList<FilePath>(pendingChanges.size());
            Collection<FilePath> invalidateFiles = new ArrayList<FilePath>();
            // set readonly status for files
            for (PendingChange pendingChange : pendingChanges) {
              TFSVcs.assertTrue(pendingChange.getItem() != null);
              if (commitFailed.contains(pendingChange.getItem())) {
                continue;
              }

              EnumMask<ChangeType> changeType = EnumMask.fromString(ChangeType.class, pendingChange.getChg());
              if (pendingChange.getType() == ItemType.File) {
                if (changeType.contains(ChangeType.Edit) || changeType.contains(ChangeType.Add) || changeType.contains(ChangeType.Rename)) {
                  VirtualFile file = VcsUtil.getVirtualFile(pendingChange.getLocal());
                  if (file != null && file.isValid()) {
                    TfsFileUtil.setReadOnlyInEventDispathThread(file, true);
                  }
                }
              }

              // TODO don't add recursive invalidate
              // TODO if Rename, invalidate old and new items?
              final FilePath path = VcsUtil.getFilePath(pendingChange.getLocal());
              invalidateRoots.add(path);
              if (changeType.contains(ChangeType.Add)) {
                // [IDEADEV-27087] invalidate parent folders since they can be implicitly checked in with child checkin
                final VirtualFile vcsRoot = ProjectLevelVcsManager.getInstance(myProject).getVcsRootFor(path);
                if (vcsRoot != null) {
                  final FilePath vcsRootPath = TfsFileUtil.getFilePath(vcsRoot);
                  for (FilePath parent = path.getParentPath();
                       parent != null && parent.isUnder(vcsRootPath, false);
                       parent = parent.getParentPath()) {
                    invalidateFiles.add(parent);
                  }
                }
              }
            }
            TfsFileUtil.invalidate(myProject, invalidateRoots, invalidateFiles);
          }
          catch (IOException e) {
            //noinspection ThrowableInstanceNeverThrown
            errors.add(new VcsException(e));
          }
        }
      });
    }
    catch (TfsException e) {
      //noinspection ThrowableInstanceNeverThrown
      errors.add(new VcsException(e));
    }
    return errors;
  }

  @Nullable
  public List<VcsException> scheduleMissingFileForDeletion(final List<FilePath> files) {
    final List<VcsException> errors = new ArrayList<VcsException>();
    try {
      WorkstationHelper.processByWorkspaces(files, false, new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) {
          Collection<VcsException> schedulingErrors = ScheduleForDeletion.execute(myProject, workspace, paths);
          errors.addAll(schedulingErrors);
        }
      });
    }
    catch (TfsException e) {
      //noinspection ThrowableInstanceNeverThrown
      errors.add(new VcsException(e));
    }
    return errors;
  }

  @Nullable
  public List<VcsException> scheduleUnversionedFilesForAddition(final List<VirtualFile> files) {
    // TODO: schedule parent folders?
    final List<VcsException> exceptions = new ArrayList<VcsException>();
    try {
      WorkstationHelper.processByWorkspaces(TfsFileUtil.getFilePaths(files), false, new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) {
          Collection<VcsException> schedulingErrors = ScheduleForAddition.execute(myProject, workspace, paths);
          exceptions.addAll(schedulingErrors);
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
