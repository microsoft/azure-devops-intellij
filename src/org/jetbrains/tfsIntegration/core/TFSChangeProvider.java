package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Processor;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.revision.TFSContentRevision;
import org.jetbrains.tfsIntegration.core.revision.TFSContentRevisionFactory;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.WorkstationHelper;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ItemType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TODO important cases
 * 1. when folder1 is unversioned and folder1/file1 is scheduled for addition, team exploded effectively shows folder1 as scheduled for addition
 */

public class TFSChangeProvider implements ChangeProvider {

  private Project myProject;

  public TFSChangeProvider(final Project project) {
    myProject = project;
  }

  public void getChanges(final VcsDirtyScope dirtyScope, final ChangelistBuilder builder, final ProgressIndicator progress)
    throws VcsException {
    if (myProject.isDisposed()) {
      return;
    }
    progress.setText("Processing changes");

    final List<String> paths = new ArrayList<String>();
    dirtyScope.iterate(new Processor<FilePath>() {
      public boolean process(final FilePath filePath) {
        paths.add(filePath.getPath());
        return true;
      }
    });

    final WorkstationHelper.ProcessResult<ExtendedItem> processResult;
    try {
      processResult = WorkstationHelper.processByWorkspaces(paths, new WorkstationHelper.Delegate<ExtendedItem>() {

        public Map<String, ExtendedItem> executeRequest(final WorkspaceInfo workspace, final List<String> serverPaths) throws TfsException {
          return workspace.getExtendedItems(serverPaths);
        }
      });

    }
    catch (TfsException e) {
      throw new VcsException(e);
    }

    for (Map.Entry<String, ExtendedItem> entry : processResult.results.entrySet()) {
      processFile(entry.getKey(), entry.getValue(), builder);
    }

    // ignore
    //for (String localPath : processResult.workspaceNotFound) {
    //}
  }

  private static void processFile(@NotNull String path, @Nullable ExtendedItem item, ChangelistBuilder builder) {
    VirtualFile virtualFile = VcsUtil.getVirtualFile(path);
    if (virtualFile == null || virtualFile.getParent() == null) {
      // TODO: IDEA crashes if reported virtual file has no parent
      return;
    }

    final FilePath filePath = VcsUtil.getFilePath(path);
    if (item == null || item.getDid() != Integer.MIN_VALUE) {  // TODO: process items deleted on server
      builder.processUnversionedFile(virtualFile);
      return;
    }
    else if (isUnversioned(item)) {
      builder.processUnversionedFile(virtualFile);
      return;
    }

    if (!virtualFile.exists()) {
      builder.processLocallyDeletedFile(filePath);
      return;
    }

    if (isChanged(item)) {
      TFSContentRevision revision = TFSContentRevisionFactory.getRevision(filePath, item.getLatest());
      builder.processChange(new Change(revision, CurrentContentRevision.create(filePath)));
    }
    else if (isHijacked(item)) {
      builder.processModifiedWithoutCheckout(virtualFile);
    }
    else if (isScheduledForAddition(item)) {
      builder.processChange(new Change(null, new CurrentContentRevision(filePath)));
    }
    else if (isScheduledForDeletion(item)) {
      builder.processChange(new Change(new CurrentContentRevision(filePath), null));
    }
  }

  private static boolean isUnversioned(final @NotNull ExtendedItem item) {
    if (item.getLocal() == null) {
      ChangeType changeType = ChangeType.fromString(item.getChg());
      if (changeType.isEmpty()) {
        //TFSVcs.assertTrue(item.getLatest() == Integer.MIN_VALUE);
        TFSVcs.assertTrue(item.getLver() == Integer.MIN_VALUE);
        TFSVcs.assertTrue(item.getDid() == Integer.MIN_VALUE);
        return true;
      }
    }
    return false;
  }

  private static boolean isScheduledForAddition(final @NotNull ExtendedItem item) {
    ChangeType changeType = ChangeType.fromString(item.getChg());
    if (changeType.contains(ChangeType.Value.Add)) {
      TFSVcs.assertTrue(changeType.contains(ChangeType.Value.Edit) || item.getType() == ItemType.Folder);
      TFSVcs.assertTrue(changeType.contains(ChangeType.Value.Encoding));
      TFSVcs.assertTrue(item.getLatest() == Integer.MIN_VALUE);
      TFSVcs.assertTrue(item.getLver() == Integer.MIN_VALUE);
      TFSVcs.assertTrue(item.getDid() == Integer.MIN_VALUE);
      return true;
    }
    return false;
  }

  private static boolean isScheduledForDeletion(final @NotNull ExtendedItem item) {
    ChangeType changeType = ChangeType.fromString(item.getChg());
    if (changeType.contains(ChangeType.Value.Delete)) {
      TFSVcs.assertTrue(changeType.containsOnly(ChangeType.Value.Delete));
      TFSVcs.assertTrue(item.getLver() == Integer.MIN_VALUE);
      TFSVcs.assertTrue(item.getDid() == Integer.MIN_VALUE);
      return true;
    }
    return false;
  }


  private static boolean isChanged(final @NotNull ExtendedItem item) {
    ChangeType changeType = ChangeType.fromString(item.getChg());
    if (changeType.containsOnly(ChangeType.Value.Edit) || changeType.containsOnly(ChangeType.Value.Edit, ChangeType.Value.Encoding)) {
      TFSVcs.assertTrue(item.getLatest() != Integer.MIN_VALUE);
      TFSVcs.assertTrue(item.getLver() != Integer.MIN_VALUE);
      TFSVcs.assertTrue(item.getDid() == Integer.MIN_VALUE);
      return true;
    }
    return false;
  }

  private static boolean isHijacked(final @NotNull ExtendedItem item) {
    ChangeType changeType = ChangeType.fromString(item.getChg());
    if (changeType.isEmpty()) {
      TFSVcs.assertTrue(item.getLatest() != Integer.MIN_VALUE);
      TFSVcs.assertTrue(item.getLver() != Integer.MIN_VALUE);
      TFSVcs.assertTrue(item.getDid() == Integer.MIN_VALUE);
      VirtualFile virtualFile = VcsUtil.getVirtualFile(item.getLocal());
      TFSVcs.assertTrue(virtualFile.exists());
      return virtualFile.isWritable() && item.getType() != ItemType.Folder; // TODO: it seems that folders must never be treated as hijacked
    }
    return false;
  }

  public boolean isModifiedDocumentTrackingRequired() {
    return true;
  }


}
