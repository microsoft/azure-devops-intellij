package org.jetbrains.tfsIntegration.core.tfs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;

public abstract class ServerStatus {

  public abstract void visitBy(final @NotNull ItemPath path, final @NotNull StatusVisitor statusVisitor, boolean localItemExists) throws TfsException;

  public String toString() {
    return getClass().getName().substring(getClass().getName().lastIndexOf("$") + 1);
  }


  abstract static class StatusWithExtendedItem extends ServerStatus {
    protected final @NotNull ExtendedItem myExtendedItem;

    protected StatusWithExtendedItem(final @NotNull ExtendedItem extendedItem) {
      myExtendedItem = extendedItem;
    }
  }

  public static class Unversioned extends ServerStatus {
    public void visitBy(final @NotNull ItemPath path, final @NotNull StatusVisitor statusVisitor, boolean localItemExists) throws TfsException {
      statusVisitor.unversioned(path, localItemExists);
    }
  }

  public static class Deleted extends StatusWithExtendedItem {
    public Deleted(final @NotNull ExtendedItem extendedItem) {
      super(extendedItem);
    }

    public void visitBy(final @NotNull ItemPath path, final @NotNull StatusVisitor statusVisitor, boolean localItemExists) throws TfsException {
      statusVisitor.deleted(path, myExtendedItem, localItemExists);
    }
  }

  public static class NotDownloaded extends StatusWithExtendedItem {
    public NotDownloaded(final @NotNull ExtendedItem extendedItem) {
      super(extendedItem);
    }

    public void visitBy(final @NotNull ItemPath path, final @NotNull StatusVisitor statusVisitor, boolean localItemExists) throws TfsException {
      statusVisitor.notDownloaded(path, myExtendedItem, localItemExists);
    }
  }

  public static class CheckedOutForEdit extends StatusWithExtendedItem {
    public CheckedOutForEdit(final @NotNull ExtendedItem extendedItem) {
      super(extendedItem);
    }

    public void visitBy(final @NotNull ItemPath path, final @NotNull StatusVisitor statusVisitor, boolean localItemExists) throws TfsException {
      statusVisitor.checkedOutForEdit(path, myExtendedItem, localItemExists);
    }
  }

  public static class ScheduledForAddition extends StatusWithExtendedItem {
    public ScheduledForAddition(final @NotNull ExtendedItem extendedItem) {
      super(extendedItem);
    }

    public void visitBy(final @NotNull ItemPath path, final @NotNull StatusVisitor statusVisitor, boolean localItemExists) throws TfsException {
      statusVisitor.scheduledForAddition(path, myExtendedItem, localItemExists);
    }
  }

  public static class ScheduledForDeletion extends StatusWithExtendedItem {
    public ScheduledForDeletion(final @NotNull ExtendedItem extendedItem) {
      super(extendedItem);
    }

    public void visitBy(final @NotNull ItemPath path, final @NotNull StatusVisitor statusVisitor, boolean localItemExists) throws TfsException {
      statusVisitor.scheduledForDeletion(path, myExtendedItem, localItemExists);
    }
  }

  public static class OutOfDate extends StatusWithExtendedItem {
    public OutOfDate(final @NotNull ExtendedItem extendedItem) {
      super(extendedItem);
    }

    public void visitBy(final @NotNull ItemPath path, final @NotNull StatusVisitor visitor, boolean localItemExists) throws TfsException {
      visitor.outOfDate(path, myExtendedItem, localItemExists);
    }
  }

  public static class UpToDate extends StatusWithExtendedItem {
    public UpToDate(final @NotNull ExtendedItem extendedItem) {
      super(extendedItem);
    }

    public void visitBy(final @NotNull ItemPath path, final @NotNull StatusVisitor statusVisitor, boolean localItemExists) throws TfsException {
      statusVisitor.upToDate(path, myExtendedItem, localItemExists);
    }
  }
}
