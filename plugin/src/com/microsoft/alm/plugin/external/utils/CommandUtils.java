// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.utils;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.common.utils.SystemHelper;
import com.microsoft.alm.helpers.Path;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.commands.AddCommand;
import com.microsoft.alm.plugin.external.commands.CheckinCommand;
import com.microsoft.alm.plugin.external.commands.Command;
import com.microsoft.alm.plugin.external.commands.CreateBranchCommand;
import com.microsoft.alm.plugin.external.commands.FindConflictsCommand;
import com.microsoft.alm.plugin.external.commands.FindWorkspaceCommand;
import com.microsoft.alm.plugin.external.commands.GetBaseVersionCommand;
import com.microsoft.alm.plugin.external.commands.GetBranchesCommand;
import com.microsoft.alm.plugin.external.commands.GetLocalPathCommand;
import com.microsoft.alm.plugin.external.commands.GetWorkspaceCommand;
import com.microsoft.alm.plugin.external.commands.HistoryCommand;
import com.microsoft.alm.plugin.external.commands.InfoCommand;
import com.microsoft.alm.plugin.external.commands.MergeCommand;
import com.microsoft.alm.plugin.external.commands.RenameCommand;
import com.microsoft.alm.plugin.external.commands.ResolveConflictsCommand;
import com.microsoft.alm.plugin.external.commands.StatusCommand;
import com.microsoft.alm.plugin.external.commands.SyncCommand;
import com.microsoft.alm.plugin.external.commands.UndoCommand;
import com.microsoft.alm.plugin.external.commands.UpdateWorkspaceCommand;
import com.microsoft.alm.plugin.external.commands.UpdateWorkspaceMappingCommand;
import com.microsoft.alm.plugin.external.models.ChangeSet;
import com.microsoft.alm.plugin.external.models.Conflict;
import com.microsoft.alm.plugin.external.models.ConflictResults;
import com.microsoft.alm.plugin.external.models.ItemInfo;
import com.microsoft.alm.plugin.external.models.MergeConflict;
import com.microsoft.alm.plugin.external.models.MergeMapping;
import com.microsoft.alm.plugin.external.models.MergeResults;
import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.external.models.RenameConflict;
import com.microsoft.alm.plugin.external.models.ServerStatusType;
import com.microsoft.alm.plugin.external.models.SyncResults;
import com.microsoft.alm.plugin.external.models.VersionSpec;
import com.microsoft.alm.plugin.external.models.Workspace;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helper for running commands
 */
public class CommandUtils {
    protected static final Logger logger = LoggerFactory.getLogger(CommandUtils.class);

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

    public static String tryGetLocalPath(final ServerContext context, final String serverPath, final String workspace) {
        final Command<String> getLocalPathCommand = new GetLocalPathCommand(context, serverPath, workspace);
        try {
            final String result = getLocalPathCommand.runSynchronously();
            if (StringUtils.startsWithIgnoreCase(result, "ERROR [main] Application - Unexpected exception:")) {
                return null;
            }
            return result;
        } catch (Throwable t) {
            logger.warn("Failed to find local path for server path " + serverPath, t);
            return null;
        }
    }


    public static List<ChangeSet> getHistoryCommand(final ServerContext context, final String itemPath, final String version,
                                                    final int stopAfter, final boolean recursive, final String user) {
        return getHistoryCommand(context, itemPath, version, stopAfter, recursive, user, false);
    }

    public static List<ChangeSet> getHistoryCommand(final ServerContext context, final String itemPath, final String version,
                                                    final int stopAfter, final boolean recursive, final String user, final boolean itemMode) {
        final Command<List<ChangeSet>> historyCommand = new HistoryCommand(context, itemPath, version, stopAfter, recursive, user, itemMode);
        return historyCommand.runSynchronously();
    }

    public static ChangeSet getLastHistoryEntryForAnyUser(final ServerContext context, final String localPath) {
        final List<ChangeSet> results = getHistoryCommand(context, localPath, null, 1, false, StringUtils.EMPTY);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Adds a workspace mapping to the workspace named
     *
     * @param serverContext
     * @param workspaceName
     * @param serverPath
     * @param localPath
     */
    public static String addWorkspaceMapping(final ServerContext serverContext, final String workspaceName, final String serverPath, final String localPath) {
        final UpdateWorkspaceMappingCommand updateMappingCommand = new UpdateWorkspaceMappingCommand(
                serverContext,
                workspaceName,
                new Workspace.Mapping(serverPath, localPath, false),
                false);
        return updateMappingCommand.runSynchronously();
    }

    /**
     * This command updates the properies of the workspace as well as the mappings.
     * There are many commands that go into the update, not just a single call.
     * If anything goes wrong, an exception will be thrown.
     * Note: this method does NOT sync the workspace.
     *
     * @param context
     * @param oldWorkspace
     * @param newWorkspace
     */
    public static String updateWorkspace(final ServerContext context, final Workspace oldWorkspace, final Workspace newWorkspace) {
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
        return updateWorkspaceCommand.runSynchronously();
    }

    /**
     * This method Syncs the workspace based on the root path recursively.
     * This is a synchronous call so it should only be called on a background thread.
     */
    public static SyncResults syncWorkspace(final ServerContext context, final String rootPath) {
        return syncWorkspace(context, Collections.singletonList(rootPath), true);
    }

    public static SyncResults syncWorkspace(final ServerContext context, final List<String> filesUpdatePaths,
                                            final boolean needRecursion) {
        final SyncCommand command = new SyncCommand(context, filesUpdatePaths, needRecursion);
        return command.runSynchronously();
    }

    /**
     * This method undoes the list of local files passed in.
     * This is a synchronous call so it should only be called on a background thread.
     */
    public static List<String> undoLocalFiles(final ServerContext context, final List<String> files) {
        final UndoCommand command = new UndoCommand(context, files);
        return command.runSynchronously();
    }

    /**
     * Get the status for a single file
     *
     * @param context
     * @param file
     * @return
     */
    public static PendingChange getStatusForFile(final ServerContext context, final String file) {
        final Command<List<PendingChange>> command = new StatusCommand(context, file);
        final List<PendingChange> results = command.runSynchronously();
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Get the status for a list of files
     *
     * @param context
     * @param files
     * @return
     */
    public static List<PendingChange> getStatusForFiles(final ServerContext context, final List<String> files) {
        final Command<List<PendingChange>> command = new StatusCommand(context, files);
        return command.runSynchronously();
    }

    /**
     * Renames a file
     *
     * @param context
     * @param oldName
     * @param newName
     */
    public static void renameFile(final ServerContext context, final String oldName, final String newName) {
        final Command<String> command = new RenameCommand(context, oldName, newName);
        command.runSynchronously();
    }

    /**
     * Resolves conflicts with the given resolution type
     *
     * @param context
     * @param conflicts
     * @param type
     * @return
     */
    public static List<Conflict> resolveConflictsByPath(final ServerContext context, final List<String> conflicts, final ResolveConflictsCommand.AutoResolveType type) {
        final Command<List<Conflict>> conflictsCommand = new ResolveConflictsCommand(context, conflicts, type);
        return conflictsCommand.runSynchronously();
    }

    public static List<Conflict> resolveConflictsByConflict(final ServerContext context, final List<Conflict> conflicts, final ResolveConflictsCommand.AutoResolveType type) {
        final List<String> conflictFiles = new ArrayList<String>();
        for (final Conflict conflict : conflicts) {
            conflictFiles.add(conflict.getLocalPath());
        }

        return resolveConflictsByPath(context, conflictFiles, type);
    }

    /**
     * Finds the conflicts under a given directory
     *
     * @param context
     * @param root
     * @return
     */
    public static List<Conflict> getConflicts(final ServerContext context, final String root, final MergeResults mergeResults) {
        final Command<ConflictResults> conflictsCommand = new FindConflictsCommand(context, root);
        final ConflictResults conflictResults = conflictsCommand.runSynchronously();
        final List<Conflict> conflicts = new ArrayList<Conflict>(conflictResults.getConflicts().size());

        for (final Conflict conflict : conflictResults.getConflicts()) {
            Conflict newConflict = null;
            if (conflict.getType() == Conflict.ConflictType.CONTENT ||
                    conflict.getType() == Conflict.ConflictType.DELETE ||
                    conflict.getType() == Conflict.ConflictType.DELETE_TARGET) {
                newConflict = conflict;
            } else if (conflict.getType() == Conflict.ConflictType.RENAME ||
                    conflict.getType() == Conflict.ConflictType.NAME_AND_CONTENT) {
                // For renames we have to find the old name and the new name which creates a different type of conflict instance
                newConflict = findLocalRename(context, conflict.getLocalPath(), root, Conflict.ConflictType.RENAME);
                if (newConflict == null) {
                    // For the rare case where there is a rename done on both sides of a merge we can end up here
                    // So, try to find the merge conflict
                    newConflict = findMergeConflict(context, conflict, mergeResults, root);
                }
            } else if (conflict.getType() == Conflict.ConflictType.MERGE) {
                // For merge conflicts we have to find get the "from" path and the to "path" similar to renames using the MergeResult
                newConflict = findMergeConflict(context, conflict, mergeResults, root);
            } else {
                logger.warn("Unable to determine conflict type from: " + conflict.getType());
            }

            if (newConflict != null) {
                conflicts.add(newConflict);
            } else {
                logger.warn("Unable to convert Merge conflict in getConflicts");
            }

        }

        return conflicts;
    }

    private static MergeConflict findMergeConflict(final ServerContext context, final Conflict originalConflict, final MergeResults mergeResults, final String workingFolder) {
        final ItemInfo conflictInfo = getItemInfo(context, workingFolder, originalConflict.getLocalPath());
        if (mergeResults == null || conflictInfo == null) {
            return null;
        }

        // Find the matching merge mapping (and local path) from the merge results
        MergeMapping conflictMapping = null;
        String localPath = originalConflict.getLocalPath();

        // Check for the rename case (signified by the fact that the local path didn't provide legitimate info)
        if (StringUtils.isEmpty(conflictInfo.getServerItem())) {
            // To handle the rename in both branches case we have to find the matching mapping in a very complex way
            // We have a local path in the original conflict that doesn't actually exist and no way to construct the correct
            // server path to match. The only way to find the mapping is use the Resolve command to get the local path from
            // the server paths we already have.

            // We will loop thru our mappings looking for a RENAME and then try to use resolve to get the "new" local path
            for (final MergeMapping mapping : mergeResults.getMappings()) {
                if (mapping.getChangeTypes().contains(ServerStatusType.RENAME)) {
                    final Command<ConflictResults> conflictsCommand = new FindConflictsCommand(context, workingFolder, mapping.getToServerItem());
                    final ConflictResults conflictResults = conflictsCommand.runSynchronously();
                    if (conflictResults.getConflicts().size() == 1) {
                        final Conflict mappingConflict = conflictResults.getConflicts().get(0);
                        final String mappingLocalPath = Path.combine(workingFolder, mappingConflict.getLocalPath());
                        // If the local paths match, then we have the right mapping
                        if (StringUtils.equalsIgnoreCase(mappingLocalPath, originalConflict.getLocalPath())) {
                            // Now that we have the right mapping, let's figure out the right local path
                            final ItemInfo info = getItemInfo(context, workingFolder, mapping.getToServerItem());
                            localPath = info.getLocalItem();
                            conflictMapping = mapping;
                            break;
                        }
                    }
                }
            }
        } else {
            // Use the server path to find the matching mapping
            final String serverPath = conflictInfo.getServerItem();
            for (final MergeMapping mapping : mergeResults.getMappings()) {
                if (StringUtils.equalsIgnoreCase(mapping.getToServerItem(), serverPath)) {
                    conflictMapping = mapping;
                    break;
                }
            }
        }

        if (conflictMapping != null) {
            return new MergeConflict(localPath, conflictMapping);
        }

        return null;
    }

    /**
     * For rename conflicts, find the old name and local name of the file by looking for the last rename entry in the
     * history. Look at the last 50 history entries first and if not found there look at all the history
     *
     * @param context
     * @param serverName
     * @param root
     * @param type
     * @return
     */
    private static RenameConflict findLocalRename(final ServerContext context, final String serverName,
                                                  final String root, final Conflict.ConflictType type) {
        final RenameConflict conflict = searchChangeSetForRename(context, serverName, root, type, 50);

        // return conflict if found, else do a search on all of the history (-1 will not add a stopAfter parameter to cmd)
        return conflict != null ? conflict : searchChangeSetForRename(context, serverName, root, type, -1);
    }

    private static RenameConflict searchChangeSetForRename(final ServerContext context, final String serverName,
                                                           final String root, final Conflict.ConflictType type,
                                                           final int stopAfter) {
        final List<ChangeSet> changeSets = CommandUtils.getHistoryCommand(context, serverName, StringUtils.EMPTY,
                stopAfter, false, StringUtils.EMPTY, true);

        // step through most current changesets to find the one that did the rename
        for (int index = 0; index < changeSets.size(); index++) {
            final ChangeSet changeSet = changeSets.get(index);
            if (doesChangeSetHaveChanges(changeSets, index) &&
                    changeSet.getChanges().get(0).getChangeTypes().contains(ServerStatusType.RENAME)) {
                // the entry after the rename contains the old name of the file
                if (doesChangeSetHaveChanges(changeSets, index + 1)) {
                    final String oldName = changeSets.get(index + 1).getChanges().get(0).getServerItem();

                    // parse local changes for the old file name to get the new local name
                    final Command<List<PendingChange>> command = new StatusCommand(context, root);
                    final List<PendingChange> results = command.runSynchronously();

                    for (final PendingChange change : results) {
                        if (SystemHelper.areFilePathsSame(change.getSourceItem(), oldName)) {
                            return new RenameConflict(change.getLocalItem(), serverName, oldName, type);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Checks that a changeset in the list contains a change
     *
     * @param changeSets
     * @param index
     * @return
     */
    private static boolean doesChangeSetHaveChanges(final List<ChangeSet> changeSets, final int index) {
        if (changeSets == null
                || index >= changeSets.size()
                || changeSets.get(index).getChanges() == null
                || changeSets.get(index).getChanges().isEmpty()) {
            return false;
        }

        return true;
    }

    /**
     * Adds the given files to the repo
     *
     * @param context
     * @param filesToAddPaths
     * @return
     */
    public static List<String> addFiles(final ServerContext context, final List<String> filesToAddPaths) {
        final Command<List<String>> addCommand = new AddCommand(context, filesToAddPaths);
        return addCommand.runSynchronously();
    }

    /**
     * Checks in the list of files
     *
     * @param context
     * @param files
     * @param preparedComment
     * @return
     */
    public static String checkinFiles(final ServerContext context, final List<String> files, final String preparedComment, final List<Integer> workItemsToAssociate) {
        final Command<String> checkinCommand = new CheckinCommand(context, files, preparedComment, workItemsToAssociate);
        return checkinCommand.runSynchronously();
    }

    /**
     * Returns the item info for a single item.
     */
    public static ItemInfo getItemInfo(final ServerContext context, final String itemPath) {
        return getItemInfo(context, null, itemPath);
    }

    /**
     * Returns the item info for a single item. Specify a working folder in the workspace if you want info for a server
     * path.
     */
    public static ItemInfo getItemInfo(final ServerContext context, final String workingFolder, final String itemPath) {
        final Command<List<ItemInfo>> infoCommand = new InfoCommand(context, workingFolder, Collections.singletonList(itemPath));
        List<ItemInfo> items = infoCommand.runSynchronously();
        if (items != null && items.size() > 0) {
            return items.get(0);
        }

        throw new RuntimeException("No items match " + itemPath);
    }

    /**
     * Calls the branch command and returns the changeset number of the changeset created.
     * @param context
     * @param workingFolder
     * @param recursive
     * @param comment
     * @param author
     * @param existingItem
     * @param newBranchedItem
     * @return
     */
    public static String createBranch(final ServerContext context, final String workingFolder,
                                      final boolean recursive,
                                      final String comment, final String author,
                                      final String existingItem, final String newBranchedItem) {
        final CreateBranchCommand createBranchCommand = new CreateBranchCommand(context, workingFolder,
                recursive, comment, author, existingItem, newBranchedItem);
        return createBranchCommand.runSynchronously();
    }

    /**
     * Calls the merge command and returns the results
     * @param context
     * @param source
     * @param destination
     * @param versionSpec
     * @param recursive
     * @return
     */
    public static MergeResults merge(final ServerContext context, final String workingFolder, final String source, final String destination,
                                     final VersionSpec versionSpec, final boolean recursive) {
        final MergeCommand mergeCommand = new MergeCommand(context, workingFolder, source, destination, versionSpec, recursive);
        return mergeCommand.runSynchronously();
    }

    /**
     * Calls the get branches command and returns the list of branched items associated with the source item.
     * @param context
     * @param sourceItem
     * @return
     */
    public static List<String> getBranches(final ServerContext context, final String workingFolder, final String sourceItem) {
        final GetBranchesCommand getBranchesCommand = new GetBranchesCommand(context, workingFolder, sourceItem);
        return getBranchesCommand.runSynchronously();
    }

    public static VersionSpec getBaseVersion(final ServerContext context, final String workingFolder, final String source, final String destination) {
        final GetBaseVersionCommand getBaseVersionCommand = new GetBaseVersionCommand(context, workingFolder, source, destination);
        return getBaseVersionCommand.runSynchronously();
    }
}
