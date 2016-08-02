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

package org.jetbrains.tfsIntegration.core.tfs.workitems;

import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class WorkItemType {

  private static final Map<String, WorkItemType> ourAllTypes = ContainerUtil.newHashMap();

  public static final WorkItemType BUG = register("Bug");
  public static final WorkItemType TASK = register("Task");

  @NonNls @NotNull private final String myName;

  private WorkItemType(@NonNls @NotNull String name) {
    myName = name;
  }

  @NotNull
  public String getName() {
    return myName;
  }

  @NotNull
  public static WorkItemType from(@NotNull String typeName) {
    return register(typeName);
  }

  @NotNull
  private static synchronized WorkItemType register(@NotNull String typeName) {
    WorkItemType result = ourAllTypes.get(typeName);

    if (result == null) {
      result = new WorkItemType(typeName);
      ourAllTypes.put(typeName, result);
    }

    return result;
  }

  @Override
  public String toString() {
    return myName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof WorkItemType)) return false;

    WorkItemType type = (WorkItemType)o;

    return myName.equals(type.myName);
  }

  @Override
  public int hashCode() {
    return myName.hashCode();
  }
}
