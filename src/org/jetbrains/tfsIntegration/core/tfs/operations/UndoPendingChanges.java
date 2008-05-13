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

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.GetOperation;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ItemType;

import java.io.File;
import java.text.MessageFormat;
import java.util.*;

public class UndoPendingChanges {

  public static class UndoPendingChangesResult {
    public final Collection<VcsException> errors;
    public final Collection<ItemPath> undonePaths;

    public UndoPendingChangesResult(final Collection<ItemPath> undonePaths, final Collection<VcsException> errors) {
      this.undonePaths = undonePaths;
      this.errors = errors;
    }
  }

  public static UndoPendingChangesResult execute(final WorkspaceInfo workspace, final Collection<ItemPath> paths, boolean download) {
    if (paths.isEmpty()) {
      return new UndoPendingChangesResult(Collections.<ItemPath>emptyList(), Collections.<VcsException>emptyList());
    }

    // undo changes
    try {
      ResultWithFailures<GetOperation> result =
        workspace.getServer().getVCS().undoPendingChanges(workspace.getName(), workspace.getOwnerName(), paths);

      Collection<VcsException> errors = new ArrayList<VcsException>();
      errors.addAll(BeanHelper.getVcsExceptions(result.getFailures()));

      Collection<ItemPath> undonePaths = new ArrayList<ItemPath>(result.getResult().size());
      for (GetOperation getOperation : result.getResult()) {
        FilePath undonePath = VcsUtil.getFilePath(getOperation.getTlocal());
        undonePaths.add(new ItemPath(undonePath, workspace.findServerPathByLocalPath(undonePath)));
      }

      Collection<VcsException> postProcessingErrors = postProcess(workspace, result.getResult(), download);
      errors.addAll(postProcessingErrors);
      return new UndoPendingChangesResult(undonePaths, errors);
    }
    catch (TfsException e) {
      return new UndoPendingChangesResult(Collections.<ItemPath>emptyList(), Collections.singletonList(new VcsException(e)));
    }
  }

  public static Collection<VcsException> postProcess(WorkspaceInfo workspace,
                                                     Collection<GetOperation> operations,
                                                     final boolean allowDownload) {
    if (operations.isEmpty()) {
      return Collections.emptyList();
    }

    // parent folders modificating operations should be processed before children to update affected child paths correctly
    List<GetOperation> sortedOperations = sortGetOperations(operations);

    // postProcess
    Collection<GetOperation> updateLocalVersions = new ArrayList<GetOperation>();
    Collection<VcsException> errors = new ArrayList<VcsException>();
    for (int i = 0; i < sortedOperations.size(); i++) {
      GetOperation operationToExecute = sortedOperations.get(i);
      ChangeType change = ChangeType.fromString(operationToExecute.getChg());

      boolean downloadNeeded = false;
      boolean updateLocalVersion = false;

      TFSVcs.assertTrue(operationToExecute.getTlocal() != null);

      if (change.contains(ChangeType.Value.Add)) {
        TFSVcs.assertTrue(change.contains(ChangeType.Value.Encoding));
        TFSVcs.assertTrue(change.size() <= 3); // can be Edit as well
        // don't delete local item
      }
      else {
        TFSVcs.assertTrue(operationToExecute.getType() == ItemType.File);
        TFSVcs.assertTrue(operationToExecute.getDurl() != null);

        if (change.contains(ChangeType.Value.Edit)) {
          downloadNeeded = true;
        }
      }

      if (change.contains(ChangeType.Value.Rename)) {
        TFSVcs.assertTrue(operationToExecute.getSlocal() != null &&
                          operationToExecute.getTlocal() != null &&
                          !operationToExecute.getSlocal().equals(operationToExecute.getTlocal()));

        if (allowDownload) {
          File source = new File(operationToExecute.getSlocal());
          File target = new File(operationToExecute.getTlocal());
          if (source.exists()) {
            boolean renameSuccessful = source.renameTo(target);
            if (renameSuccessful) {
              target.setReadOnly();
            } else {
              String errorMessage = MessageFormat.format("Failed to rename ''{0}'' to ''{1}''", source.getPath(), target.getPath());
              errors.add(new VcsException(errorMessage));
            }
          }
          else {
            downloadNeeded = true;
          }
        }

        for (GetOperation operationToUpdate : sortedOperations.subList(i + 1, sortedOperations.size())) {
          if (operationToUpdate.getSlocal() != null) {
            final String updated = operationToUpdate.getSlocal().replace(operationToExecute.getSlocal(), operationToExecute.getTlocal());
            operationToUpdate.setSlocal(updated);
          }
          if (operationToUpdate.getTlocal() != null) {
            final String updated = operationToUpdate.getTlocal().replace(operationToExecute.getSlocal(), operationToExecute.getTlocal());
            operationToUpdate.setTlocal(updated);
          }
        }
        updateLocalVersion = true;
      }

      if (change.isEmpty() && operationToExecute.getType() == ItemType.File) {
        VirtualFile file = VcsUtil.getVirtualFile(operationToExecute.getTlocal());
        if (file != null && file.exists() && file.isWritable()) {
          // hijacked
          downloadNeeded = true;
          // TODO: this will cause local version update that is probably not needed 
        }
      }

      if (change.contains(ChangeType.Value.Delete)) {
        TFSVcs.assertTrue(change.containsOnly(ChangeType.Value.Delete));

        if (operationToExecute.getType() == ItemType.Folder) {
          new File(operationToExecute.getTlocal()).mkdirs();
        }
        else {
          downloadNeeded = true;
        }
      }

      if (downloadNeeded && allowDownload) {
        File targetFile = new File(operationToExecute.getTlocal());
        if (targetFile.getParentFile() != null) {
          targetFile.getParentFile().mkdirs();
        }
        try {
          VersionControlServer.downloadItem(workspace, operationToExecute.getDurl(), targetFile, true, true, true);
          updateLocalVersion = true;
        }
        catch (TfsException e) {
          errors.add(new VcsException(e));
        }
      }
      if (updateLocalVersion) {
        updateLocalVersions.add(operationToExecute);
      }
    }

    try {
      workspace.getServer().getVCS().updateLocalVersions(workspace.getName(), workspace.getOwnerName(), updateLocalVersions);
    }
    catch (TfsException e) {
      errors.add(new VcsException(e));
    }
    return errors;
  }

  private static List<GetOperation> sortGetOperations(Collection<GetOperation> getOperations) {
    List<GetOperation> result = new ArrayList<GetOperation>(getOperations.size());
    for (GetOperation newOperation : getOperations) {
      TFSVcs.assertTrue(newOperation.getSlocal() != null || newOperation.getTlocal() != null);
      final FilePath newOpPath =
        VcsUtil.getFilePath(newOperation.getSlocal() != null ? newOperation.getSlocal() : newOperation.getTlocal());

      int positionToInsert = result.size();
      for (int i = 0; i < result.size(); i++) {
        final GetOperation existingOperation = result.get(i);
        final FilePath existingOpPath =
          VcsUtil.getFilePath(existingOperation.getSlocal() != null ? existingOperation.getSlocal() : existingOperation.getTlocal());
        if (existingOpPath.isUnder(newOpPath, false)) {
          positionToInsert = i;
          break;
        }
      }
      result.add(positionToInsert, newOperation);
    }
    return result;
  }


}
