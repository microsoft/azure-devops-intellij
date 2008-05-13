package org.jetbrains.tfsIntegration.core.tfs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;

public abstract class ServerStatus {

  protected final @Nullable ExtendedItem myExtendedItem;
  
  protected ServerStatus(final @Nullable ExtendedItem extendedItem) {
    myExtendedItem = extendedItem;
  }

  public abstract void visitBy(final @NotNull ItemPath path, final @NotNull StatusVisitor statusVisitor, boolean localItemExists) throws TfsException;

  public String toString() {
    return getClass().getName().substring(getClass().getName().lastIndexOf("$") + 1);
  }

  public static class Unversioned extends ServerStatus {
    protected Unversioned(final @Nullable ExtendedItem extendedItem) {
      super(extendedItem);
    }

    public void visitBy(final @NotNull ItemPath path, final @NotNull StatusVisitor statusVisitor, boolean localItemExists) throws TfsException {
      statusVisitor.unversioned(path, myExtendedItem,  localItemExists);
    }
  }

  public static class Deleted extends ServerStatus {
    public Deleted(final @NotNull ExtendedItem extendedItem) {
      super(extendedItem);
    }

    public void visitBy(final @NotNull ItemPath path, final @NotNull StatusVisitor statusVisitor, boolean localItemExists) throws TfsException {
      statusVisitor.deleted(path, myExtendedItem, localItemExists);
    }
  }

  public static class CheckedOutForEdit extends ServerStatus {
    public CheckedOutForEdit(final @NotNull ExtendedItem extendedItem) {
      super(extendedItem);
    }

    public void visitBy(final @NotNull ItemPath path, final @NotNull StatusVisitor statusVisitor, boolean localItemExists) throws TfsException {
      statusVisitor.checkedOutForEdit(path, myExtendedItem, localItemExists);
    }
  }

  public static class ScheduledForAddition extends ServerStatus {
    public ScheduledForAddition(final @NotNull ExtendedItem extendedItem) {
      super(extendedItem);
    }

    public void visitBy(final @NotNull ItemPath path, final @NotNull StatusVisitor statusVisitor, boolean localItemExists) throws TfsException {
      statusVisitor.scheduledForAddition(path, myExtendedItem, localItemExists);
    }
  }

  public static class ScheduledForDeletion extends ServerStatus {
    public ScheduledForDeletion(final @NotNull ExtendedItem extendedItem) {
      super(extendedItem);
    }

    public void visitBy(final @NotNull ItemPath path, final @NotNull StatusVisitor statusVisitor, boolean localItemExists) throws TfsException {
      statusVisitor.scheduledForDeletion(path, myExtendedItem, localItemExists);
    }
  }

  public static class OutOfDate extends ServerStatus {
    public OutOfDate(final @NotNull ExtendedItem extendedItem) {
      super(extendedItem);
    }

    public void visitBy(final @NotNull ItemPath path, final @NotNull StatusVisitor visitor, boolean localItemExists) throws TfsException {
      visitor.outOfDate(path, myExtendedItem, localItemExists);
    }
  }

  public static class UpToDate extends ServerStatus {
    public UpToDate(final @NotNull ExtendedItem extendedItem) {
      super(extendedItem);
    }

    public void visitBy(final @NotNull ItemPath path, final @NotNull StatusVisitor statusVisitor, boolean localItemExists) throws TfsException {
      statusVisitor.upToDate(path, myExtendedItem, localItemExists);
    }
  }

  public static class Renamed extends ServerStatus {
    public Renamed(final @NotNull ExtendedItem extendedItem) {
      super(extendedItem);
    }

    public void visitBy(final @NotNull ItemPath path, final @NotNull StatusVisitor statusVisitor, boolean localItemExists) throws TfsException {
      statusVisitor.renamed(path, myExtendedItem, localItemExists);
    }
  }

  public static class RenamedCheckedOut extends ServerStatus {
    public RenamedCheckedOut(final @NotNull ExtendedItem extendedItem) {
      super(extendedItem);
    }

    public void visitBy(final @NotNull ItemPath path, final @NotNull StatusVisitor statusVisitor, boolean localItemExists) throws TfsException {
      statusVisitor.renamedCheckedOut(path, myExtendedItem, localItemExists);
    }
  }

}
