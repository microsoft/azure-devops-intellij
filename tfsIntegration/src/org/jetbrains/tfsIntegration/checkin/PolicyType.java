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
import org.jetbrains.annotations.Nullable;

/**
 * Lightweight checkin policy definition that is stored server-side
 */
public final class PolicyType {

  public static final String DEFAULT_INSTALLATION_INSTRUCTIONS = "(No installation instructions provided)";
  public static final String DEFAULT_DESCRIPTION = "(No description)";

  @NotNull private final String myId;
  @NotNull private final String myName;
  @Nullable private final String myDescription;
  @Nullable private final String myInstallationInstructions;

  /**
   * @param id                       unique identifier of checkin policy. ID is used to search for installed policy implementation
   * @param name                     displayable policy name that is presented to the user
   * @param description              displayable policy description
   * @param installationInstructions instructions that are presented to the user in case policy implementation is missing on local machine
   */
  public PolicyType(@NotNull String id, @NotNull String name, @Nullable String description, @Nullable String installationInstructions) {
    myId = id;
    myName = name;
    myDescription = description;
    myInstallationInstructions = installationInstructions;
  }

  @NotNull
  public String getId() {
    return myId;
  }

  @NotNull
  public String getName() {
    return myName;
  }

  @NotNull
  public String getDescription() {
    return myDescription != null ? myDescription : DEFAULT_DESCRIPTION;
  }

  @NotNull
  public String getInstallationInstructions() {
    return myInstallationInstructions != null ? myInstallationInstructions : DEFAULT_INSTALLATION_INSTRUCTIONS;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PolicyType that = (PolicyType)o;

    if (!myId.equals(that.myId)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myId.hashCode();
  }


}
