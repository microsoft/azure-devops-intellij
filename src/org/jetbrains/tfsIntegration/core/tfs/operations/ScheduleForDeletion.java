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
import com.intellij.openapi.vcs.VcsException;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ScheduleForDeletion {

  public static Collection<VcsException> execute(Project project, WorkspaceInfo workspace, List<ItemPath> paths) {
    // choose roots
    // recursively undo pending changes except schedule for deletion => map: modified_name->original_name
    // schedule roots for deletion using their original names (+updateLocalVersion)

    Collection<VcsException> errors = new ArrayList<VcsException>();

    try {
      RootsCollection.ItemPathRootsCollection roots = new RootsCollection.ItemPathRootsCollection(paths);

      final Collection<PendingChange> pendingChanges =
        workspace.getServer().getVCS().queryPendingSetsByLocalPaths(workspace.getName(), workspace.getOwnerName(), roots, RecursionType.Full);

      Collection<ItemPath> revert = new ArrayList<ItemPath>();
      for (PendingChange pendingChange : pendingChanges) {
        EnumMask<ChangeType> change = EnumMask.fromString(ChangeType.class, pendingChange.getChg());
        if (!change.contains(ChangeType.Delete)) {
          // TODO assert for possible change types here
          revert.add(new ItemPath(VcsUtil.getFilePath(pendingChange.getLocal()), pendingChange.getItem()));
        }
      }

      final UndoPendingChanges.UndoPendingChangesResult undoResult =
        UndoPendingChanges.execute(project, workspace, revert, ApplyGetOperations.DownloadMode.FORBID);
      errors.addAll(undoResult.errors);

      List<ItemPath> undoneRoots = new ArrayList<ItemPath>(roots.size());
      for (ItemPath originalRoot : roots) {
        ItemPath undoneRoot = undoResult.undonePaths.get(originalRoot);
        undoneRoots.add(undoneRoot != null ? undoneRoot : originalRoot);
      }

      Map<ItemPath, ExtendedItem> serverItems = workspace.getServer().getVCS()
        .getExtendedItems(workspace.getName(), workspace.getOwnerName(), undoneRoots, DeletedState.NonDeleted);

      List<ItemPath> scheduleForDeletion = new ArrayList<ItemPath>();
      for (Map.Entry<ItemPath, ExtendedItem> e : serverItems.entrySet()) {
        ServerStatus serverStatus = StatusProvider.determineServerStatus(e.getValue());
        if (serverStatus instanceof ServerStatus.Unversioned == false &&
            serverStatus instanceof ServerStatus.Deleted == false &&
            serverStatus instanceof ServerStatus.ScheduledForDeletion == false) {
          scheduleForDeletion.add(e.getKey());
        }
      }

      ResultWithFailures<GetOperation> schedulingForDeletionResults =
        workspace.getServer().getVCS().scheduleForDeletion(workspace.getName(), workspace.getOwnerName(), scheduleForDeletion);
      errors.addAll(BeanHelper.getVcsExceptions(schedulingForDeletionResults.getFailures()));

      workspace.getServer().getVCS()
        .updateLocalVersionsByGetOperations(workspace.getName(), workspace.getOwnerName(), schedulingForDeletionResults.getResult());

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
