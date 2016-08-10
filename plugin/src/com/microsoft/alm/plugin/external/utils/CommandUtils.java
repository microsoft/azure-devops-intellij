// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.utils;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.commands.Command;
import com.microsoft.alm.plugin.external.commands.FindWorkspaceCommand;
import com.microsoft.alm.plugin.external.commands.GetLocalPathCommand;
import com.microsoft.alm.plugin.external.commands.HistoryCommand;
import com.microsoft.alm.plugin.external.models.ChangeSet;
import com.microsoft.alm.plugin.external.models.Workspace;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * Helper for running commands
 */
public class CommandUtils {

    public static Workspace getWorkspaceSynchronously(final ServerContext context, final Project project) {
        ArgumentHelper.checkNotNull(project, "project");
        final FindWorkspaceCommand command = new FindWorkspaceCommand(context, project.getBasePath());
        return command.runSynchronously();
    }

    public static String getLocalPathSynchronously(final ServerContext context, final String serverPath, final String workspace) {
        final Command<String> getLocalPathCommand = new GetLocalPathCommand(context, serverPath, workspace);
        return getLocalPathCommand.runSynchronously();
    }

    public static List<ChangeSet> getHistoryCommand(final ServerContext context, final String localPath, final String version,
                                                    final int stopAfter, final boolean recursive, final String user) {
        final Command<List<ChangeSet>> historyCommand = new HistoryCommand(context, localPath, version, stopAfter, recursive, user);
        return historyCommand.runSynchronously();
    }

    public static ChangeSet getLastHistoryEntryForAnyUser(final ServerContext context, final String localPath) {
        final List<ChangeSet> results = getHistoryCommand(context, localPath, null, 1, false, StringUtils.EMPTY);
        return results.isEmpty() ? null : results.get(0);
    }
}
