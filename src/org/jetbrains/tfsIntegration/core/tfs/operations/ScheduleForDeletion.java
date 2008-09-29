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

package org.jetbrains.tfsIntegration.core.tfs.operations;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.GetOperation;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.PendingChange;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.RecursionType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ScheduleForDeletion {

  public static Collection<VcsException> execute(Project project, WorkspaceInfo workspace, List<ItemPath> paths) {
    // choose roots
    // recursively undo pending changes except schedule for deletion => map: modified_name->original_name
    // schedule roots for deletion using their original names (+updateLocalVersion)

    Collection<VcsException> errors = new ArrayList<VcsException>();

    try {
      RootsCollection.ItemPathRootsCollection roots = new RootsCollection.ItemPathRootsCollection(paths);

      final Collection<PendingChange> pendingChanges = workspace.getServer().getVCS()
        .queryPendingSetsByLocalPaths(workspace.getName(), workspace.getOwnerName(), roots, RecursionType.Full);

      Collection<String> revert = new ArrayList<String>();
      for (PendingChange pendingChange : pendingChanges) {
        EnumMask<ChangeType> change = EnumMask.fromString(ChangeType.class, pendingChange.getChg());
        if (!change.contains(ChangeType.Delete)) {
          // TODO assert for possible change types here
          revert.add(pendingChange.getItem());
        }
      }

      final UndoPendingChanges.UndoPendingChangesResult undoResult = UndoPendingChanges.execute(project, workspace, revert, true);
      errors.addAll(undoResult.errors);

      List<ItemPath> undoneRoots = new ArrayList<ItemPath>(roots.size());
      for (ItemPath originalRoot : roots) {
        ItemPath undoneRoot = undoResult.undonePaths.get(originalRoot);
        undoneRoots.add(undoneRoot != null ? undoneRoot : originalRoot);
      }

      final List<FilePath> scheduleForDeletion = new ArrayList<FilePath>();
      StatusProvider.visitByStatus(workspace, undoneRoots, false, null, new StatusVisitor() {

        public void unversioned(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
          throws TfsException {
          // ignore
        }

        public void deleted(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus) {
          // ignore
        }

        public void checkedOutForEdit(final @NotNull FilePath localPath,
                                      final boolean localItemExists,
                                      final @NotNull ServerStatus serverStatus) throws TfsException {
          scheduleForDeletion.add(localPath);
        }

        public void scheduledForAddition(final @NotNull FilePath localPath,
                                         final boolean localItemExists,
                                         final @NotNull ServerStatus serverStatus) {
          scheduleForDeletion.add(localPath);
        }

        public void scheduledForDeletion(final @NotNull FilePath localPath,
                                         final boolean localItemExists,
                                         final @NotNull ServerStatus serverStatus) {
          // ignore
        }

        public void outOfDate(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
          throws TfsException {
          scheduleForDeletion.add(localPath);
        }

        public void upToDate(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
          throws TfsException {
          scheduleForDeletion.add(localPath);
        }

        public void renamed(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
          throws TfsException {
          scheduleForDeletion.add(localPath);
        }

        public void renamedCheckedOut(final @NotNull FilePath localPath,
                                      final boolean localItemExists,
                                      final @NotNull ServerStatus serverStatus) throws TfsException {
          scheduleForDeletion.add(localPath);
        }
      });

      ResultWithFailures<GetOperation> schedulingForDeletionResults =
        workspace.getServer().getVCS().scheduleForDeletionAndUpateLocalVersion(workspace.getName(), workspace.getOwnerName(), scheduleForDeletion);
      errors.addAll(BeanHelper.getVcsExceptions(schedulingForDeletionResults.getFailures()));

      for (GetOperation getOperation : schedulingForDeletionResults.getResult()) {
        String localPath = getOperation.getSlocal();
        TfsFileUtil.invalidateFile(project, VcsUtil.getFilePath(localPath));
      }
    }
    catch (TfsException e) {
      errors.add(new VcsException(e));
    }

    return errors;
  }

}
