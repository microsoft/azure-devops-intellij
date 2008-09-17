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

import java.util.Collection;
import java.util.Collections;

class NullContentProvider<T> implements ContentProvider<T> {

  public boolean canHaveChildren(T parent) {
    return false;
  }

  public Collection<T> getChildren(T parent) {
    return Collections.emptyList();
  }

  public boolean equals(@NotNull T item1, @NotNull T item2) {
    return item1 == item2;
  }

  public String getLabel(final @NotNull T item) {
    return "";
  }

  public int getHashCode(final @NotNull T item) {
    return 0;
  }
}
