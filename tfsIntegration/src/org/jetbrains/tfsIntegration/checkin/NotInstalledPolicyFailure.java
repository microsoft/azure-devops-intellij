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

import java.text.MessageFormat;

public class NotInstalledPolicyFailure extends PolicyFailure {
  private final PolicyType myPolicyType;

  public NotInstalledPolicyFailure(@NotNull PolicyType policyType, boolean reportId) {
    super(CheckinPoliciesManager.DUMMY_POLICY, getMessage(policyType), getTooltip(policyType, reportId));
    myPolicyType = policyType;
  }

  private static String getMessage(PolicyType type) {
    return MessageFormat.format("Checkin policy ''{0}'' is not installed", type.getName());
  }

  private static String getTooltip(PolicyType type, boolean reportId) {
    if (reportId) {
      return MessageFormat.format("Policy id: {0}\n{1}", type.getId(), type.getInstallationInstructions());
    }
    else {
      return type.getInstallationInstructions();
    }
  }

  @Override
  public String getPolicyName() {
    return myPolicyType.getName();
  }
}
