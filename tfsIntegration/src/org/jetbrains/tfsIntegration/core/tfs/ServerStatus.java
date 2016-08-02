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

package org.jetbrains.tfsIntegration.core.tfs;

import com.intellij.openapi.vcs.FilePath;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.ExtendedItem;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.ItemType;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.PendingChange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.exceptions.TfsException;

public abstract class ServerStatus {
  public final int localVer;
  public final int itemId;
  public final boolean isDirectory;
  public @Nullable final String sourceItem;
  public @Nullable final String targetItem;

  protected ServerStatus(final int localVer,
                         final int itemId,
                         final boolean isDirectory,
                         final String sourceItem,
                         final String targetItem) {
    this.localVer = localVer;
    this.itemId = itemId;
    this.isDirectory = isDirectory;
    this.sourceItem = sourceItem;
    this.targetItem = targetItem;
  }

  protected ServerStatus(final @NotNull PendingChange pendingChange) {
    this(pendingChange.getVer(), pendingChange.getItemid(), pendingChange.getType() == ItemType.Folder, pendingChange.getSrcitem(),
         pendingChange.getItem());
  }

  protected ServerStatus(final @NotNull ExtendedItem extendedItem) {
    this(extendedItem.getLver(), extendedItem.getItemid(), extendedItem.getType() == ItemType.Folder, extendedItem.getSitem(),
         extendedItem.getTitem());
  }

  public abstract void visitBy(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull StatusVisitor statusVisitor)
    throws TfsException;

  public String toString() {
    return getClass().getName().substring(getClass().getName().lastIndexOf("$") + 1);
  }

  public static class Unversioned extends ServerStatus {

    public static final ServerStatus INSTANCE = new Unversioned();

    private Unversioned() {
      super(0, 0, false, null, null);
    }

    public void visitBy(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull StatusVisitor statusVisitor)
      throws TfsException {
      statusVisitor.unversioned(localPath, localItemExists, this);
    }
  }

  /*public static class Deleted extends ServerStatus {
    protected Deleted(final @Nullable ExtendedItem item) {
      super(item);
    }

    public void visitBy(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull StatusVisitor statusVisitor)
      throws TfsException {
      statusVisitor.deleted(localPath, localItemExists, this);
    }
  }*/

  public static class CheckedOutForEdit extends ServerStatus {
    protected CheckedOutForEdit(final @NotNull PendingChange pendingChange) {
      super(pendingChange);
    }

    public CheckedOutForEdit(final @NotNull ExtendedItem item) {
      super(item);
    }

    public void visitBy(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull StatusVisitor statusVisitor)
      throws TfsException {
      statusVisitor.checkedOutForEdit(localPath, localItemExists, this);
    }
  }

  public static class ScheduledForAddition extends ServerStatus {
    protected ScheduledForAddition(final @NotNull PendingChange pendingChange) {
      super(pendingChange);
    }

    protected ScheduledForAddition(final @NotNull ExtendedItem extendedItem) {
      super(extendedItem);
    }

    public void visitBy(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull StatusVisitor statusVisitor)
      throws TfsException {
      statusVisitor.scheduledForAddition(localPath, localItemExists, this);
    }
  }

  public static class ScheduledForDeletion extends ServerStatus {
    protected ScheduledForDeletion(final @NotNull PendingChange pendingChange) {
      super(pendingChange);
    }

    public ScheduledForDeletion(final @NotNull ExtendedItem item) {
      super(item);
    }

    public void visitBy(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull StatusVisitor statusVisitor)
      throws TfsException {
      statusVisitor.scheduledForDeletion(localPath, localItemExists, this);
    }
  }

  public static class OutOfDate extends ServerStatus {
    protected OutOfDate(final @NotNull ExtendedItem extendedItem) {
      super(extendedItem);
    }

    public void visitBy(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull StatusVisitor statusVisitor)
      throws TfsException {
      statusVisitor.outOfDate(localPath, localItemExists, this);
    }
  }

  public static class UpToDate extends ServerStatus {
    protected UpToDate(final @NotNull ExtendedItem extendedItem) {
      super(extendedItem);
    }

    public void visitBy(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull StatusVisitor statusVisitor)
      throws TfsException {
      statusVisitor.upToDate(localPath, localItemExists, this);
    }
  }

  public static class Renamed extends ServerStatus {
    protected Renamed(final @NotNull PendingChange pendingChange) {
      super(pendingChange);
    }

    public Renamed(final @NotNull ExtendedItem item) {
      super(item);
    }

    public void visitBy(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull StatusVisitor statusVisitor)
      throws TfsException {
      statusVisitor.renamed(localPath, localItemExists, this);
    }
  }

  public static class RenamedCheckedOut extends ServerStatus {
    protected RenamedCheckedOut(final @NotNull PendingChange pendingChange) {
      super(pendingChange);
    }

    public RenamedCheckedOut(final @NotNull ExtendedItem item) {
      super(item);
    }

    public void visitBy(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull StatusVisitor statusVisitor)
      throws TfsException {
      statusVisitor.renamedCheckedOut(localPath, localItemExists, this);
    }
  }

  public static class Undeleted extends ServerStatus {
    protected Undeleted(final @NotNull PendingChange pendingChange) {
      super(pendingChange);
    }

    public Undeleted(final @NotNull ExtendedItem item) {
      super(item);
    }

    public void visitBy(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull StatusVisitor statusVisitor)
      throws TfsException {
      statusVisitor.undeleted(localPath, localItemExists, this);
    }
  }

}
