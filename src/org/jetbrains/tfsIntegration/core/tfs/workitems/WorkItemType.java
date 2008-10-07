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

import org.jetbrains.annotations.NonNls;

public enum WorkItemType {

  Bug("Bug"), QualityOfServiceRequirement("Quality of Service Requirement"), Risk("Risk"), Scenario("Scenario"), Task("Task");

  private WorkItemType(@NonNls String serialized) {
    mySerialized = serialized;
  }

  private @NonNls final String mySerialized;

  public String getSerialized() {
    return mySerialized;
  }

  public static WorkItemType fromString(String s) {
    for (WorkItemType type : values()) {
      if (type.getSerialized().equals(s)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown work item type serialized form: " + s);
  }
}
