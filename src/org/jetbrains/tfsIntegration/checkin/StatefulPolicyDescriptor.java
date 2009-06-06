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

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class StatefulPolicyDescriptor extends PolicyDescriptor {

  public static final String DEFAULT_PRIORITY = "0";

  @NotNull private Element myConfiguration;

  private final List<String> myScope;
  @NotNull private final String myPriority;
  @Nullable private final String myLongDescription;

  public StatefulPolicyDescriptor(@NotNull PolicyType type,
                                  boolean enabled,
                                  @NotNull Element configuration,
                                  List<String> scope,
                                  @NotNull String priority,
                                  @Nullable String longDescription) {
    super(type, enabled);
    myConfiguration = configuration;
    myScope = scope;
    myPriority = priority;
    myLongDescription = longDescription;
  }

  @NotNull
  public Element getConfiguration() {
    return myConfiguration;
  }

  public List<String> getScope() {
    return myScope;
  }

  @NotNull
  public String getPriority() {
    return myPriority;
  }

  @NotNull
  public String getLongDescription() {
    return myLongDescription != null ? myLongDescription : PolicyType.DEFAULT_DESCRIPTION;
  }

  public void setConfiguration(@NotNull Element configuration) {
    myConfiguration = configuration;
  }
}
