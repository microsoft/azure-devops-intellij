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

package org.jetbrains.tfsIntegration.core.tfs.locks;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.VersionControlPath;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.TfsUtil;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ItemType;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.LockLevel;

import java.util.Comparator;

public class LockItemModel {

  private final @NotNull ExtendedItem myExtendedItem;
  private final @NotNull WorkspaceInfo myWorkspace;
  // null means that the item is locked by another user so current user can not do anything with the item
  private @Nullable Boolean mySelectionStatus;

  public LockItemModel(@NotNull final ExtendedItem item, @NotNull final WorkspaceInfo workspace) {
    myExtendedItem = item;
    myWorkspace = workspace;
    mySelectionStatus = canBeLocked() || canBeUnlocked() ? Boolean.FALSE : null;
  }

  @NotNull
  public ExtendedItem getExtendedItem() {
    return myExtendedItem;
  }

  @NotNull
  public WorkspaceInfo getWorkspace() {
    return myWorkspace;
  }

  @Nullable
  public Boolean getSelectionStatus() {
    return mySelectionStatus;
  }

  /**
   * @throws IllegalArgumentException if this item is locked by another user.
   */
  public void setSelectionStatus(final @NotNull Boolean selectionStatus) {
    if (mySelectionStatus == null) {
      throw new IllegalArgumentException("State of items locked by another user cannot be changed.");
    }
    mySelectionStatus = selectionStatus;
  }

  @Nullable
  public String getLockOwner() {
    return myExtendedItem.getLowner() != null ? TfsUtil.getNameWithoutDomain(myExtendedItem.getLowner()) : null;
  }

  public static final Comparator<LockItemModel> LOCK_ITEM_PARENT_FIRST = new Comparator<LockItemModel>() {
    public int compare(final LockItemModel o1, final LockItemModel o2) {
      return VersionControlPath.compareParentToChild(o1.getExtendedItem().getSitem(), o1.getExtendedItem().getType() == ItemType.Folder,
                                                     o2.getExtendedItem().getSitem(), o2.getExtendedItem().getType() == ItemType.Folder);
    }
  };

  public boolean canBeLocked() {
    return myExtendedItem.getLock() == null || myExtendedItem.getLock() == LockLevel.None;
  }

  public boolean canBeUnlocked() {
    return myWorkspace.getOwnerName().equalsIgnoreCase(myExtendedItem.getLowner());
  }
}
