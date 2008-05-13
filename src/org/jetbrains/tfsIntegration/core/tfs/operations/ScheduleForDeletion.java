package org.jetbrains.tfsIntegration.core.tfs.operations;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.DeletedState;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.GetOperation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ScheduleForDeletion {

  public static Collection<VcsException> execute(Project project, WorkspaceInfo workspace, List<ItemPath> paths) {
    Collection<VcsException> errors = new ArrayList<VcsException>();

    try {
      Map<ItemPath, ExtendedItem> local2serverItems =
        workspace.getServer().getVCS().getExtendedItems(workspace.getName(), workspace.getOwnerName(), paths, DeletedState.NonDeleted);

      ScheduleForDeletionStatusVisitor visitor = new ScheduleForDeletionStatusVisitor();
      for (Map.Entry<ItemPath, ExtendedItem> e : local2serverItems.entrySet()) {
        ServerStatus serverStatus = StatusProvider.determineServerStatus(e.getValue());
        serverStatus.visitBy(e.getKey(), visitor, true);
      }

      Collection<ItemPath> undoPendingChanges = new ArrayList<ItemPath>(visitor.getUndoPendingChangesAndSchedule());
      undoPendingChanges.addAll(visitor.getUndoPendingChangesOnly());

      final UndoPendingChanges.UndoPendingChangesResult undoResult = UndoPendingChanges.execute(workspace, undoPendingChanges, false);
      errors.addAll(undoResult.errors);

      Collection<ItemPath> scheduleForDeletion = visitor.getScheduleForDeletion();
      for (ItemPath undonePath : undoResult.undonePaths) {
        if (!visitor.getUndoPendingChangesOnly().contains(undonePath)) {
          scheduleForDeletion.add(undonePath);
        }
      }

      ResultWithFailures<GetOperation> schedulingForDeletionResults =
        workspace.getServer().getVCS().scheduleForDeletion(workspace.getName(), workspace.getOwnerName(), scheduleForDeletion);

      for (GetOperation getOp : schedulingForDeletionResults.getResult()) {
        String localPath = getOp.getSlocal();
        TfsFileUtil.invalidateFile(project, VcsUtil.getFilePath(localPath));
        VirtualFile file = VcsUtil.getVirtualFile(localPath);
        if (file != null && file.isValid() && !file.isDirectory()) {
          TfsFileUtil.setReadOnlyInEventDispathThread(file, false);
        }
      }
      errors.addAll(BeanHelper.getVcsExceptions(schedulingForDeletionResults.getFailures()));
    }
    catch (TfsException e) {
      errors.add(new VcsException(e));
    }

    return errors;
  }

  private static class ScheduleForDeletionStatusVisitor implements StatusVisitor {
    
    private Collection<ItemPath> myUndoPendingChangesOnly = new ArrayList<ItemPath>();
    private Collection<ItemPath> myUndoPendingChangesAndSchedule = new ArrayList<ItemPath>();
    private Collection<ItemPath> myScheduleForDeletion = new ArrayList<ItemPath>();

    public void scheduledForAddition(@NotNull final ItemPath path,
                                     final @NotNull ExtendedItem extendedItem,
                                     final boolean localItemExists) {
      myUndoPendingChangesOnly.add(path);
    }

    public void unversioned(@NotNull final ItemPath path, final @Nullable ExtendedItem extendedItem, final boolean localItemExists) {
      // ignore
    }

    public void checkedOutForEdit(@NotNull final ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists) {
      myUndoPendingChangesAndSchedule.add(path);
    }

    public void scheduledForDeletion(@NotNull final ItemPath path,
                                     final @NotNull ExtendedItem extendedItem,
                                     final boolean localItemExists) {
      // ignore
    }

    public void outOfDate(@NotNull final ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists) {
      myScheduleForDeletion.add(path);
    }

    public void deleted(@NotNull final ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists) {
      // ignore
    }

    public void upToDate(@NotNull final ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists) {
      myScheduleForDeletion.add(path);
    }

    public void renamed(@NotNull final ItemPath path, @NotNull final ExtendedItem extendedItem, final boolean localItemExists)
      throws TfsException {
      myUndoPendingChangesAndSchedule.add(path);
    }

    public void renamedCheckedOut(@NotNull final ItemPath path, @NotNull final ExtendedItem extendedItem, final boolean localItemExists)
      throws TfsException {
      myUndoPendingChangesAndSchedule.add(path);
    }

    public Collection<ItemPath> getUndoPendingChangesOnly() {
      return myUndoPendingChangesOnly;
    }

    public Collection<ItemPath> getUndoPendingChangesAndSchedule() {
      return myUndoPendingChangesAndSchedule;
    }

    public Collection<ItemPath> getScheduleForDeletion() {
      return myScheduleForDeletion;
    }
  }

}
