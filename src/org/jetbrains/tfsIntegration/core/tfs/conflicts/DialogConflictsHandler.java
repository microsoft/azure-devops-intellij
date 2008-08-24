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
package org.jetbrains.tfsIntegration.core.tfs.conflicts;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Ref;
import org.jetbrains.tfsIntegration.core.tfs.TfsFileUtil;
import org.jetbrains.tfsIntegration.ui.ResolveConflictsDialog;

public class DialogConflictsHandler implements ConflictsHandler {
  public boolean resolveConflicts(final ResolveConflictHelper resolveConflictHelper) {
    final Ref<Integer> exitCode = new Ref<Integer>(DialogWrapper.CANCEL_EXIT_CODE);
    Runnable runnable = new Runnable() {
      public void run() {
          ResolveConflictsDialog dialog = new ResolveConflictsDialog(resolveConflictHelper);
          dialog.show();
          exitCode.set(dialog.getExitCode());
      }
    };
    TfsFileUtil.executeInEventDispatchThread(runnable);
    return exitCode.get() == DialogWrapper.OK_EXIT_CODE;
  }
}
