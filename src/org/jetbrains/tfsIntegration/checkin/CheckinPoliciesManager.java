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

import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSVcs;

import java.util.HashSet;
import java.util.Set;

public class CheckinPoliciesManager {

  private static PolicyBase[] ourInstalledPolicies;

  public static final PolicyBase DUMMY_POLICY = new PolicyBase() {
    final PolicyType DUMMY = new PolicyType("DUMMY_POLICY", "", "", "");

    @NotNull
    @Override
    public PolicyType getPolicyType() {
      return DUMMY;
    }

    @Override
    public PolicyFailure[] evaluate(@NotNull PolicyContext policycontext, @NotNull ProgressIndicator progressIndicator) {
      return new PolicyFailure[0];
    }

    public boolean canEdit() {
      return false;
    }

    public boolean edit(Project project) {
      return false;
    }

    public void loadState(@NotNull Element element) {
    }

    public void saveState(@NotNull Element element) {
    }
  };

  public static PolicyBase[] getInstalledPolicies() throws DuplicatePolicyIdException {
    if (ourInstalledPolicies == null) {
      final PolicyBase[] installedPolicies = Extensions.getExtensions(PolicyBase.EP_NAME);

      Set<PolicyType> types = new HashSet<PolicyType>(installedPolicies.length);
      for (PolicyBase policy : installedPolicies) {
        if (!types.add(policy.getPolicyType())) {
          TFSVcs.LOG.warn("Duplicate check in policy type: " + policy.getPolicyType().getId());
          throw new DuplicatePolicyIdException(policy.getPolicyType().getId());
        }
      }
      ourInstalledPolicies = installedPolicies;
    }

    return ourInstalledPolicies;
  }

  @Nullable
  public static PolicyBase find(PolicyType type) throws DuplicatePolicyIdException {
    PolicyBase result = null;
    for (PolicyBase p : getInstalledPolicies()) {
      if (p.getPolicyType().equals(type)) {
        result = p;
        break;
      }
    }
    return result;
  }

}
