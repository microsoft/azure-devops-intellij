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

package org.jetbrains.tfsIntegration.core.tfs.conflicts;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.openapi.vcs.update.FileGroup;
import com.intellij.openapi.vcs.update.UpdatedFiles;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsRunnable;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSUpdateEnvironment;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.core.revision.TFSContentRevision;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.core.tfs.operations.ApplyGetOperations;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.*;
import org.jetbrains.tfsIntegration.ui.ConflictData;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

// TODO use VersionControlPath.toTfsRepresentation() instead of FileUtil.toSystemDependentName() to assign conflict data paths

public class ResolveConflictHelper {
  private final @NotNull Project myProject;
  private final @NotNull WorkspaceInfo myWorkspace;
  private final @Nullable UpdatedFiles myUpdatedFiles;
  private List<Conflict> myConflicts;
  private List<ItemPath> myPaths;

  public ResolveConflictHelper(final Project project,
                               final WorkspaceInfo workspace,
                               final List<ItemPath> paths,
                               final UpdatedFiles updatedFiles) {
    myProject = project;
    myWorkspace = workspace;
    myPaths = paths;
    myUpdatedFiles = updatedFiles;
  }

  public void conflictResolved(final Conflict conflict,
                               final ResolutionType nameResolutionType,
                               final ResolutionType contentResolutionType,
                               final String newLocalPath) throws TfsException {
    Resolution resolution = Resolution.AcceptMerge;
    if (contentResolutionType == ResolutionType.ACCEPT_YOURS && nameResolutionType == ResolutionType.ACCEPT_YOURS) {
      resolution = Resolution.AcceptYours;
    }
    if (contentResolutionType == ResolutionType.ACCEPT_THEIRS && nameResolutionType == ResolutionType.ACCEPT_THEIRS) {
      resolution = Resolution.AcceptTheirs;
    }
    VersionControlServer.ResolveConflictParams resolveConflictParams =
      new VersionControlServer.ResolveConflictParams(conflict.getCid(), resolution, LockLevel.Unchanged, -2, newLocalPath);

    ResolveResponse response =
      myWorkspace.getServer().getVCS().resolveConflict(myWorkspace.getName(), myWorkspace.getOwnerName(), resolveConflictParams);

    final ArrayOfGetOperation getOperations;
    final ApplyGetOperations.ProcessMode operationType;
    if (resolution == Resolution.AcceptTheirs) {
      getOperations = response.getUndoOperations();
      operationType = ApplyGetOperations.ProcessMode.UNDO;
      TFSVcs.assertTrue(response.getResolveResult().getGetOperation() == null);
    }
    else if (resolution == Resolution.AcceptYoursRenameTheirs) {
      // Currently it is not possible in our implementation...
      TFSVcs.error("AcceptYoursRenameTheirs resolution type not supported");
      getOperations = null;
      operationType = null;
    }
    else { //  resolution == Resolution.AcceptMerge || resolution == Resolution.AcceptYours
      getOperations = response.getResolveResult();
      operationType = ApplyGetOperations.ProcessMode.RESOLVE;
      TFSVcs.assertTrue(response.getUndoOperations().getGetOperation() == null);
    }
    // TODO check for null not needed?
    if (getOperations != null && getOperations.getGetOperation() != null) {
      ApplyGetOperations.DownloadMode downloadMode = resolution == Resolution
        .AcceptTheirs ? ApplyGetOperations.DownloadMode.FORCE : ApplyGetOperations.DownloadMode.ALLOW;
      ApplyGetOperations
        .execute(myProject, myWorkspace, Arrays.asList(getOperations.getGetOperation()), null, myUpdatedFiles, downloadMode, operationType);
    }
  }

  @NotNull
  private ConflictData getConflictData(final @NotNull Conflict conflict) throws VcsException {
    final ConflictData data = new ConflictData();
    VcsRunnable runnable = new VcsRunnable() {
      public void run() throws VcsException {

        try {
          // names
          FilePath sourceLocalPath = myWorkspace.findLocalPathByServerPath(conflict.getYsitem(), conflict.getYtype() == ItemType.Folder);
          data.sourceLocalName = sourceLocalPath != null ? FileUtil.toSystemDependentName(sourceLocalPath.getPath()) : null;
          FilePath targetLocalPath = myWorkspace.findLocalPathByServerPath(conflict.getTsitem(), conflict.getTtype() == ItemType.Folder);
          data.targetLocalName = targetLocalPath != null ? FileUtil.toSystemDependentName(targetLocalPath.getPath()) : null;

          // content
          if (conflict.getYtype() == ItemType.File) {
            String original = TFSContentRevision.create(myWorkspace, conflict.getBitemid(), conflict.getBver()).getContent();
            data.baseContent = original != null ? original : ""; // TODO: why null is not OK?
            String current = CurrentContentRevision.create(VcsUtil.getFilePath(conflict.getSrclitem())).getContent();
            data.localContent = current != null ? current : "";
            String last = TFSContentRevision.create(myWorkspace, conflict.getTitemid(), conflict.getTver()).getContent();
            data.serverContent = last != null ? last : "";
          }
        }
        catch (TfsException e) {
          throw new VcsException("Unable to get content for item " + data.sourceLocalName);
        }
      }
    };
    VcsUtil.runVcsProcessWithProgress(runnable, "Prepare merge data...", false, myProject);
    return data;
  }

  public String acceptMerge(final @NotNull Conflict conflict) throws TfsException, VcsException {
    TFSVcs.assertTrue(canMerge(conflict));  // TODO: inserted here for debug purpose, remove later
    final ConflictData conflictData = getConflictData(conflict);
    ResolutionType nameResolutionType = isNameConflict(conflict) ? ResolutionType.IGNORED : ResolutionType.NO_CONFLICT;
    ResolutionType contentResolutionType = isContentConflict(conflict) ? ResolutionType.IGNORED : ResolutionType.NO_CONFLICT;
    String localName = null;

    // merge names if needed
    if (EnumMask.fromString(ChangeType.class, conflict.getYchg()).contains(ChangeType.Rename)) {
      // TODO proper type?
      final String resultingName = TFSUpdateEnvironment.getNameConflictsHandler().mergeName(conflict);
      FilePath newLocalPath = myWorkspace.findLocalPathByServerPath(resultingName, conflict.getYtype() == ItemType.Folder);
      if (newLocalPath != null) {
        localName = VersionControlPath.toTfsRepresentation(newLocalPath);
        nameResolutionType = ResolutionType.MERGED;
      }
    }
    else {
      localName = conflictData.targetLocalName;
    }

    // if content conflict present show merge dialog
    if (conflict.getYtype() == ItemType.File && contentResolutionType == ResolutionType.IGNORED) {
      final VirtualFile vFile = VcsUtil.getVirtualFile(conflictData.sourceLocalName);
      if (vFile != null) {
        TFSUpdateEnvironment.getContentConflictsHandler().mergeContent(conflict, conflictData, myProject, vFile, localName);
        contentResolutionType = ResolutionType.MERGED;
      }
    }
    conflictResolved(conflict, nameResolutionType, contentResolutionType, localName);
    return localName;
  }

  public String acceptYours(final @NotNull Conflict conflict) throws TfsException {
    conflictResolved(conflict, ResolutionType.ACCEPT_YOURS, ResolutionType.ACCEPT_YOURS, null);
    if (myUpdatedFiles != null) {
      myUpdatedFiles.getGroupById(FileGroup.SKIPPED_ID).add(conflict.getSrclitem());
    }
    return conflict.getSrclitem();
  }

  public String acceptTheirs(final @NotNull Conflict conflict) throws TfsException, IOException {
    conflictResolved(conflict, ResolutionType.ACCEPT_THEIRS, ResolutionType.ACCEPT_THEIRS, null);
    return conflict.getTgtlitem();
  }

  public static boolean isNameConflict(final @NotNull Conflict conflict) {
    return EnumMask.fromString(ChangeType.class, conflict.getYchg()).contains(ChangeType.Rename);
  }

  public static boolean isContentConflict(final @NotNull Conflict conflict) {
    return EnumMask.fromString(ChangeType.class, conflict.getYchg()).contains(ChangeType.Edit);
  }

  public void updateConflicts() throws TfsException {
    myConflicts =
      myWorkspace.getServer().getVCS().queryConflicts(myWorkspace.getName(), myWorkspace.getOwnerName(), myPaths, RecursionType.Full);
  }

  public List<Conflict> getConflicts() {
    return myConflicts;
  }

  public static boolean canMerge(final @NotNull Conflict conflict) {
    boolean isNamespaceConflict =
      ((conflict.getCtype().equals(ConflictType.Get)) || (conflict.getCtype().equals(ConflictType.Checkin))) && conflict.getIsnamecflict();
    if ((conflict.getYtype() != ItemType.Folder) && !isNamespaceConflict) {
      if (EnumMask.fromString(ChangeType.class, conflict.getYchg()).contains(ChangeType.Edit) &&
          EnumMask.fromString(ChangeType.class, conflict.getBchg()).contains(ChangeType.Edit)) {
        return true;
      }
      if (EnumMask.fromString(ChangeType.class, conflict.getYchg()).contains(ChangeType.Rename) &&
          EnumMask.fromString(ChangeType.class, conflict.getBchg()).contains(ChangeType.Rename)) {
        return true;
      }
      if (conflict.getCtype().equals(ConflictType.Merge) &&
          EnumMask.fromString(ChangeType.class, conflict.getBchg()).contains(ChangeType.Edit)) {
        if (EnumMask.fromString(ChangeType.class, conflict.getYchg()).contains(ChangeType.Edit)) {
          return true;
        }
        if (conflict.getIsforced()) {
          return true;
        }
        if ((conflict.getTlmver() != conflict.getBver()) || (conflict.getYlmver() != conflict.getYver())) {
          return true;
        }
      }
    }
    return false;
  }
}
