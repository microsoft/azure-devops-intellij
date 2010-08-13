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
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.openapi.vcs.update.FileGroup;
import com.intellij.openapi.vcs.update.UpdatedFiles;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsRunnable;
import com.intellij.vcsUtil.VcsUtil;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSBundle;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.core.revision.TFSContentRevision;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.core.tfs.operations.ApplyGetOperations;
import org.jetbrains.tfsIntegration.core.tfs.operations.ApplyProgress;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.ui.ContentTriplet;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

public class ResolveConflictHelper {
  private final @NotNull Project myProject;
  private final @Nullable UpdatedFiles myUpdatedFiles;
  private final Map<Conflict, WorkspaceInfo> myConflict2Workspace = new HashMap<Conflict, WorkspaceInfo>();

  public ResolveConflictHelper(final Project project,
                               Map<WorkspaceInfo, Collection<Conflict>> workspace2Conflicts,
                               final UpdatedFiles updatedFiles) {
    myProject = project;

    for (Map.Entry<WorkspaceInfo, Collection<Conflict>> e : workspace2Conflicts.entrySet()) {
      for (Conflict conflict : e.getValue()) {
        myConflict2Workspace.put(conflict, e.getKey());
      }
    }
    myUpdatedFiles = updatedFiles;
  }

  public void acceptMerge(final @NotNull Conflict conflict) throws TfsException, VcsException {
    TFSVcs.assertTrue(canMerge(conflict));

    final WorkspaceInfo workspace = myConflict2Workspace.get(conflict);

    @SuppressWarnings({"ConstantConditions"}) @NotNull final FilePath localPath = VersionControlPath
      .getFilePath(conflict.getSrclitem() != null ? conflict.getSrclitem() : conflict.getTgtlitem(),
                   conflict.getYtype() == ItemType.Folder);

    final ContentTriplet contentTriplet = new ContentTriplet();
    VcsRunnable runnable = new VcsRunnable() {
      public void run() throws VcsException {
        // virtual file can be out of the current project so force its discovery
        TfsFileUtil.refreshAndFindFile(localPath);
        try {
          if (conflict.getYtype() == ItemType.File) {
            final String current;
            final String last;
            if (conflict.getCtype() == ConflictType.Merge) {
              current = TFSContentRevision.create(myProject, workspace, conflict.getTver(), conflict.getTitemid()).getContent();
              last = TFSContentRevision.create(myProject, workspace, conflict.getYver(), conflict.getYitemid()).getContent();
            }
            else {
              current = CurrentContentRevision.create(localPath).getContent();
              last = TFSContentRevision.create(myProject, workspace, conflict.getTver(), conflict.getTitemid()).getContent();
            }
            final String original = TFSContentRevision.create(myProject, workspace, conflict.getBver(), conflict.getBitemid()).getContent();
            contentTriplet.baseContent = original != null ? original : "";
            contentTriplet.localContent = current != null ? current : "";
            contentTriplet.serverContent = last != null ? last : "";
          }
        }
        catch (TfsException e) {
          throw new VcsException("Cannot get content for item " + localPath.getPresentableUrl());
        }
      }

    };

    if (isContentConflict(conflict)) {
      // we will need content only if it conflicts
      VcsUtil.runVcsProcessWithProgress(runnable, "Preparing merge data...", false, myProject);
    }

    // merge names
    final String localName;
    if (isNameConflict(conflict)) {
      // TODO proper type?
      final String mergedServerPath = ConflictsEnvironment.getNameMerger().mergeName(workspace, conflict);
      if (mergedServerPath == null) {
        // user cancelled
        return;
      }
      //noinspection ConstantConditions
      @NotNull FilePath mergedLocalPath = workspace.findLocalPathByServerPath(mergedServerPath, conflict.getYtype() == ItemType.Folder);
      localName = mergedLocalPath.getPath();
    }
    else {
      localName = VersionControlPath.localPathFromTfsRepresentation(conflict.getTgtlitem());
    }

    // merge content
    if (isContentConflict(conflict)) {
      TFSVcs.assertTrue(conflict.getYtype() == ItemType.File);
      localPath.refresh();
      final VirtualFile vFile = localPath.getVirtualFile();
      if (vFile != null) {
        try {
          TfsFileUtil.setReadOnly(vFile, false);
          ConflictsEnvironment.getContentMerger().mergeContent(conflict, contentTriplet, myProject, vFile, localName);
        }
        catch (IOException e) {
          throw new VcsException(e);
        }
      }
      else {
        String errorMessage = MessageFormat.format("File ''{0}'' is missing", localPath.getPresentableUrl());
        throw new VcsException(errorMessage);
      }
    }
    conflictResolved(conflict, Resolution.AcceptMerge, localName);
  }

  public void acceptYours(final @NotNull Conflict conflict) throws TfsException, VcsException {
    conflictResolved(conflict, Resolution.AcceptYours, null);
    // no actions will be executed so fill UpdatedFiles explicitly
    if (myUpdatedFiles != null) {
      String localPath =
        VersionControlPath.localPathFromTfsRepresentation(conflict.getSrclitem() != null ? conflict.getSrclitem() : conflict.getTgtlitem());
      myUpdatedFiles.getGroupById(FileGroup.SKIPPED_ID).add(localPath, TFSVcs.getKey(), null);
    }
  }

  public void acceptTheirs(final @NotNull Conflict conflict) throws TfsException, IOException, VcsException {
    conflictResolved(conflict, Resolution.AcceptTheirs, null);
  }

  public void skip(final @NotNull Conflict conflict) {
    if (myUpdatedFiles != null) {
      String localPath =
        VersionControlPath.localPathFromTfsRepresentation(conflict.getSrclitem() != null ? conflict.getSrclitem() : conflict.getTgtlitem());
      myUpdatedFiles.getGroupById(FileGroup.SKIPPED_ID).add(localPath, TFSVcs.getKey(), null);
    }
  }

  public Collection<Conflict> getConflicts() {
    return Collections.unmodifiableCollection(myConflict2Workspace.keySet());
  }

  public static boolean canMerge(final @NotNull Conflict conflict) {
    if (conflict.getSrclitem() == null) {
      return false;
    }

    final ChangeTypeMask yourChange = new ChangeTypeMask(conflict.getYchg());
    final ChangeTypeMask yourLocalChange = new ChangeTypeMask(conflict.getYlchg());
    final ChangeTypeMask baseChange = new ChangeTypeMask(conflict.getBchg());

    boolean isNamespaceConflict =
      ((conflict.getCtype().equals(ConflictType.Get)) || (conflict.getCtype().equals(ConflictType.Checkin))) && conflict.getIsnamecflict();
    if (!isNamespaceConflict) {
      boolean yourRenamedOrModified = yourChange.containsAny(ChangeType_type0.Rename, ChangeType_type0.Edit);
      boolean baseRenamedOrModified = baseChange.containsAny(ChangeType_type0.Rename, ChangeType_type0.Edit);
      if (yourRenamedOrModified && baseRenamedOrModified) {
        return true;
      }
    }
    if ((conflict.getYtype() != ItemType.Folder) && !isNamespaceConflict) {
      if (conflict.getCtype().equals(ConflictType.Merge) && baseChange.contains(ChangeType_type0.Edit)) {
        if (yourLocalChange.contains(ChangeType_type0.Edit)) {
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

  private void conflictResolved(final Conflict conflict, final Resolution resolution, final @Nullable String newLocalPath)
    throws TfsException, VcsException {
    WorkspaceInfo workspace = myConflict2Workspace.get(conflict);

    VersionControlServer.ResolveConflictParams resolveConflictParams =
      new VersionControlServer.ResolveConflictParams(conflict.getCid(), resolution, LockLevel.Unchanged, -2,
                                                     VersionControlPath.toTfsRepresentation(newLocalPath));

    ResolveResponse response =
      workspace.getServer().getVCS().resolveConflict(workspace.getName(), workspace.getOwnerName(), resolveConflictParams, myProject,
                                                     TFSBundle.message("reporting.conflict.resolved"));

    final UpdatedFiles updatedFiles = resolution != Resolution.AcceptMerge ? myUpdatedFiles : null;

    if (response.getResolveResult().getGetOperation() != null) {
      ApplyGetOperations.DownloadMode downloadMode =
        resolution == Resolution.AcceptTheirs ? ApplyGetOperations.DownloadMode.FORCE : ApplyGetOperations.DownloadMode.MERGE;

      final Collection<VcsException> applyErrors = ApplyGetOperations
        .execute(myProject, workspace, Arrays.asList(response.getResolveResult().getGetOperation()), ApplyProgress.EMPTY, updatedFiles,
                 downloadMode);
      if (!applyErrors.isEmpty()) {
        throw TfsUtil.collectExceptions(applyErrors);
      }
    }

    if (response.getUndoOperations().getGetOperation() != null) {
      final Collection<VcsException> applyErrors = ApplyGetOperations
        .execute(myProject, workspace, Arrays.asList(response.getUndoOperations().getGetOperation()), ApplyProgress.EMPTY, updatedFiles,
                 ApplyGetOperations.DownloadMode.FORCE);
      if (!applyErrors.isEmpty()) {
        throw TfsUtil.collectExceptions(applyErrors);
      }
    }

    if (resolution == Resolution.AcceptMerge) {
      if (myUpdatedFiles != null) {
        myUpdatedFiles.getGroupById(FileGroup.MERGED_ID).add(newLocalPath, TFSVcs.getKey(), null);
      }
    }
    myConflict2Workspace.remove(conflict);
  }

  private static boolean isNameConflict(final @NotNull Conflict conflict) {
    final ChangeTypeMask yourChange = new ChangeTypeMask(conflict.getYchg());
    final ChangeTypeMask baseChange = new ChangeTypeMask(conflict.getBchg());
    return yourChange.contains(ChangeType_type0.Rename) || baseChange.contains(ChangeType_type0.Rename);
  }

  private static boolean isContentConflict(final @NotNull Conflict conflict) {
    final ChangeTypeMask yourChange = new ChangeTypeMask(conflict.getYchg());
    final ChangeTypeMask baseChange = new ChangeTypeMask(conflict.getBchg());
    return yourChange.contains(ChangeType_type0.Edit) || baseChange.contains(ChangeType_type0.Edit);
  }

  public static Collection<Conflict> getUnresolvedConflicts(Collection<Conflict> conflicts) {
    Collection<Conflict> result = new ArrayList<Conflict>();
    for (Conflict c : conflicts) {
      if (!c.getIsresolved()) {
        TFSVcs.assertTrue(c.getCid() != 0);
        result.add(c);
      }
    }
    return result;
  }

}
