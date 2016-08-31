// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.utils;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.commands.Command;
import com.microsoft.alm.plugin.external.commands.FindWorkspaceCommand;
import com.microsoft.alm.plugin.external.commands.GetLocalPathCommand;
import com.microsoft.alm.plugin.external.commands.GetWorkspaceCommand;
import com.microsoft.alm.plugin.external.commands.HistoryCommand;
import com.microsoft.alm.plugin.external.commands.SyncCommand;
import com.microsoft.alm.plugin.external.commands.UndoCommand;
import com.microsoft.alm.plugin.external.commands.UpdateWorkspaceCommand;
import com.microsoft.alm.plugin.external.commands.UpdateWorkspaceMappingCommand;
import com.microsoft.alm.plugin.external.models.ChangeSet;
import com.microsoft.alm.plugin.external.models.Workspace;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * Helper for running commands
 */
public class CommandUtils {

    /**
     * This method will return just the workspace name or empty string (never null)
     *
     * @param context
     * @param project
     * @return
     */
    public static String getWorkspaceName(final ServerContext context, final Project project) {
        ArgumentHelper.checkNotNull(project, "project");
        final FindWorkspaceCommand command = new FindWorkspaceCommand(project.getBasePath());
        final Workspace workspace = command.runSynchronously();
        if (workspace != null) {
            return workspace.getName();
        }
        return StringUtils.EMPTY;
    }

    /**
     * This method determines the workspace name from the project and then calls getWorkspace with the name.
     *
     * @param context
     * @param project
     * @return
     */
    public static Workspace getWorkspace(final ServerContext context, final Project project) {
        final String workspaceName = getWorkspaceName(context, project);
        return getWorkspace(context, workspaceName);
    }

    /**
     * This method returns the fully filled out Workspace object.
     *
     * @param context
     * @param workspaceName
     * @return
     */
    public static Workspace getWorkspace(final ServerContext context, final String workspaceName) {
        final GetWorkspaceCommand command = new GetWorkspaceCommand(context, workspaceName);
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

    /**
     * This command updates the properies of the workspace as well as the mappings.
     * There are many commands that go into the update, not just a single call.
     * If anything goes wrong, an exception will be thrown.
     * Note: this method does NOT sync the workspace.
     * @param context
     * @param oldWorkspace
     * @param newWorkspace
     */
    public static void updateWorkspace(final ServerContext context, final Workspace oldWorkspace, final Workspace newWorkspace) {
        // No need to update the mappings if they are the same
        if (WorkspaceHelper.areMappingsDifferent(oldWorkspace, newWorkspace)) {
            // First remove the mappings that are no longer needed
            for (final Workspace.Mapping m : WorkspaceHelper.getMappingsToRemove(oldWorkspace, newWorkspace)) {
                final UpdateWorkspaceMappingCommand command = new UpdateWorkspaceMappingCommand(context, oldWorkspace.getName(), m, true);
                command.runSynchronously();
            }

            // Now update the mappings to match the new workspace
            for (final Workspace.Mapping m : WorkspaceHelper.getMappingsToChange(oldWorkspace, newWorkspace)) {
                final UpdateWorkspaceMappingCommand command = new UpdateWorkspaceMappingCommand(context, oldWorkspace.getName(), m, false);
                command.runSynchronously();
            }
        }

        // Finally update the properties of the workspace
        final UpdateWorkspaceCommand updateWorkspaceCommand = new UpdateWorkspaceCommand(context, oldWorkspace.getName(),
                newWorkspace.getName(), newWorkspace.getComment(), null, null);
        updateWorkspaceCommand.runSynchronously();
    }

    /**
     * This method Syncs the workspace based on the root path recursively.
     * This is a synchronous call so it should only be called on a background thread.
     */
    public static void syncWorkspace(final ServerContext context, final String rootPath) {
        final SyncCommand command = new SyncCommand(context, Collections.singletonList(rootPath), true);
        command.runSynchronously();
    }

    /**
     * This method undoes the list of local files passed in.
     * This is a synchronous call so it should only be called on a background thread.
     */
    public static List<String> undoLocalFiles(final ServerContext context, final List<String> files) {
        final UndoCommand command = new UndoCommand(context, files);
        return command.runSynchronously();
    }
}
