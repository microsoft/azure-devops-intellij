package org.jetbrains.tfsIntegration.core.tfs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;

public interface StatusVisitor {

  void unversioned(final @NotNull ItemPath path, final @Nullable ExtendedItem extendedItem, final boolean localItemExists) throws TfsException;

  void checkedOutForEdit(final @NotNull ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists);

  void scheduledForAddition(final @NotNull ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists);

  void scheduledForDeletion(final @NotNull ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists);

  void outOfDate(final @NotNull ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists) throws TfsException;

  void deleted(final @NotNull ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists);

  void upToDate(final @NotNull ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists) throws TfsException;

  void renamed(final @NotNull ItemPath path, final ExtendedItem extendedItem, final boolean localItemExists) throws TfsException;
}
