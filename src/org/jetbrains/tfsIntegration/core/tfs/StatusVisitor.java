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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;

public interface StatusVisitor {

  void unversioned(final @NotNull ItemPath path, final @Nullable ExtendedItem extendedItem, final boolean localItemExists) throws TfsException;

  void checkedOutForEdit(final @NotNull ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists) throws
                                                                                                                                TfsException;

  void scheduledForAddition(final @NotNull ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists);

  void scheduledForDeletion(final @NotNull ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists);

  void outOfDate(final @NotNull ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists) throws TfsException;

  void deleted(final @NotNull ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists);

  void upToDate(final @NotNull ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists) throws TfsException;

  void renamed(final @NotNull ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists) throws TfsException;

  void renamedCheckedOut(final ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists) throws TfsException;
}
