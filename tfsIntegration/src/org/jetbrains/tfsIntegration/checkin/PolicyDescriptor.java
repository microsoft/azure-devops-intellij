/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.jetbrains.tfsIntegration.checkin;

import org.jetbrains.annotations.NotNull;

public class PolicyDescriptor {

  private boolean myEnabled;

  @NotNull private final PolicyType myType;

  public PolicyDescriptor(@NotNull PolicyType type, boolean enabled) {
    myEnabled = enabled;
    myType = type;
  }

  public boolean isEnabled() {
    return myEnabled;
  }

  public void setEnabled(Boolean value) {
    myEnabled = value;
  }

  @NotNull
  public PolicyType getType() {
    return myType;
  }

}