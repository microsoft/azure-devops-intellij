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

package org.jetbrains.tfsIntegration.ui.deferredtree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface ContentProvider<T> {

  /**
   * @param item can be null for tree root
   */
  boolean canHaveChildren(final @Nullable T item);

  /**
   * @param item can be null for tree root, collection of one or zero items should be returned in this case
   */
  Collection<T> getChildren(final @Nullable T item) throws Exception;

  boolean equals(final @NotNull T item1, final @NotNull T item2);

  int getHashCode(final @NotNull T item);
}
