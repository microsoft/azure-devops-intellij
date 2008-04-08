package org.jetbrains.tfsIntegration.core.tfs;

import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;
import org.jetbrains.tfsIntegration.exceptions.TfsException;

public interface StatusVisitor {

  void unversioned(final ItemPath path, final boolean localItemExists);

  void notDownloaded(final ItemPath path, final ExtendedItem extendedItem, final boolean localItemExists) throws TfsException;

  void checkedOutForEdit(final ItemPath path, final ExtendedItem extendedItem, final boolean localItemExists);

  void scheduledForAddition(final ItemPath path, final ExtendedItem extendedItem, final boolean localItemExists);

  void scheduledForDeletion(final ItemPath path, final ExtendedItem extendedItem, final boolean localItemExists);

  void outOfDate(final ItemPath path, final ExtendedItem extendedItem, final boolean localItemExists) throws TfsException;

  void deleted(final ItemPath path, final ExtendedItem extendedItem, final boolean localItemExists);

  void upToDate(final ItemPath path, final ExtendedItem extendedItem, final boolean localItemExists) throws TfsException;
}
