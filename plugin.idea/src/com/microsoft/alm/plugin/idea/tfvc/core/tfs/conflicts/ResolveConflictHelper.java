// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core.tfs.conflicts;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.openapi.vcs.update.FileGroup;
import com.intellij.openapi.vcs.update.UpdatedFiles;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsRunnable;
import com.intellij.vcsUtil.VcsUtil;
import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.common.utils.SystemHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.commands.ResolveConflictsCommand;
import com.microsoft.alm.plugin.external.models.ChangeSet;
import com.microsoft.alm.plugin.external.models.Conflict;
import com.microsoft.alm.plugin.external.models.MergeConflict;
import com.microsoft.alm.plugin.external.models.MergeResults;
import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.external.models.RenameConflict;
import com.microsoft.alm.plugin.external.models.ServerStatusType;
import com.microsoft.alm.plugin.external.models.VersionSpec;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.ModelValidationInfo;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import com.microsoft.alm.plugin.idea.tfvc.core.TFSVcs;
import com.microsoft.alm.plugin.idea.tfvc.core.revision.TFSContentRevision;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.VersionControlPath;
import com.microsoft.alm.plugin.idea.tfvc.ui.resolve.ContentTriplet;
import com.microsoft.alm.plugin.idea.tfvc.ui.resolve.ResolveConflictsModel;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Helper to resolve conflicts found when updating TFVC files
 */
public class ResolveConflictHelper {
    private static final Logger logger = LoggerFactory.getLogger(ResolveConflictHelper.class);

    @NotNull
    private final Project project;
    @Nullable
    private final UpdatedFiles updatedFiles;
    @NotNull
    private final List<String> updateRoots;
    @Nullable
    private final MergeResults mergeResults;

    public ResolveConflictHelper(final Project project,
                                 final UpdatedFiles updatedFiles,
                                 final List<String> updateRoots) {
        this(project, updatedFiles, updateRoots, null);
    }

    public ResolveConflictHelper(final Project project,
                                 final UpdatedFiles updatedFiles,
                                 final List<String> updateRoots,
                                 final MergeResults mergeResults) {
        this.project = project;
        this.updatedFiles = updatedFiles;
        this.updateRoots = updateRoots;
        this.mergeResults = mergeResults;
    }

    /**
     * Determines which type of merge conflict is present (content or name) and then gives the user the ability to resolve the issue
     *
     * @param conflict
     * @param model
     * @throws VcsException
     */
    public void acceptMerge(final @NotNull Conflict conflict, final ResolveConflictsModel model) throws VcsException {
        logger.info(String.format("Merging changes for file %s", conflict.getLocalPath()));
        final File conflictPath = new File(conflict.getLocalPath());
        final ServerContext context = TFSVcs.getInstance(project).getServerContext(false);

        final FilePath localPath = VersionControlPath.getFilePath(conflict.getLocalPath(), conflictPath.isDirectory());
        ContentTriplet contentTriplet = null;

        // only get file contents if a content conflict exists
        // need to do this before any rename changes take place b/c it could overwrite the contents of the file
        if (isContentConflict(conflict)) {
            logger.info("Content conflict have been found so getting file contents");
            contentTriplet = populateThreeWayDiffWithProgress(conflict, conflictPath, localPath, context);
        }

        if (isNameConflict(conflict) && isContentConflict(conflict)) {
            logger.info("Both conflict types found");
            processBothConflicts(conflict, context, model, conflictPath, contentTriplet);
        } else if (isNameConflict(conflict)) {
            logger.info("Naming conflict found");
            processRenameConflict(conflict, context, model);
        } else if (isContentConflict(conflict)) {
            logger.info("Content conflict found");
            processContentConflict(context, model, contentTriplet, localPath);
        } else {
            logger.error("Unknown conflict state");
        }
    }

    /**
     * Process a conflict that has both rename and content conflicts
     *
     * @param conflict
     * @param context
     * @param model
     * @param conflictPath
     * @param contentTriplet
     * @throws VcsException
     */
    @VisibleForTesting
    protected void processBothConflicts(final Conflict conflict, final ServerContext context, final ResolveConflictsModel model, final File conflictPath, @Nullable final ContentTriplet contentTriplet) throws VcsException {
        final RenameConflict renameConflict = (RenameConflict) conflict;
        final String mergedServerPath = ConflictsEnvironment.getNameMerger().mergeName(renameConflict, project);
        if (mergedServerPath == null) {
            // user cancelled
            logger.warn("User canceled rename merge");
            return;
        }

        // take their name
        if (StringUtils.equals(mergedServerPath, renameConflict.getServerPath())) {
            logger.debug("Taking their name before processing content conflict");
            // resolve name conflict but do not update updatedFiles yet because content conflict still needs resolved
            resolveConflictWithProgress(conflict.getLocalPath(), ResolveConflictsCommand.AutoResolveType.TakeTheirs, context, model, false);
            processContentConflict(context, model, contentTriplet, VersionControlPath.getFilePath(mergedServerPath, conflictPath.isDirectory()));
        } else {
            // keep your name so just skip to content resolution which will do the resolve for you (it always does KeepYours)
            logger.debug("Keeping your name so continue to content conflict");
            processContentConflict(context, model, contentTriplet, VersionControlPath.getFilePath(conflict.getLocalPath(), conflictPath.isDirectory()));
        }
    }

    /**
     * Process a rename conflict specifically
     *
     * @param conflict
     * @param context
     * @param model
     * @throws VcsException
     */
    @VisibleForTesting
    protected void processRenameConflict(final Conflict conflict, final ServerContext context, final ResolveConflictsModel model) throws VcsException {
        final RenameConflict renameConflict = (RenameConflict) conflict;
        final String mergedServerPath = ConflictsEnvironment.getNameMerger().mergeName(renameConflict, project);
        if (mergedServerPath == null) {
            // user cancelled
            logger.warn("User canceled rename merge");
            return;
        }

        // take their name
        if (StringUtils.equals(mergedServerPath, renameConflict.getServerPath())) {
            logger.debug("Taking their name");
            resolveConflictWithProgress(conflict.getLocalPath(), mergedServerPath, ResolveConflictsCommand.AutoResolveType.TakeTheirs, context, model, true);
        } else {
            // keep your name
            logger.debug("Keeping your name");
            resolveConflictWithProgress(conflict.getLocalPath(), ResolveConflictsCommand.AutoResolveType.KeepYours, context, model, true);
        }
    }

    /**
     * Process a content conflict specifically
     *
     * @param context
     * @param model
     * @param contentTriplet
     * @param localPath
     * @throws VcsException
     */
    @VisibleForTesting
    protected void processContentConflict(final ServerContext context, final ResolveConflictsModel model, final ContentTriplet contentTriplet, final FilePath localPath) throws VcsException {
        boolean resolved = true;
        if (contentTriplet != null) {
            final File localFile = new File(localPath.getPath());
            ArgumentHelper.checkIfFile(localFile);
            VirtualFile vFile = VcsUtil.getVirtualFileWithRefresh(localFile);
            if (vFile != null) {
                try {
                    TfsFileUtil.setReadOnly(vFile, false);
                    // opens dialog for merge
                    resolved = ConflictsEnvironment.getContentMerger()
                            .mergeContent(contentTriplet, project, vFile, null);
                } catch (IOException e) {
                    throw new VcsException(e);
                }
            } else {
                throw new VcsException(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_MERGE_LOAD_FAILED, localPath.getPresentableUrl()));
            }
        }

        if (resolved) {
            resolveConflictWithProgress(localPath.getPath(), ResolveConflictsCommand.AutoResolveType.KeepYours, context, model, true);
        } else {
            logger.warn("Conflict merge was aborted by user");
        }
    }

    @VisibleForTesting
    protected void resolveConflictWithProgress(final String localPath, final ResolveConflictsCommand.AutoResolveType type, final ServerContext context, final ResolveConflictsModel model, final boolean updateFiles) throws VcsException {
        resolveConflictWithProgress(localPath, localPath, type, context, model, updateFiles);
    }

    @VisibleForTesting
    protected void resolveConflictWithProgress(final String localPath, final String updatedPath, final ResolveConflictsCommand.AutoResolveType type, final ServerContext context, final ResolveConflictsModel model, final boolean updateFiles) throws VcsException {
        final VcsRunnable resolveRunnable = new VcsRunnable() {
            public void run() throws VcsException {
                resolveConflict(localPath, updatedPath, type, context, model, updateFiles);
            }
        };
        VcsUtil.runVcsProcessWithProgress(resolveRunnable, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_RESOLVING_PROGRESS_BAR), false, project);
    }

    /**
     * Resolve a conflict based on the given local path, update the updatedFiles if needed with the updated local path (this is needed for renames where the local path changes once a resolve takes place),
     * then reload the conflicts
     *
     * @param localPath
     * @param updatedPath
     * @param type
     * @param context
     * @param model
     * @param updateFiles
     * @throws VcsException
     */
    @VisibleForTesting
    protected void resolveConflict(final String localPath, final String updatedPath, final ResolveConflictsCommand.AutoResolveType type, final ServerContext context, final ResolveConflictsModel model, final boolean updateFiles) throws VcsException {
        IdeaHelper.setProgress(ProgressManager.getInstance().getProgressIndicator(), 0.1, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_RESOLVING_STATUS, localPath));

        try {
            CommandUtils.resolveConflictsByPath(context, Arrays.asList(localPath), type);

            if (updateFiles && updatedFiles != null) {
                // use local path from conflict to update list because results from command gives server name even if you keep your own change
                updatedFiles.getGroupById(FileGroup.MERGED_ID).add(updatedPath, TFSVcs.getKey(), null);
            }

            // update progress
            IdeaHelper.setProgress(ProgressManager.getInstance().getProgressIndicator(), 0.5, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_RESOLVING_REFRESH));

            // reload conflicts
            findConflicts(model);
        } catch (VcsException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error while resolving conflict: " + e.getMessage());
            throw new VcsException(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_MERGE_ERROR, localPath, e.getMessage()));
        }
    }

    @VisibleForTesting
    protected ContentTriplet populateThreeWayDiffWithProgress(final Conflict conflict, final File conflictPath,
                                                              final FilePath localPath, final ServerContext context) throws VcsException {
        final ContentTriplet contentTriplet = new ContentTriplet();
        final VcsRunnable runnable = new VcsRunnable() {
            public void run() throws VcsException {
                populateThreeWayDiff(conflict, conflictPath, localPath, context, contentTriplet);
            }
        };
        VcsUtil.runVcsProcessWithProgress(runnable, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_MERGE_LOADING), false, project);
        return contentTriplet;
    }

    /**
     * Gets contents for the 3 way diff
     *
     * @param conflict
     * @param conflictPath
     * @param localPath
     * @param context
     * @return
     * @throws VcsException
     */
    @VisibleForTesting
    protected void populateThreeWayDiff(final Conflict conflict, final File conflictPath, final FilePath localPath,
                                        final ServerContext context, final ContentTriplet contentTriplet) throws VcsException {
        // virtual file can be out of the current project so force its discovery
        TfsFileUtil.refreshAndFindFile(localPath);

        // update progress
        IdeaHelper.setProgress(ProgressManager.getInstance().getProgressIndicator(), 0.1, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_MERGE_ORIGINAL));

        try {
            // only get diff contents if it is an edited file
            if (conflictPath.isFile()) {
                final String original;
                final String serverChanges;
                final String myLocalChanges;

                final PendingChange originalChange = CommandUtils.getStatusForFile(context, conflict.getLocalPath());
                if (isMergeConflict(conflict, null)) {
                    final String workingFolder = localPath.isDirectory() ?
                            localPath.getPath() :
                            localPath.getParentPath().getPath();
                    final MergeConflict mergeConflict = (MergeConflict) conflict;
                    final VersionSpec baseVersion = CommandUtils.getBaseVersion(
                            context, workingFolder, mergeConflict.getOldPath(), mergeConflict.getServerPath());

                    original = TFSContentRevision.createRenameRevision(project, context, localPath,
                            SystemHelper.toInt(baseVersion.getValue(), 1), originalChange.getDate(), mergeConflict.getOldPath()).getContent();
                    serverChanges = TFSContentRevision.createRenameRevision(project, context, localPath,
                            getMergeFromVersion(mergeConflict), originalChange.getDate(), mergeConflict.getOldPath()).getContent();
                    myLocalChanges = CurrentContentRevision.create(localPath).getContent();
                } else {
                    // get original contents of file
                    if (originalChange != null) {
                        final int version = Integer.parseInt(originalChange.getVersion());
                        if (isNameConflict(conflict)) {
                            final FilePath renamePath = VersionControlPath.getFilePath(conflict.getLocalPath(), conflictPath.isDirectory());
                            original = TFSContentRevision.createRenameRevision(project, context, renamePath,
                                    version, originalChange.getDate(), ((RenameConflict) conflict).getOldPath()).getContent();
                        } else {
                            original = TFSContentRevision.create(project, context, localPath,
                                    version, originalChange.getDate()).getContent();
                        }
                    } else {
                        original = null;
                    }

                    // update progress
                    IdeaHelper.setProgress(ProgressManager.getInstance().getProgressIndicator(), 0.5, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_MERGE_SERVER));

                    // get content from local file
                    myLocalChanges = CurrentContentRevision.create(localPath).getContent();

                    // get content from server
                    if (isNameConflict(conflict)) {
                        final ChangeSet serverChange = CommandUtils.getLastHistoryEntryForAnyUser(context, ((RenameConflict) conflict).getServerPath());
                        final FilePath renamePath = VersionControlPath.getFilePath(conflict.getLocalPath(), conflictPath.isDirectory());
                        serverChanges = TFSContentRevision.createRenameRevision(project, context, renamePath, serverChange.getIdAsInt(), serverChange.getDate(), ((RenameConflict) conflict).getServerPath()).getContent();
                    } else {
                        final ChangeSet serverChange = CommandUtils.getLastHistoryEntryForAnyUser(context, conflict.getLocalPath());
                        serverChanges = TFSContentRevision.create(project, context, localPath, serverChange.getIdAsInt(), serverChange.getDate()).getContent();
                    }
                }

                contentTriplet.baseContent = original != null ? original : StringUtils.EMPTY;
                contentTriplet.localContent = myLocalChanges != null ? myLocalChanges : StringUtils.EMPTY;
                contentTriplet.serverContent = serverChanges != null ? serverChanges : StringUtils.EMPTY;
            }
        } catch (Exception e) {
            logger.error("Error loading contents for files");
            throw new VcsException(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_LOAD_FAILED, localPath.getPresentableUrl(), e.getMessage()));
        }
    }

    public void acceptChanges(final @NotNull String conflict, final ResolveConflictsCommand.AutoResolveType type) {
        if (type == ResolveConflictsCommand.AutoResolveType.TakeTheirs) {
            acceptTheirs(conflict);
        } else if (type == ResolveConflictsCommand.AutoResolveType.KeepYours) {
            acceptYours(conflict);
        }
    }

    public void acceptYours(final @NotNull String conflict) {
        // treat just like skip
        skip(conflict);
    }

    public void acceptTheirs(final @NotNull String conflict) {
        if (updatedFiles != null) {
            //TODO create version number
            updatedFiles.getGroupById(FileGroup.UPDATED_ID).add(conflict, TFSVcs.getKey(), null);
        }
    }

    public void skip(final @NotNull String conflict) {
        if (updatedFiles != null) {
            // Jetbrains used null for version number in this case
            updatedFiles.getGroupById(FileGroup.SKIPPED_ID).add(conflict, TFSVcs.getKey(), null);
        }
    }

    public void skip(final @NotNull List<Conflict> conflicts) {
        for (final Conflict conflict : conflicts) {
            // Jetbrains used null for version number in this case
            skip(conflict.getLocalPath());
        }
    }

    public static boolean isNameConflict(final @NotNull Conflict conflict) {
        // Return true if this is a Rename conflict, Name and Content conflict, or a Merge(with rename) conflict
        return Conflict.ConflictType.RENAME.equals(conflict.getType()) ||
                Conflict.ConflictType.NAME_AND_CONTENT.equals(conflict.getType()) ||
                isMergeConflict(conflict, ServerStatusType.RENAME);
    }

    public static boolean isContentConflict(final @NotNull Conflict conflict) {
        // Return true if this is a content conflict, Name and Content conflict, or a Merge(with edit) conflict
        return Conflict.ConflictType.CONTENT.equals(conflict.getType()) ||
                Conflict.ConflictType.NAME_AND_CONTENT.equals(conflict.getType()) ||
                isMergeConflict(conflict, ServerStatusType.EDIT);
    }

    public static boolean isMergeConflict(final @NotNull Conflict conflict, final @Nullable ServerStatusType matchingChangeType) {
        if (Conflict.ConflictType.MERGE.equals(conflict.getType())) {
            final MergeConflict mergeConflict = (MergeConflict) conflict;
            if (matchingChangeType != null) {
                return mergeConflict.getMapping().getChangeTypes().contains(matchingChangeType);
            }

            return true;
        }
        return false;
    }

    public static int getMergeFromVersion(final MergeConflict mergeConflict) {
        if (mergeConflict == null || mergeConflict.getMapping() == null ||
                mergeConflict.getMapping().getFromServerItemVersion() == null ||
                mergeConflict.getMapping().getFromServerItemVersion().getEnd().getType() != VersionSpec.Type.Changeset) {
            logger.warn("The merge conflict does not contain the correct From Version Range (End).");
            throw new IllegalArgumentException("mergeConflict");
        }
        return SystemHelper.toInt(mergeConflict.getMapping().getFromServerItemVersion().getEnd().getValue(), 0);
    }

    /**
     * Resolve the conflicts based on auto resolve type and then refresh the table model to update the list of conflicts
     *
     * @param conflicts
     * @param type
     */
    public void acceptChangeAsync(final List<Conflict> conflicts, final ResolveConflictsCommand.AutoResolveType type, final ResolveConflictsModel model) {
        logger.info(String.format("Accepting changes to %s for file %s", type.name(), Arrays.toString(conflicts.toArray())));
        final Task.Backgroundable loadConflictsTask = new Task.Backgroundable(project, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_RESOLVING_PROGRESS_BAR),
                true, PerformInBackgroundOption.DEAF) {

            @Override
            public void run(@NotNull final ProgressIndicator progressIndicator) {
                acceptChange(conflicts, progressIndicator, project, type, model);
            }
        };
        loadConflictsTask.queue();
    }

    @VisibleForTesting
    protected void acceptChange(final List<Conflict> conflicts, final ProgressIndicator progressIndicator, final Project project, final ResolveConflictsCommand.AutoResolveType type, final ResolveConflictsModel model) {
        for (final Conflict conflict : conflicts) {
            try {
                progressIndicator.setText(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_RESOLVING_STATUS, conflict.getLocalPath()));
                final List<Conflict> resolved = CommandUtils.resolveConflictsByConflict(TFSVcs.getInstance(project).getServerContext(false), Arrays.asList(conflict), type);

                // check if error is a rename so the correct file name is displayed in the Update Info tab
                if (resolved != null && resolved.size() > 0) {
                    if (conflict instanceof RenameConflict && ResolveConflictsCommand.AutoResolveType.TakeTheirs.equals(type)) {
                        acceptChanges(((RenameConflict) conflict).getServerPath(), type);
                    } else {
                        acceptChanges(conflict.getLocalPath(), type);
                    }
                } else {
                    skip(Arrays.asList(conflict));
                }
            } catch (Exception e) {
                logger.error("Error while handling merge resolution: " + e.getMessage());
                model.addError(ModelValidationInfo.createWithMessage(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_MERGE_ERROR, conflict.getLocalPath(), e.getMessage())));
            }
        }

        // update status bar
        IdeaHelper.setProgress(progressIndicator, 0.5, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_RESOLVING_REFRESH));

        try {
            // refresh conflicts so resolved ones are removed
            findConflicts(model);
        } catch (VcsException e) {
            model.addError(ModelValidationInfo.createWithMessage(e.getMessage()));
        }
    }

    /**
     * Call command to find conflicts and add to table model
     * <p/>
     * Should always be called on a background thread!
     */
    public void findConflicts(final ResolveConflictsModel model) throws VcsException {
        final List<Conflict> conflicts = new ArrayList<Conflict>();
        try {
            for (final String updatePath : updateRoots) {
                conflicts.addAll(CommandUtils.getConflicts(TFSVcs.getInstance(project).getServerContext(false), updatePath, mergeResults));
            }
        } catch (Exception e) {
            logger.error("Error while finding conflicts: " + e.getMessage());
            throw new VcsException(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_LOAD_ERROR));
        }

        // sort results based on path
        Collections.sort(conflicts, new Comparator<Conflict>() {
            @Override
            public int compare(Conflict conflict1, Conflict conflict2) {
                return conflict1.getLocalPath().compareToIgnoreCase(conflict2.getLocalPath());
            }
        });

        IdeaHelper.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                model.getConflictsTableModel().setConflicts(conflicts);
            }
        });
    }
}
