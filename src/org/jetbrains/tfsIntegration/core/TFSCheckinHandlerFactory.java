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

package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitExecutor;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;
import com.intellij.util.PairConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.checkin.CheckinParameters;
import org.jetbrains.tfsIntegration.checkin.CheckinPoliciesManager;
import org.jetbrains.tfsIntegration.checkin.DuplicatePolicyIdException;
import org.jetbrains.tfsIntegration.ui.OverridePolicyWarningsDialog;

import java.text.MessageFormat;

public class TFSCheckinHandlerFactory extends CheckinHandlerFactory {
  @NotNull
  public CheckinHandler createHandler(final CheckinProjectPanel panel) {
    return new CheckinHandler() {
      @Override
      public ReturnResult beforeCheckin(@Nullable CommitExecutor executor, PairConsumer<Object, Object> additionalDataConsumer) {
        if (executor != null) {
          return ReturnResult.COMMIT;
        }
        
        final TFSVcs tfsVcs = TFSVcs.getInstance(panel.getProject());
        if (!panel.getAffectedVcses().contains(tfsVcs)) {
          return ReturnResult.COMMIT;
        }

        final CheckinParameters parameters = tfsVcs.getCheckinEnvironment().getCheckinParameters();
        if (parameters == null) {
          Messages.showErrorDialog(panel.getProject(), "Validation must be performed before checking in", "Checkin");
          return ReturnResult.CLOSE_WINDOW;
        }

        @Nullable Pair<String, CheckinParameters.Severity> msg = parameters.getValidationMessage(CheckinParameters.Severity.ERROR);
        if (msg != null) {
          Messages.showErrorDialog(panel.getProject(), msg.first, "Checkin: Validation Failed");
          return ReturnResult.CANCEL;
        }

        try {
          CheckinPoliciesManager.getInstalledPolicies();
        }
        catch (DuplicatePolicyIdException e) {
          String message = MessageFormat
            .format("Found multiple checkin policies with the same id: ''{0}''.\nPlease review your extensions.", e.getDuplicateId());
          Messages.showErrorDialog(panel.getProject(), message, "Checkin Policies Evaluation");
          return ReturnResult.CLOSE_WINDOW;
        }

        // need to evaluate policies again since comment and checkboxes state may change since last validation
        // remove when commit dialog state change listener is provided
        boolean completed = ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
          public void run() {
            final ProgressIndicator pi = ProgressManager.getInstance().getProgressIndicator();
            pi.setIndeterminate(true);
            parameters.evaluatePolicies(pi);
          }
        }, "Evaluating Checkin Policies", true, panel.getProject());
        if (!completed) {
          tfsVcs.getCheckinEnvironment().updateMessage();
          return ReturnResult.CANCEL;
        }

        msg = parameters.getValidationMessage(CheckinParameters.Severity.WARNING);
        if (msg == null) {
          return ReturnResult.COMMIT;
        }

        OverridePolicyWarningsDialog d = new OverridePolicyWarningsDialog(panel.getProject(), parameters.getAllFailures());
        d.show();
        if (d.isOK()) {
          parameters.setOverrideReason(d.getReason());
          return CheckinHandler.ReturnResult.COMMIT;
        }
        else {
          return ReturnResult.CANCEL;
        }
      }
    };
  }

}
