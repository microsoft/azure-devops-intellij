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
import org.jetbrains.tfsIntegration.core.tfs.Workstation;
import org.jetbrains.tfsIntegration.stubs.org.jetbrains.tfsIntegration.stubs.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ItemType;

import java.util.*;

public class TFSChangeProvider implements ChangeProvider {

  private Project myProject;

  private static final ExtendedItem WORKSPACE_NOT_FOUND_MARKER = new ExtendedItem(); // TODO: get rid of

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

    List<ExtendedItem> items;
    try {
      items = getExtendedItems(paths);
    }
    catch (TfsException e) {
      throw new VcsException(e);
    }

    for (int i = 0; i < items.size(); i++) {
      processFile(paths.get(i), items.get(i), builder);
    }
  }

  private static void processFile(@NotNull String path, @Nullable ExtendedItem item, ChangelistBuilder builder) {
    if (WORKSPACE_NOT_FOUND_MARKER.equals(item)) {
      // ignore
      return;
    }
    VirtualFile virtualFile = VcsUtil.getVirtualFile(path);
    if (virtualFile == null || virtualFile.getParent() == null) {
      // TODO: IDEA crashes if reported virtual file has no parent
      return;
    }

    final FilePath filePath = VcsUtil.getFilePath(path);
    if (item == null
          || item.getDid() != Integer.MIN_VALUE) {  // TODO: process items deleted on server
      builder.processUnversionedFile(virtualFile);
      return;
    }
    else if (isUnversioned(item)) {
      builder.processUnversionedFile(virtualFile);
    }

    if (!virtualFile.exists()) {
      builder.processLocallyDeletedFile(filePath);
      return;
    }

    if (isChanged(item)) {
      TFSContentRevision revision = TFSContentRevisionFactory.getRevision(filePath, item.getLatest());
      builder.processChange(new Change(revision, new CurrentContentRevision(filePath)));
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
        TFSVcs.assertTrue(item.getLatest() == Integer.MIN_VALUE);
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
      //TFSVcs.assertTrue(changeType.contains(ChangeType.Value.Edit));
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
    if (changeType.containsOnly(ChangeType.Value.Edit)) {
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
      return virtualFile.isWritable()
             && item.getType() != ItemType.Folder; // TODO: it seems that folders must never be treated as hijacked
    }
    return false;
  }


  private static List<ExtendedItem> getExtendedItems(List<String> fileNames) throws TfsException {
    Map<String, WorkspaceInfo> path2workspace = new HashMap<String, WorkspaceInfo>();
    Map<WorkspaceInfo, List<String>> workspace2paths = new HashMap<WorkspaceInfo, List<String>>();
    Map<WorkspaceInfo, List<ExtendedItem>> workspace2items = new HashMap<WorkspaceInfo, List<ExtendedItem>>();
    Map<String, Integer> path2index = new HashMap<String, Integer>();
    // group paths by workspace
    for (String fileName : fileNames) {
      WorkspaceInfo workspaceInfo = Workstation.getInstance().findWorkspace(fileName);
      path2workspace.put(fileName, workspaceInfo);
      if (workspaceInfo != null) {
        List<String> workspacePaths = workspace2paths.get(workspaceInfo);
        if (workspacePaths == null) {
          workspacePaths = new ArrayList<String>();
          workspace2paths.put(workspaceInfo, workspacePaths);
        }
        workspacePaths.add(workspaceInfo.findServerPathByLocalPath(fileName));
        int index = workspacePaths.size() - 1;
        path2index.put(fileName, index);
      }
    }
    // make queries
    for (WorkspaceInfo workspaceInfo : workspace2paths.keySet()) {
      workspace2items.put(workspaceInfo, workspaceInfo.getExtendedItems(workspace2paths.get(workspaceInfo)));
    }
    // merge results
    List<ExtendedItem> result = new LinkedList<ExtendedItem>();
    for (String fileName : fileNames) {
      WorkspaceInfo workspaceInfo = path2workspace.get(fileName);
      if (workspaceInfo == null) {
        result.add(WORKSPACE_NOT_FOUND_MARKER);
      }
      else {
        result.add(workspace2items.get(workspaceInfo).get(path2index.get(fileName)));
      }
    }
    return result;
  }

  public boolean isModifiedDocumentTrackingRequired() {
    return true;
  }


}
