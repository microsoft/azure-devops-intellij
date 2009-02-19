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

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vcs.update.FileGroup;
import com.intellij.openapi.vcs.update.UpdatedFiles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSProgressUtil;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.exceptions.OperationFailedException;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.GetOperation;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ItemType;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.LocalVersionUpdate;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// TODO review file groups

@SuppressWarnings({"HardCodedStringLiteral"})
public class ApplyGetOperations {
  private static LocalConflictHandlingType ourLocalConflictHandlingType = LocalConflictHandlingType.SHOW_MESSAGE;


  private final Project myProject;
  private final WorkspaceInfo myWorkspace;
  private final Collection<GetOperation> myOperations;
  private final @NotNull ApplyProgress myProgress;
  private final @Nullable UpdatedFiles myUpdatedFiles;
  private final Collection<VcsException> myErrors = new ArrayList<VcsException>();
  private final Collection<LocalVersionUpdate> myUpdateLocalVersions = new ArrayList<LocalVersionUpdate>();
  private final DownloadMode myDownloadMode;

  public enum DownloadMode {
    FORCE,
    ALLOW,
    FORBID,
    MERGE
  }

  public enum LocalConflictHandlingType {
    SHOW_MESSAGE,
    ERROR
  }

  private ApplyGetOperations(Project project,
                             WorkspaceInfo workspace,
                             Collection<GetOperation> operations,
                             final @NotNull ApplyProgress progress,
                             final @Nullable UpdatedFiles updatedFiles,
                             final DownloadMode downloadMode) {
    myProject = project;
    myWorkspace = workspace;
    myOperations = operations;
    myProgress = progress;
    myUpdatedFiles = updatedFiles;
    myDownloadMode = downloadMode;
  }

  public static LocalConflictHandlingType getLocalConflictHandlingType() {
    return ourLocalConflictHandlingType;
  }

  public static void setLocalConflictHandlingType(LocalConflictHandlingType type) {
    ourLocalConflictHandlingType = type;
  }

  public static Collection<VcsException> execute(Project project,
                                                 WorkspaceInfo workspace,
                                                 Collection<GetOperation> operations,
                                                 final @NotNull ApplyProgress progress,
                                                 final @Nullable UpdatedFiles updatedFiles,
                                                 DownloadMode downloadMode) {
    ApplyGetOperations session = new ApplyGetOperations(project, workspace, operations, progress, updatedFiles, downloadMode);
    session.execute();
    return session.myErrors;
  }

  private void execute() {
    if (myOperations.isEmpty()) {
      return;
    }

    // parent folders modificating operations should be processed before children to update affected child paths correctly
    List<GetOperation> sortedOperations = new ArrayList<GetOperation>(myOperations);//GetOperationsUtil.sortGetOperations(myOperations);
    // TODO do we need to sort them or they come in apply order?

    try {
      for (int i = 0; i < sortedOperations.size(); i++) {
        if (myProgress.isCancelled()) {
          throw new ProcessCanceledException();
        }

        GetOperation operationToExecute = sortedOperations.get(i);

        final String currentPath = VersionControlPath.localPathFromTfsRepresentation(
          operationToExecute.getTlocal() != null ? operationToExecute.getTlocal() : operationToExecute.getSlocal());
        myProgress.setFraction(i / sortedOperations.size());
        myProgress.setText(currentPath);

        if (operationToExecute.getCnflct()) {
          // TODO can be confict on undo?
          // conflict will be resolved later
          processConflict(operationToExecute);
        }
        else if (operationToExecute.getSlocal() == null && operationToExecute.getTlocal() == null) {
          updateLocalVersion(operationToExecute);
        }
        else if (operationToExecute.getTlocal() == null) {
          if (operationToExecute.getType() == ItemType.File) {
            processDeleteFile(operationToExecute);
          }
          else {
            processDeleteFolder(operationToExecute);
          }
        }
        else if (operationToExecute.getSlocal() == null) {
          if (operationToExecute.getType() == ItemType.File) {
            processCreateFile(operationToExecute);
          }
          else {
            processCreateFolder(operationToExecute);
          }
        }
        else if (operationToExecute.getType() == ItemType.File) {
          processFileChange(operationToExecute);
        }
        else {
          processFolderChange(operationToExecute);
          if (!operationToExecute.getSlocal().equals(operationToExecute.getTlocal())) {
            GetOperationsUtil.updateSourcePaths(sortedOperations, i, operationToExecute);
          }
        }
      }

      myWorkspace.getServer().getVCS().updateLocalVersions(myWorkspace.getName(), myWorkspace.getOwnerName(), myUpdateLocalVersions);
    }
    catch (TfsException e) {
      myErrors.add(new VcsException(e));
    }
  }

  private void processDeleteFile(final GetOperation operation) throws TfsException {
    File source = VersionControlPath.getFile(operation.getSlocal());
    if (source.isDirectory()) {
      String errorMessage = MessageFormat.format("Failed to delete file ''{0}''. Folder with the same name exists", source.getPath());
      myErrors.add(new VcsException(errorMessage));
      return;
    }
    if (source.canWrite() && !canOverrideLocalConflictingItem(operation, true)) {
      return;
    }

    final boolean exists = source.exists();
    if (!deleteFile(source)) {
      return;
    }

    updateLocalVersion(operation);
    if (exists) {
      addToGroup(FileGroup.REMOVED_FROM_REPOSITORY_ID, source, operation);
    }
  }

  private void processDeleteFolder(final GetOperation operation) throws TfsException {
    File source = VersionControlPath.getFile(operation.getSlocal());
    if (source.isFile()) {
      String errorMessage = MessageFormat.format("Failed to delete folder ''{0}''. File with the same name exists", source.getPath());
      myErrors.add(new VcsException(errorMessage));
      return;
    }

    // TODO: if force, delete anyway?
    if (!canDeleteFolder(source)) {
      String errorMessage = MessageFormat.format("Failed to delete folder ''{0}'' because it is not empty", source.getPath());
      myErrors.add(new VcsException(errorMessage));
      return;
    }

    boolean exists = source.exists();
    if (!deleteFile(source)) {
      return;
    }

    updateLocalVersion(operation);
    if (exists) {
      addToGroup(FileGroup.REMOVED_FROM_REPOSITORY_ID, source, operation);
    }
  }

  private void processCreateFile(final @NotNull GetOperation operation) throws TfsException {
    File target = VersionControlPath.getFile(operation.getTlocal());
    if (target.isDirectory()) {
      String errorMessage = MessageFormat.format("Failed to create file ''{0}''. Folder with the same name exists", target.getPath());
      myErrors.add(new VcsException(errorMessage));
      return;
    }

    if (target.canWrite()) {
      if (!canOverrideLocalConflictingItem(operation, false)) {
        return;
      }
      if (!FileUtil.delete(target)) {
        String errorMessage = MessageFormat.format("Failed to overwrite file ''{0}''", target.getPath());
        myErrors.add(new VcsException(errorMessage));
        return;
      }
    }

    if (!createFolder(target.getParentFile())) {
      return;
    }

    if (downloadFile(operation)) {
      updateLocalVersion(operation);
      addToGroup(FileGroup.CREATED_ID, target, operation);
    }
  }

  private void processCreateFolder(final GetOperation operation) throws TfsException {
    File target = VersionControlPath.getFile(operation.getTlocal());
    if (target.isFile() && target.canWrite() && !canOverrideLocalConflictingItem(operation, false)) {
      return;
    }

    if (target.isFile() && !FileUtil.delete(target)) {
      String errorMessage = MessageFormat.format("Failed to create folder ''{0}''. File with the same name exists", target.getPath());
      myErrors.add(new VcsException(errorMessage));
      return;
    }

    boolean folderExists = target.exists();
    if (!createFolder(target)) {
      return;
    }

    updateLocalVersion(operation);
    if (!folderExists) {
      addToGroup(FileGroup.CREATED_ID, target, operation);
    }
  }

  private void processFileChange(final GetOperation operation) throws TfsException {
    File source = VersionControlPath.getFile(operation.getSlocal());
    File target = VersionControlPath.getFile(operation.getTlocal());
    final EnumMask<ChangeType> change = EnumMask.fromString(ChangeType.class, operation.getChg());

    if (source.equals(target) &&
        operation.getLver() == operation.getSver() &&
        (change.containsOnly(ChangeType.Rename) || (myDownloadMode != DownloadMode.FORCE && myDownloadMode != DownloadMode.MERGE))) {
      // rename + source=target means rename of parent folder 
      // not an explicit change, nothing to do
      updateLocalVersion(operation);
      return;
    }

    if (!source.equals(target) && source.canWrite()) {
      if (canOverrideLocalConflictingItem(operation, true)) {
        if (myDownloadMode == DownloadMode.FORCE && !deleteFile(source)) {
          return;
        }
      }
      else {
        return;
      }
    }

    if (!source.equals(target) && source.isDirectory() && (!canOverrideLocalConflictingItem(operation, true) || !deleteFile(source))) {
      return;
    }

    if (target.isDirectory()) {
      // Note: TFC does not report local conflict in this case
      String errorMessage = MessageFormat.format("Failed to create file ''{0}''. Folder with same name exists", target.getPath());
      myErrors.add(new VcsException(errorMessage));
      return;
    }

    // don't ask 2nd time if source = target
    if (target.canWrite() && !target.equals(source) && !canOverrideLocalConflictingItem(operation, false)) {
      return;
    }

    if (!createFolder(target.getParentFile())) {
      return;
    }

    if (myDownloadMode == DownloadMode.FORCE || (myDownloadMode != DownloadMode.MERGE && operation.getLver() != operation.getSver())) {
      // remove source, create target
      // don't download file if undoing Add
      if ((source.equals(target) || deleteFile(source)) && (change.contains(ChangeType.Add) || downloadFile(operation))) {
        updateLocalVersion(operation);
        if (source.equals(target)) {
          addToGroup(FileGroup.UPDATED_ID, target, operation);
        }
        else {
          addToGroup(FileGroup.REMOVED_FROM_REPOSITORY_ID, source, operation);
          addToGroup(FileGroup.CREATED_ID, target, operation);
        }
      }
      return;
    }

    if (!target.exists()) {
      if (source.exists()) {
        // source exists (so it is a file), target is not
        if (rename(source, target)) {
          addToGroup(FileGroup.UPDATED_ID, target, operation);
          updateLocalVersion(operation);
        }
      }
      else {
        // source & target not exist
        // don't create file if undoing locally missing scheduled for addition file
        if (!change.contains(ChangeType.Add) || !source.equals(target) || operation.getLver() != operation.getSver()) {
          if (downloadFile(operation)) {
            addToGroup(FileGroup.CREATED_ID, target, operation);
            updateLocalVersion(operation);
          }
        }
      }
    }
    else {
      // target exists
      if (!source.equals(target)) {
        deleteFile(source);
      }
      if (myDownloadMode == DownloadMode.MERGE) {
        addToGroup(FileGroup.MERGED_ID, target, operation);
      }
      updateLocalVersion(operation);
    }
  }

  private void processFolderChange(final GetOperation operation) throws TfsException {
    File source = VersionControlPath.getFile(operation.getSlocal());
    File target = VersionControlPath.getFile(operation.getTlocal());
    final EnumMask<ChangeType> change = EnumMask.fromString(ChangeType.class, operation.getChg());

    if (source.equals(target) &&
        operation.getLver() == operation.getSver() &&
        (change.containsOnly(ChangeType.Rename) || (myDownloadMode != DownloadMode.FORCE && myDownloadMode != DownloadMode.MERGE))) {
      // rename + source=target means rename of parent folder
      // not an explicit change, nothing to do
      updateLocalVersion(operation);
      return;
    }

    // redundand check source.equals(target) for consistency with processFileChange() 
    if (!source.equals(target) && source.isFile() && !source.canWrite() && !deleteFile(source)) {
      return;
    }

    if (target.isFile() && !target.canWrite() && !deleteFile(target)) {
      return;
    }

    if (target.isFile() && target.canWrite() && (!canOverrideLocalConflictingItem(operation, false) || !deleteFile(target))) {
      return;
    }

    if (!createFolder(target.getParentFile())) {
      return;
    }

    if (!target.exists()) {
      if (source.isDirectory()) {
        // source exists, target is not
        if (rename(source, target)) {
          addToGroup(FileGroup.UPDATED_ID, target, operation);
          updateLocalVersion(operation);
        }
      }
      else {
        // source & target not exist
        // don't create folder if undoing locally missing scheduled for addition folder
        if (!change.contains(ChangeType.Add) || !source.equals(target) || operation.getLver() != operation.getSver()) {
          if (createFolder(target)) {
            addToGroup(FileGroup.CREATED_ID, target, operation);
            updateLocalVersion(operation);
          }
        }
      }
    }
    else {
      // target exists
      if (!source.equals(target)) {
        deleteFile(source);
      }
      updateLocalVersion(operation);
    }
  }

  private void processConflict(final GetOperation operation) {
    //File subject = new File(operation.getTlocal() != null ? operation.getTlocal() : operation.getSlocal());
    //addToGroup(FileGroup.MODIFIED_ID, subject, operation);
  }

  private void addToGroup(String groupId, File file, GetOperation operation) {
    if (myUpdatedFiles != null) {
      int revisionNumber = operation.getSver() != Integer.MIN_VALUE ? operation.getSver() : 0;
      myUpdatedFiles.getGroupById(groupId).add(file.getPath(), TFSVcs.getInstance(myProject), new VcsRevisionNumber.Int(revisionNumber));
    }
  }

  private boolean deleteFile(File target) {
    if (myDownloadMode != DownloadMode.FORBID && !FileUtil.delete(target)) {
      String errorMessage = MessageFormat.format("Failed to delete {0} ''{1}''", target.isFile() ? "file" : "folder", target.getPath());
      myErrors.add(new VcsException(errorMessage));
      return false;
    }
    else {
      return true;
    }
  }

  private boolean canDeleteFolder(final File folder) {
    File[] files = folder.listFiles();
    if (files != null) {
      for (File child : files) {
        if (child.isFile()) {
          if (child.canWrite()) {
            return false;
          }
          else {
            boolean childWillBeDeletedAnyway = false;
            for (GetOperation operation : myOperations) {
              if (operation.getSlocal() != null &&
                  VersionControlPath.getFile(operation.getSlocal()).equals(child) &&
                  operation.getTlocal() == null) {
                childWillBeDeletedAnyway = true;
                break;
              }
            }
            if (!childWillBeDeletedAnyway) {
              return false;
            }
          }
        }
        else if (!canDeleteFolder(child)) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean createFolder(File target) {
    if (myDownloadMode != DownloadMode.FORBID && !target.exists() && !target.mkdirs()) {
      String errorMessage = MessageFormat.format("Failed to create folder ''{0}''", target.getPath());
      myErrors.add(new VcsException(errorMessage));
      return false;
    }
    else {
      return true;
    }
  }

  private boolean rename(File source, File target) {
    if (myDownloadMode != DownloadMode.FORBID && !source.equals(target) && !source.renameTo(target)) {
      String errorMessage = MessageFormat
        .format("Failed to rename {0} ''{1}'' to ''{2}''", source.isFile() ? "file" : "folder", source.getPath(), target.getPath());
      myErrors.add(new VcsException(errorMessage));
      return false;
    }
    else {
      return true;
    }
  }

  private boolean downloadFile(final GetOperation operation) throws TfsException {
    TFSVcs.assertTrue(operation.getDurl() != null,
                      "Null download url for " + VersionControlPath.localPathFromTfsRepresentation(operation.getTlocal()));

    if (myDownloadMode == DownloadMode.FORBID) {
      return true;
    }

    final File target = VersionControlPath.getFile(operation.getTlocal());
    try {
      TfsFileUtil.setFileContent(target, new TfsFileUtil.ContentWriter() {
        public void write(final OutputStream outputStream) throws TfsException {
          VersionControlServer.downloadItem(myProject, myWorkspace.getServer(), operation.getDurl(), outputStream);
        }
      });
      if (!target.setReadOnly()) {
        String errorMessage = MessageFormat.format("Failed to write to file ''{0}''", target.getPath());
        myErrors.add(new VcsException(errorMessage));
        return false;
      }
      return true;
    }
    catch (IOException e) {
      String errorMessage = MessageFormat.format("Failed to write to file ''{0}'': {1}", target.getPath(), e.getMessage());
      myErrors.add(new VcsException(errorMessage));
      return false;
    }
  }

  private boolean canOverrideLocalConflictingItem(final GetOperation operation, boolean sourceNotTarget) throws TfsException {
    if (myDownloadMode == DownloadMode.FORCE || myDownloadMode == DownloadMode.MERGE) {
      return true;
    }

    LocalConflictHandlingType conflictHandlingType = getLocalConflictHandlingType();
    if (conflictHandlingType == LocalConflictHandlingType.ERROR) {
      throw new OperationFailedException("Local conflict detected for " +
                                         VersionControlPath.localPathFromTfsRepresentation(
                                           sourceNotTarget ? operation.getSlocal() : operation.getTlocal()));
    }
    else if (conflictHandlingType == LocalConflictHandlingType.SHOW_MESSAGE) {
      String path = VersionControlPath.localPathFromTfsRepresentation(sourceNotTarget ? operation.getSlocal() : operation.getTlocal());
      final String message = MessageFormat.format("Local conflict detected. Override local item?\n {0}", path);
      // TODO: more detailed message needed
      final String title = "Modify files";
      final Ref<Integer> result = new Ref<Integer>();
      try {
        TfsUtil.runOrInvokeAndWait(new Runnable() {
          public void run() {
            result.set(Messages.showYesNoDialog(message, title, Messages.getQuestionIcon()));
          }
        });
      }
      catch (InvocationTargetException e) {
        // ignore
      }
      catch (InterruptedException e) {
        // ignore
      }
      if (result.get() == DialogWrapper.OK_EXIT_CODE) {
        return true;
      }
      else {
        reportLocalConflict(operation, sourceNotTarget);
        return false;
      }
    }
    else {
      throw new IllegalArgumentException("Unknown conflict handling type: " + conflictHandlingType);
    }
  }

  private void reportLocalConflict(final GetOperation operation, boolean sourceNotTarget) throws TfsException {
    int reason = sourceNotTarget ? VersionControlServer.LOCAL_CONFLICT_REASON_SOURCE : VersionControlServer.LOCAL_CONFLICT_REASON_TARGET;
    myWorkspace.getServer().getVCS()
      .addLocalConflict(myWorkspace.getName(), myWorkspace.getOwnerName(), operation.getItemid(), operation.getSver(),
                        operation.getPcid() != Integer.MIN_VALUE ? operation.getPcid() : 0, operation.getSlocal(), operation.getTlocal(),
                        reason);
  }

  private void updateLocalVersion(GetOperation operation) {
    myUpdateLocalVersions.add(VersionControlServer.getLocalVersionUpdate(operation));
  }

}
