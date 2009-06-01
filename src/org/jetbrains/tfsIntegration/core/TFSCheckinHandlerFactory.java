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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;
import com.intellij.openapi.util.Condition;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.exceptions.TfsException;

import java.io.File;
import java.text.MessageFormat;
import java.util.*;

public class TFSCheckinHandlerFactory extends CheckinHandlerFactory {
  @NotNull
  public CheckinHandler createHandler(final CheckinProjectPanel panel) {
    return new CheckinHandler() {
      @Override
      public ReturnResult beforeCheckin() {
        final TFSVcs tfsVcs = TFSVcs.getInstance(panel.getProject());
        if (!panel.getAffectedVcses().contains(tfsVcs)) {
          return ReturnResult.COMMIT;
        }

        if (tfsVcs.getCheckinEnvironment().getValidationError() != null) {
          String message = MessageFormat.format("Validation was not performed:\n{0}", tfsVcs.getCheckinEnvironment().getValidationError());
          Messages.showErrorDialog(panel.getProject(), message, "Check In: Validation Required");
          return ReturnResult.CLOSE_WINDOW;
        }
        else {
          final Collection<ServerInfo> affectedServers = getAffectedServers(panel.getProject(), panel.getFiles());
          if (affectedServers.isEmpty()) {
            return ReturnResult.CLOSE_WINDOW;
          }

          final String errors = CheckinParameters.validate(tfsVcs.getCheckinEnvironment().getCheckinParameters(), new Condition<ServerInfo>() {
            public boolean value(ServerInfo server) {
              return affectedServers.contains(server);
            }
          });

          if (errors != null) {
            Messages.showWarningDialog(panel.getProject(), errors, "Check In: Validation Failed");
            return ReturnResult.CANCEL;
          }
          else {
            return ReturnResult.COMMIT;
          }
        }
      }
    };
  }

  // TODO until commit dialog refreshable is notified when user unchecks some items
  private static Collection<ServerInfo> getAffectedServers(Project project, Collection<File> files) {
    final Collection<FilePath> filePaths = new ArrayList<FilePath>(files.size());
    for (File file : files) {
      filePaths.add(VcsUtil.getFilePath(file));
    }

    final TfsExecutionUtil.ResultWithError<Collection<ServerInfo>> result =
      TfsExecutionUtil.executeInBackground("Validating Check In", project, new TfsExecutionUtil.Process<Collection<ServerInfo>>() {
        public Collection<ServerInfo> run() throws TfsException, VcsException {
          final Set<ServerInfo> result = new HashSet<ServerInfo>();
          WorkstationHelper.processByWorkspaces(filePaths, false, new WorkstationHelper.VoidProcessDelegate() {
            public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
              result.add(workspace.getServer());
            }
          });
          return result;
        }
      });

    if (result.cancelled) {
      Messages.showWarningDialog(project, "Cancelled by user", "Check In: Validation Failed");
      return Collections.emptyList();
    }
    else if (result.error != null) {
      String message = MessageFormat.format("Validation was not performed:\n{0}", result.error);
      Messages.showErrorDialog(project, message, "Check In: Validation Required");
      return Collections.emptyList();
    }
    return result.result;
  }
}
