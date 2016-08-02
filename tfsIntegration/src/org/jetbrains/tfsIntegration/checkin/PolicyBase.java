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

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import org.jdom.Element;

import java.text.MessageFormat;

/**
 * This is a base class for checkin policy implementation
 */
public abstract class PolicyBase {

  public static final ExtensionPointName<PolicyBase> EP_NAME = new ExtensionPointName<PolicyBase>("TFS.checkinPolicy");

  protected static final PolicyFailure[] NO_FAILURES = new PolicyFailure[0];

  /**
   * @return type of the policy
   */
  @NotNull
  public abstract PolicyType getPolicyType();

  /**
   * Evaluate current checkin provided as {@link PolicyContext} object
   *
   * <p>This method is called in background thread, so user interaction should be made using {@link javax.swing.SwingUtilities#invokeAndWait(Runnable)}.
   * If evaluation is long running it is recommended to call {@link com.intellij.openapi.progress.ProgressIndicator#checkCanceled()}
   * to give user a chance to cancel. Prior to evaluation {@link #loadState(org.jdom.Element)} is called. Runtime exception thrown out of this method
   * will be reported as evaluation error.</p>
   *
   * @param policycontext configuration of checkin to evaluate
   * @param progressIndicator progress indicator
   * @return collection is evaluation errors or empty array if checkin is OK to be done
   */
  public abstract PolicyFailure[] evaluate(@NotNull PolicyContext policycontext, @NotNull ProgressIndicator progressIndicator);

  /**
   * Check if current policy supports configuring with UI. If yes, Edit button for the policy will be enabled in Edit Chec In Policies dialog
   *
   * @return <code>true</code> if current policy configuration can be changed
   */
  public abstract boolean canEdit();

  /**
   * Open edit dialog for the policy
   *
   * <p>Called in UI thread if {@link #canEdit()} returned <code>true</code>. {@link #loadState(org.jdom.Element)} is called in before the call.
   * If method returns <code>true</code>, {@link #saveState(org.jdom.Element)} is called afterwards
   *
   * @param project current project
   * @return <code>true</code> if edit was done successfully and policy state changed
   */
  public abstract boolean edit(Project project);

  /**
   * Load policy configuration state from {@link org.jdom.Element} node. Runtime exception thrown out of this method
   * will be reported as loading error.
   * @param element root element of the configuration XML store
   */
  public abstract void loadState(@NotNull Element element);

  /**
   * Save policy configuration state to {@link org.jdom.Element} node. Runtime exception thrown out of this method
   * will be reported as saving error.
   * @param element root element of the configuration XML store
   */
  public abstract void saveState(@NotNull Element element);

  /**
   * This method is invoked when user double clicks on policy failure item in Checkin Parameters dialog.
   * Default implementation shows message dialog with the problem description
   *
   * @param project       project for the current frame
   * @param policyFailure item that is activated
   */
  public void activate(@NotNull Project project, @NotNull PolicyFailure policyFailure) {
    final String message;
    if (policyFailure.getTooltipText() != null) {
      message = MessageFormat.format("{0}\n\n{1}", policyFailure.getMessage(), policyFailure.getTooltipText());
    }
    else {
      message = policyFailure.getMessage();
    }
    Messages.showWarningDialog(project, message, "Checkin Policy Warning");
  }

}
