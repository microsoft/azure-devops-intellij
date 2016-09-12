// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core.tfs.conflicts;

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
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.commands.ResolveConflictsCommand;
import com.microsoft.alm.plugin.external.models.ChangeSet;
import com.microsoft.alm.plugin.external.models.Conflict;
import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.external.models.RenameConflict;
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

public class ResolveConflictHelper {
    private static final Logger logger = LoggerFactory.getLogger(ResolveConflictHelper.class);

    @NotNull
    private final Project project;
    @Nullable
    private final UpdatedFiles updatedFiles;
    @NotNull
    private final List<String> updateRoots;

    public ResolveConflictHelper(final Project project,
                                 final UpdatedFiles updatedFiles,
                                 final List<String> updateRoots) {
        this.project = project;
        this.updatedFiles = updatedFiles;
        this.updateRoots = updateRoots;
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

        FilePath localPath = VersionControlPath.getFilePath(conflict.getLocalPath(), conflictPath.isDirectory());
        ContentTriplet contentTriplet = null;

        // only get file contents if a content conflict exists
        if (isContentConflict(conflict)) {
            logger.info("Content conflict have been found so getting file contents");
            contentTriplet = populateContents(conflict, conflictPath, localPath, context);
        }

        // merge names
        if (isNameConflict(conflict)) {
            logger.info("Naming conflict found");
            final RenameConflict renameConflict = (RenameConflict) conflict;
            final String mergedServerPath = ConflictsEnvironment.getNameMerger().mergeName(renameConflict, project);
            if (mergedServerPath == null) {
                // user cancelled
                logger.warn("User canceled rename merge");
                return;
            }

            // rename local file if server name was selected
            // do this by resolving conflict to TakeTheirs (you won't lose the current content because its already has been saved above)
            if (StringUtils.equals(mergedServerPath, renameConflict.getServerPath())) {
                final VcsRunnable resolveRunnable = new VcsRunnable() {
                    public void run() throws VcsException {
                        IdeaHelper.setProgress(ProgressManager.getInstance().getProgressIndicator(), 0.1, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_RESOLVING_STATUS, conflict.getLocalPath()));

                        final List<Conflict> resolvedFiles = CommandUtils.resolveConflictsByPath(context, Arrays.asList(conflict.getLocalPath()), ResolveConflictsCommand.AutoResolveType.TakeTheirs);

                        // if there are no content conflicts, add to the updatedFiles list and refresh conflicts
                        if (!isContentConflict(conflict)) {
                            // only 1 file is merged at a time so if resolvedFiles is not empty take the first first entry since that will be the only entry
                            if (!resolvedFiles.isEmpty()) {
                                // TODO: create version number
                                updatedFiles.getGroupById(FileGroup.MERGED_ID).add(resolvedFiles.get(0).getLocalPath(), TFSVcs.getKey(), null);
                            }

                            // update progress
                            IdeaHelper.setProgress(ProgressManager.getInstance().getProgressIndicator(), 0.5, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_RESOLVING_REFRESH));

                            // reload conflicts
                            findConflicts(model);
                        }
                    }
                };
                VcsUtil.runVcsProcessWithProgress(resolveRunnable, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_RESOLVING_PROGRESS_BAR), false, project);

                // if no content conflicts then no more work to do
                if (!isContentConflict(conflict)) {
                    return;
                }

                // update the local path to the new path
                localPath = VersionControlPath.getFilePath(mergedServerPath, conflictPath.isDirectory());
            }
        }

        // merge content
        boolean resolved = true;
        if (isContentConflict(conflict) && contentTriplet != null) {
            logger.info("Content conflict found");
            final File localFile = new File(localPath.getPath());
            ArgumentHelper.checkIfFile(localFile);
            VirtualFile vFile = VcsUtil.getVirtualFileWithRefresh(localFile);
            if (vFile != null) {
                try {
                    TfsFileUtil.setReadOnly(vFile, false);
                    // opens dialog for merge
                    resolved = ConflictsEnvironment.getContentMerger()
                            .mergeContent(conflict.getLocalPath(), contentTriplet, project, vFile, conflict.getLocalPath(), null);
                } catch (IOException e) {
                    throw new VcsException(e);
                }
            } else {
                throw new VcsException(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_MERGE_LOAD_FAILED, localPath.getPresentableUrl()));
            }
        }

        // get the final path of the conflict file since it may have changed due to rename
        final String finalLocalPath = localPath.getPath();

        if (resolved) {
            final VcsRunnable resolveRunnable = new VcsRunnable() {
                public void run() throws VcsException {
                    IdeaHelper.setProgress(ProgressManager.getInstance().getProgressIndicator(), 0.1, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_RESOLVING_STATUS, finalLocalPath));

                    try {
                        CommandUtils.resolveConflictsByPath(context, Arrays.asList(finalLocalPath), ResolveConflictsCommand.AutoResolveType.KeepYours);
                        // TODO: create version number
                        // use local path from conflict to update list because results from command gives server name even if you keep your own change
                        updatedFiles.getGroupById(FileGroup.MERGED_ID).add(finalLocalPath, TFSVcs.getKey(), null);

                        // update progress
                        IdeaHelper.setProgress(ProgressManager.getInstance().getProgressIndicator(), 0.5, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_RESOLVING_REFRESH));

                        // reload conflicts
                        findConflicts(model);
                    } catch (VcsException e) {
                        throw e;
                    } catch (Exception e) {
                        logger.error("Error while resolving conflict: " + e.getMessage());
                        throw new VcsException(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_MERGE_ERROR, finalLocalPath, e.getMessage()));
                    }
                }
            };
            VcsUtil.runVcsProcessWithProgress(resolveRunnable, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_RESOLVING_PROGRESS_BAR), false, project);
        }
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
    private ContentTriplet populateContents(final Conflict conflict, final File conflictPath, final FilePath localPath, final ServerContext context) throws VcsException {
        final ContentTriplet contentTriplet = new ContentTriplet();
        final VcsRunnable runnable = new VcsRunnable() {
            public void run() throws VcsException {
                // virtual file can be out of the current project so force its discovery
                TfsFileUtil.refreshAndFindFile(localPath);

                // update progress
                IdeaHelper.setProgress(ProgressManager.getInstance().getProgressIndicator(), 0.1, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_MERGE_ORIGINAL));

                try {
                    // only get diff contents if it is an edited file
                    if (conflictPath.isFile()) {
                        // get original contents of file
                        final PendingChange originalChange = CommandUtils.getStatusForFile(context, conflict.getLocalPath());
                        String original = null;
                        if (originalChange != null) {
                            if (isNameConflict(conflict)) {
                                final FilePath renamePath = VersionControlPath.getFilePath(conflict.getLocalPath(), conflictPath.isDirectory());
                                original = TFSContentRevision.createRenameRevision(project, context, renamePath,
                                        Integer.parseInt(originalChange.getVersion()), originalChange.getDate(), ((RenameConflict) conflict).getOldPath()).getContent();
                            } else {
                                original = TFSContentRevision.create(project, context, localPath,
                                        Integer.parseInt(originalChange.getVersion()), originalChange.getDate()).getContent();
                            }
                        }

                        // update progress
                        IdeaHelper.setProgress(ProgressManager.getInstance().getProgressIndicator(), 0.5, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_MERGE_SERVER));

                        // get content from local file
                        final String myLocalChanges = CurrentContentRevision.create(localPath).getContent();

                        // get content from server
                        final String serverChanges;
                        if (isNameConflict(conflict)) {
                            final ChangeSet serverChange = CommandUtils.getLastHistoryEntryForAnyUser(context, ((RenameConflict) conflict).getServerPath());
                            final FilePath renamePath = VersionControlPath.getFilePath(conflict.getLocalPath(), conflictPath.isDirectory());
                            serverChanges = TFSContentRevision.createRenameRevision(project, context, renamePath, Integer.parseInt(serverChange.getId()), serverChange.getDate(), ((RenameConflict) conflict).getServerPath()).getContent();
                        } else {
                            final ChangeSet serverChange = CommandUtils.getLastHistoryEntryForAnyUser(context, conflict.getLocalPath());
                            serverChanges = TFSContentRevision.create(project, context, localPath, Integer.parseInt(serverChange.getId()), serverChange.getDate()).getContent();
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
        };

        VcsUtil.runVcsProcessWithProgress(runnable, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_MERGE_LOADING), false, project);
        return contentTriplet;
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

    private static boolean isNameConflict(final @NotNull Conflict conflict) {
        return Conflict.ConflictType.RENAME.equals(conflict.getType()) || Conflict.ConflictType.BOTH.equals(conflict.getType());
    }

    private static boolean isContentConflict(final @NotNull Conflict conflict) {
        return Conflict.ConflictType.CONTENT.equals(conflict.getType()) || Conflict.ConflictType.BOTH.equals(conflict.getType());
    }

    /**
     * Resolve the conflicts based on auto resolve type and then refresh the table model to update the list of conflicts
     *
     * @param conflicts
     * @param type
     */
    public void acceptChange(final List<Conflict> conflicts, final ResolveConflictsCommand.AutoResolveType type, final ResolveConflictsModel model) {
        logger.info(String.format("Accepting changes to %s for file %s", type.name(), Arrays.toString(conflicts.toArray())));
        final Task.Backgroundable loadConflictsTask = new Task.Backgroundable(project, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_RESOLVING_PROGRESS_BAR),
                true, PerformInBackgroundOption.DEAF) {

            @Override
            public void run(@NotNull final ProgressIndicator progressIndicator) {
                for (final Conflict conflict : conflicts) {
                    try {
                        progressIndicator.setText(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_RESOLVING_STATUS, conflict.getLocalPath()));
                        final List<Conflict> resolved = CommandUtils.resolveConflictsByConflict(TFSVcs.getInstance(myProject).getServerContext(false), Arrays.asList(conflict), type);

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
        };
        loadConflictsTask.queue();
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
                conflicts.addAll(CommandUtils.getConflicts(TFSVcs.getInstance(project).getServerContext(false), updatePath));
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
