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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.DialogWrapper;
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
import org.jetbrains.tfsIntegration.core.tfs.ChangeType;
import org.jetbrains.tfsIntegration.core.tfs.TfsFileUtil;
import org.jetbrains.tfsIntegration.core.tfs.VersionControlServer;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.exceptions.OperationFailedException;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.GetOperation;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ItemType;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.LocalVersionUpdate;

import java.io.File;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// TODO review file groups

@SuppressWarnings({"HardCodedStringLiteral"})
public class ApplyGetOperations {
  private static LocalConflictHandlingType ourLocalConflictHandlingType = LocalConflictHandlingType.SHOW_MESSAGE;


  private Project myProject;
  private final WorkspaceInfo myWorkspace;
  private final Collection<GetOperation> myOperations;
  private final @Nullable ProgressIndicator myProgressIndicator;
  private final @Nullable UpdatedFiles myUpdatedFiles;
  private final Collection<VcsException> myErrors = new ArrayList<VcsException>();
  private final Collection<LocalVersionUpdate> myUpdateLocalVersions = new ArrayList<LocalVersionUpdate>();
  private final DownloadMode myDownloadMode;
  private final ProcessMode myProcessMode;

  public enum DownloadMode {
    FORCE,
    ALLOW,
    FORBID
  }

  public enum ProcessMode {
    GET,
    UNDO,
    RESOLVE
  }

  public enum LocalConflictHandlingType {
    OVERRIDE_LOCAL_ITEM,
    REPORT_LOCAL_CONFLICT,
    SHOW_MESSAGE,
    ERROR
  }

  private ApplyGetOperations(Project project,
                             WorkspaceInfo workspace,
                             Collection<GetOperation> operations,
                             final @Nullable ProgressIndicator progressIndicator,
                             final @Nullable UpdatedFiles updatedFiles,
                             final DownloadMode downloadMode,
                             final ProcessMode processMode) {
    myProject = project;
    myWorkspace = workspace;
    myOperations = operations;
    myProgressIndicator = progressIndicator;
    myUpdatedFiles = updatedFiles;
    myDownloadMode = downloadMode;
    myProcessMode = processMode;
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
                                                 final @Nullable ProgressIndicator progressIndicator,
                                                 final @Nullable UpdatedFiles updatedFiles,
                                                 DownloadMode downloadMode,
                                                 ProcessMode operationType) {
    ApplyGetOperations session =
      new ApplyGetOperations(project, workspace, operations, progressIndicator, updatedFiles, downloadMode, operationType);
    session.execute();
    return session.myErrors;
  }

  private void execute() {
    if (myOperations.isEmpty()) {
      return;
    }

    // parent folders modificating operations should be processed before children to update affected child paths correctly
    List<GetOperation> sortedOperations = GetOperationsUtil.sortGetOperations(myOperations);
    // TODO do we need to sort them or they come in apply order?

    try {
      for (int i = 0; i < sortedOperations.size(); i++) {
        TFSProgressUtil.checkCanceled(myProgressIndicator);

        GetOperation operationToExecute = sortedOperations.get(i);

        String progressText = (operationToExecute.getTlocal() != null ? operationToExecute.getTlocal() : operationToExecute.getSlocal());
        TFSProgressUtil.setProgressText(myProgressIndicator, progressText);

        if (operationToExecute.getCnflct()) {
          // TODO can be confict on undo?
          // conflict will be resolved later
          processConflict(operationToExecute);
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

      myWorkspace.getServer().getVCS()
        .updateLocalVersions(myWorkspace.getName(), myWorkspace.getOwnerName(), myUpdateLocalVersions);
    }
    catch (TfsException e) {
      myErrors.add(new VcsException(e));
    }
  }

  private void processDeleteFile(final GetOperation operation) throws TfsException {
    File source = new File(operation.getSlocal());
    if (source.isDirectory()) {
      String errorMessage = MessageFormat.format("Failed to delete file ''{0}''. Folder with the same name exists", source.getPath());
      myErrors.add(new VcsException(errorMessage));
      return;
    }
    if (myProcessMode != ProcessMode.UNDO && source.canWrite()) {
      if (!canOverrideLocalConflictingItem(operation, true)) {
        return;
      }
    }

    final boolean exists = source.exists();
    if (exists && !FileUtil.delete(source)) {
      String errorMessage = MessageFormat.format("Failed to delete file ''{0}''", source.getPath());
      myErrors.add(new VcsException(errorMessage));
      return;
    }

    updateLocalVersion(operation);
    if (exists) {
      addToGroup(FileGroup.REMOVED_FROM_REPOSITORY_ID, source, operation);
    }
  }

  private void processDeleteFolder(final GetOperation operation) throws TfsException {
    File source = new File(operation.getSlocal());
    if (source.isFile()) {
      String errorMessage = MessageFormat.format("Failed to delete folder ''{0}''. File with the same name exists", source.getPath());
      myErrors.add(new VcsException(errorMessage));
      return;
    }

    if (!canDeleteFolder(source)) {
      String errorMessage = MessageFormat.format("Failed to delete folder ''{0}'' because it is not empty", source.getPath());
      myErrors.add(new VcsException(errorMessage));
      return;
    }

    if (!FileUtil.delete(source)) {
      String errorMessage = MessageFormat.format("Failed to delete folder ''{0}''", source.getPath());
      myErrors.add(new VcsException(errorMessage));
      return;
    }

    updateLocalVersion(operation);
    addToGroup(FileGroup.REMOVED_FROM_REPOSITORY_ID, source, operation);
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
              if (operation.getSlocal() != null && new File(operation.getSlocal()).equals(child) && operation.getTlocal() == null) {
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

  private void processCreateFile(final @NotNull GetOperation operation) throws TfsException {
    if (!ensureTargetIsReadonlyFile(operation)) {
      return;
    }

    File target = new File(operation.getTlocal());

    if (!target.getParentFile().exists() && !createFolder(target.getParentFile())) {
      String errorMessage = MessageFormat.format("Failed to create folder ''{0}''", target.getParentFile().getPath());
      myErrors.add(new VcsException(errorMessage));
      return;
    }

    boolean fileExists = target.exists();
    if (operation.getDurl() != null) {
      downloadFile(operation);
    }

    updateLocalVersion(operation);
    addToGroup(fileExists ? FileGroup.SKIPPED_ID : FileGroup.CREATED_ID, target, operation);
  }

  private void processCreateFolder(final GetOperation operation) throws TfsException {
    File target = new File(operation.getTlocal());
    if (target.isFile() && target.canWrite()) {
      if (!canOverrideLocalConflictingItem(operation, false)) {
        return;
      }
    }

    if (target.isFile() && !FileUtil.delete(target)) {
      String errorMessage = MessageFormat.format("Failed to delete file ''{0}''", target.getPath());
      myErrors.add(new VcsException(errorMessage));
      return;
    }

    boolean folderExists = target.exists();
    if (!folderExists && !createFolder(target)) {
      String errorMessage = MessageFormat.format("Failed to create folder ''{0}''", target.getPath());
      myErrors.add(new VcsException(errorMessage));
      return;
    }

    updateLocalVersion(operation);
    if (!folderExists) {
      addToGroup(FileGroup.RESTORED_ID, target, operation);
    }
  }

  private void processFileChange(final GetOperation operation) throws TfsException {
    final ChangeType changeType = ChangeType.fromString(operation.getChg());
    boolean download =
      operation.getSver() != operation.getLver() || (myProcessMode == ProcessMode.UNDO && changeType.contains(ChangeType.Value.Edit));

    File source = new File(operation.getSlocal());
    File target = new File(operation.getTlocal());

    boolean rename = !source.equals(target);

    if (!rename && !download) {
      if (myDownloadMode == DownloadMode.FORCE && operation.getDurl() != null) {
        if (!target.getParentFile().exists() && !createFolder(target.getParentFile())) {
          String errorMessage = MessageFormat.format("Failed to create folder ''{0}''", target.getParentFile().getPath());
          myErrors.add(new VcsException(errorMessage));
          return;
        }

        downloadFile(operation);
        addToGroup(FileGroup.RESTORED_ID, target, operation);
      }
      updateLocalVersion(operation);
      return;
    }

    if (!target.getParentFile().exists() && !createFolder(target.getParentFile())) {
      String errorMessage = MessageFormat.format("Failed to create folder ''{0}''", target.getParentFile().getPath());
      myErrors.add(new VcsException(errorMessage));
      return;
    }

    if (target.isDirectory()) {
      // Note: TFC does not report local conflict in this case
      String errorMessage = MessageFormat.format("Failed to create file ''{0}''. Folder with same name exists", target.getPath());
      myErrors.add(new VcsException(errorMessage));
      return;
    }

    if (source.isDirectory() || (source.canWrite() && myProcessMode != ProcessMode.UNDO)) {
      if (canOverrideLocalConflictingItem(operation, true)) {
        // remove source
        if (!FileUtil.delete(source)) {
          String errorMessage = MessageFormat.format("Failed to delete ''{0}''", source.getPath());

          myErrors.add(new VcsException(errorMessage));
          return;
        }
      }
      else {
        return;
      }
    }

    if (target.canWrite() && myProcessMode != ProcessMode.UNDO) {
      if (!canOverrideLocalConflictingItem(operation, false)) {
        String errorMessage = MessageFormat.format("Failed to delete file ''{0}''", target.getPath());

        myErrors.add(new VcsException(errorMessage));
        return;
      }
    }

    if (rename && !download && source.exists()) {
      if (source.renameTo(target)) {
        addToGroup(myProcessMode == ProcessMode.UNDO ? FileGroup.RESTORED_ID : FileGroup.UPDATED_ID, target, operation);
        updateLocalVersion(operation);
      }
      else {
        String errorMessage = MessageFormat.format("Failed to rename file ''{0}'' to ''{1}''", source.getPath(), target.getPath());
        myErrors.add(new VcsException(errorMessage));
      }
    }
    else {
      if (rename && !FileUtil.delete(source)) {
        String errorMessage = MessageFormat.format("Failed to delete file ''{0}''", source.getPath());
        myErrors.add(new VcsException(errorMessage));
        return;
      }

      if (operation.getDurl() != null) {
        downloadFile(operation);
        updateLocalVersion(operation);
        addToGroup(myProcessMode == ProcessMode.UNDO ? FileGroup.RESTORED_ID : FileGroup.UPDATED_ID, target, operation);
      }
    }
  }

  private void processFolderChange(final GetOperation operation) throws TfsException {
    File source = new File(operation.getSlocal());
    File target = new File(operation.getTlocal());

    if (source.isFile() && !source.canWrite() && !FileUtil.delete(source)) {
      String errorMessage = MessageFormat.format("Failed to delete file ''{0}''", source.getPath());
      myErrors.add(new VcsException(errorMessage));
      return;
    }

    if (target.isFile() && !target.canWrite() && !FileUtil.delete(target)) {
      String errorMessage = MessageFormat.format("Failed to delete file ''{0}''", target.getPath());
      myErrors.add(new VcsException(errorMessage));
      return;
    }

    if (target.isFile() && target.canWrite()) {
      if (!canOverrideLocalConflictingItem(operation, false)) {
        return;
      }
      if (!FileUtil.delete(target)) {
        String errorMessage = MessageFormat.format("Failed to delete file ''{0}''", target.getPath());
        myErrors.add(new VcsException(errorMessage));
        return;
      }
    }

    if (!target.getParentFile().exists() && !createFolder(target.getParentFile())) {
      String errorMessage = MessageFormat.format("Failed to create folder ''{0}''.", target.getParentFile().getPath());
      myErrors.add(new VcsException(errorMessage));
      return;
    }

    if (!target.exists()) {
      // source exists, target is not
      if (source.exists()) {
        if (source.renameTo(target)) {
          addToGroup(FileGroup.UPDATED_ID, target, operation);
          updateLocalVersion(operation);
        }
        else {
          String errorMessage = MessageFormat.format("Failed to rename folder ''{0}'' to ''{1}''", source.getPath(), target.getPath());
          myErrors.add(new VcsException(errorMessage));
        }
      }
      // source & target not exist
      else {
        updateLocalVersion(operation);

        // don't create folder if undoing locally missing scheduled for addition folder
        if (!ChangeType.fromString(operation.getChg()).contains(ChangeType.Value.Add) &&
            (!source.equals(target) || myDownloadMode == DownloadMode.FORCE)) {
          if (createFolder(target)) {
            addToGroup(FileGroup.CREATED_ID, target, operation);
          }
          else {
            String errorMessage = MessageFormat.format("Failed to create folder ''{0}''", target.getPath());
            myErrors.add(new VcsException(errorMessage));
          }
        }
      }
    }
    // target exists
    else {
      updateLocalVersion(operation);
      if (!source.equals(target)) {
        if (!FileUtil.delete(source)) {
          String errorMessage = MessageFormat.format("Failed to delete folder ''{0}''", source.getPath());
          myErrors.add(new VcsException(errorMessage));
        }
      }
    }
  }

  private void processConflict(final GetOperation operation) {
    File subject = new File(operation.getTlocal() != null ? operation.getTlocal() : operation.getSlocal());
    addToGroup(FileGroup.MODIFIED_ID, subject, operation);
  }

  private void addToGroup(String groupId, File file, GetOperation operation) {
    if (myUpdatedFiles != null) {
      int revisionNumber = operation.getSver() != Integer.MIN_VALUE ? operation.getSver() : 0;
      myUpdatedFiles.getGroupById(groupId).add(file.getPath(), TFSVcs.getInstance(myProject), new VcsRevisionNumber.Int(revisionNumber));
    }
  }

  private boolean ensureTargetIsReadonlyFile(GetOperation operation) throws TfsException {
    File targetFile = new File(operation.getTlocal());
    if (targetFile.isDirectory()) {
      String errorMessage = MessageFormat.format("Failed to create file ''{0}''. Folder with the same name exists", targetFile.getPath());

      myErrors.add(new VcsException(errorMessage));
      return false;
    }

    if (targetFile.canWrite()) {
      if (canOverrideLocalConflictingItem(operation, false)) {
        if (!FileUtil.delete(targetFile)) {
          String errorMessage = MessageFormat.format("Failed to delete file ''{0}''", targetFile.getPath());

          myErrors.add(new VcsException(errorMessage));
        }
      }
      else {
        return false;
      }
    }
    return true;
  }

  private boolean createFolder(File target) {
    return myDownloadMode == DownloadMode.FORBID || target.mkdirs();
  }

  private void downloadFile(final GetOperation operation) throws TfsException {
    if (myDownloadMode == DownloadMode.FORBID) {
      return;
    }
    final File target = new File(operation.getTlocal());
    TfsFileUtil.setFileContent(target, new TfsFileUtil.ContentWriter() {
      public void write(final OutputStream outputStream) throws TfsException {
        VersionControlServer.downloadItem(myWorkspace.getServer(), operation.getDurl(), outputStream);
      }
    });
    target.setReadOnly();
  }

  private boolean canOverrideLocalConflictingItem(final GetOperation operation, boolean sourceNotTarget) throws TfsException {
    LocalConflictHandlingType conflictHandlingType = getLocalConflictHandlingType();
    if (conflictHandlingType == LocalConflictHandlingType.ERROR) {
      throw new OperationFailedException(
        "Local conflict detected for " + (sourceNotTarget ? operation.getSlocal() : operation.getTlocal()));
    }
    else if (conflictHandlingType == LocalConflictHandlingType.SHOW_MESSAGE) {
      String itemName = sourceNotTarget ? operation.getSlocal() : operation.getTlocal();
      final String message =
        MessageFormat.format("Local conflict detected. Override local item?\n {0}", itemName); // TODO: more detailed message needed
      final String title = "Modify files";
      final Ref<Integer> result = new Ref<Integer>();
      TfsFileUtil.executeInEventDispatchThread(new Runnable() {
        public void run() {
          result.set(Messages.showYesNoDialog(message, title, Messages.getQuestionIcon()));
        }
      });
      if (result.get() == DialogWrapper.OK_EXIT_CODE) {
        return true;
      }
      else {
        reportLocalConflict(operation, sourceNotTarget);
        return false;
      }
    }
    else if (conflictHandlingType == LocalConflictHandlingType.OVERRIDE_LOCAL_ITEM) {
      return true;
    }
    else {
      throw new IllegalArgumentException("Unknown conflict handling type: " + conflictHandlingType);
    }
  }

  private void reportLocalConflict(final GetOperation operation, boolean sourceNotTarget) throws TfsException {
    int reason = sourceNotTarget ? VersionControlServer.LOCAL_CONFLICT_REASON_SOURCE : VersionControlServer.LOCAL_CONFLICT_REASON_TARGET;
    myWorkspace.getServer().getVCS().addLocalConflict(myWorkspace.getName(), myWorkspace.getOwnerName(), operation.getItemid(),
                                                      operation.getSver(),
                                                      operation.getPcid() != Integer.MIN_VALUE ? operation.getPcid() : 0,
                                                      operation.getSlocal(), operation.getTlocal(), reason);
  }

  private void updateLocalVersion(GetOperation operation) {
    myUpdateLocalVersions.add(VersionControlServer.getLocalVersionUpdate(operation));
  }

}
