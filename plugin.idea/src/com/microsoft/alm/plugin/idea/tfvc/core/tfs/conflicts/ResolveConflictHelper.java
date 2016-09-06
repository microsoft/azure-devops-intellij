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
import com.microsoft.alm.plugin.external.commands.Command;
import com.microsoft.alm.plugin.external.commands.FindConflictsCommand;
import com.microsoft.alm.plugin.external.commands.ResolveConflictsCommand;
import com.microsoft.alm.plugin.external.models.ChangeSet;
import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.external.models.ServerStatusType;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
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
import java.util.List;

public class ResolveConflictHelper {
    private static final Logger logger = LoggerFactory.getLogger(ResolveConflictHelper.class);

    @NotNull
    private final Project myProject;
    @Nullable
    private final UpdatedFiles myUpdatedFiles;
    @NotNull
    private final List<String> updateRoots;

    public ResolveConflictHelper(final Project project,
                                 final UpdatedFiles updatedFiles,
                                 final List<String> updateRoots) {
        myProject = project;
        myUpdatedFiles = updatedFiles;
        this.updateRoots = updateRoots;
    }

    /**
     * Determines which type of merge conflict is present (content or name) and then gives the user the ability to resolve the issue
     *
     * @param conflict
     * @param model
     * @throws VcsException
     */
    public void acceptMerge(final @NotNull String conflict, final ResolveConflictsModel model) throws VcsException {
        final File conflictPath = new File(conflict);
        final FilePath localPath = VersionControlPath.getFilePath(conflict, conflictPath.isDirectory());
        final ServerContext context = TFSVcs.getInstance(myProject).getServerContext(false);

        final ContentTriplet contentTriplet = new ContentTriplet();
        final List<ServerStatusType> changeTypes = new ArrayList<ServerStatusType>();
        final VcsRunnable runnable = new VcsRunnable() {
            public void run() throws VcsException {
                // virtual file can be out of the current project so force its discovery
                TfsFileUtil.refreshAndFindFile(localPath);

                // update progress
                IdeaHelper.setProgress(ProgressManager.getInstance().getProgressIndicator(), 0.1, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_MERGE_ORIGINAL));

                try {
                    // get original contents first so you know what type of change it is
                    final PendingChange originalChange = CommandUtils.getStatusForFile(context, conflict);
                    String original = null;
                    if (originalChange != null) {
                        original = TFSContentRevision.create(myProject, context, localPath,
                                Integer.parseInt(originalChange.getVersion()), originalChange.getDate()).getContent();
                        changeTypes.addAll(originalChange.getChangeTypes());
                    }

                    // update progress
                    IdeaHelper.setProgress(ProgressManager.getInstance().getProgressIndicator(), 0.5, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_MERGE_SERVER));

                    // only get diff contents if it is an edited file
                    if (conflictPath.isFile() && isContentConflict(changeTypes)) {
                        // get content from local file
                        final String myLocalChanges = CurrentContentRevision.create(localPath).getContent();

                        // get content from server
                        final ChangeSet serverChange = CommandUtils.getLastHistoryEntryForAnyUser(context, conflict);
                        final String serverChanges = TFSContentRevision.create(myProject, context, localPath, Integer.parseInt(serverChange.getId()), serverChange.getDate()).getContent();

                        contentTriplet.baseContent = original != null ? original : StringUtils.EMPTY;
                        contentTriplet.localContent = myLocalChanges != null ? myLocalChanges : StringUtils.EMPTY;
                        contentTriplet.serverContent = serverChanges != null ? serverChanges : StringUtils.EMPTY;
                    }
                } catch (Exception e) {
                    throw new VcsException(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_LOAD_FAILED, localPath.getPresentableUrl(), e.getMessage()));
                }
            }
        };

        // needed for both content and rename conflicts
        VcsUtil.runVcsProcessWithProgress(runnable, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_MERGE_LOADING), false, myProject);

        // TODO: merge names
//        final String localName;
//        if (isNameConflict(conflict)) {
//            // TODO proper type?
//            final String mergedServerPath = ConflictsEnvironment.getNameMerger().mergeName(workspace, conflict, myProject);
//            if (mergedServerPath == null) {
//                // user cancelled
//                return;
//            }
//            //noinspection ConstantConditions
//            @NotNull FilePath mergedLocalPath =
//                    workspace.findLocalPathByServerPath(mergedServerPath, conflict.getYtype() == ItemType.Folder, myProject);
//            localName = mergedLocalPath.getPath();
//        } else {
//            localName = VersionControlPath.localPathFromTfsRepresentation(conflict.getTgtlitem());
//        }

        boolean resolved = true;
        // merge content
        if (isContentConflict(changeTypes)) {
            ArgumentHelper.checkIfFile(new File(conflict));
            VirtualFile vFile = localPath.getVirtualFile();
            if (vFile != null) {
                try {
                    TfsFileUtil.setReadOnly(vFile, false);
                    // opens dialog for merge
                    resolved = ConflictsEnvironment.getContentMerger()
                            .mergeContent(conflict, contentTriplet, myProject, vFile, conflict, null);
                } catch (IOException e) {
                    throw new VcsException(e);
                }
            } else {
                throw new VcsException(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_MERGE_LOAD_FAILED, localPath.getPresentableUrl()));
            }
        }
        if (resolved) {
            final VcsRunnable resolveRunnable = new VcsRunnable() {
                public void run() throws VcsException {
                    IdeaHelper.setProgress(ProgressManager.getInstance().getProgressIndicator(), 0.1, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_RESOLVING_STATUS));
                    final List<String> resolvedFiles = CommandUtils.resolveConflicts(context, Arrays.asList(conflict), ResolveConflictsCommand.AutoResolveType.KeepYours);
                    // only 1 file is merged at a time so if resolvedFiles is not empty take the first first entry since that will be the only entry
                    if (!resolvedFiles.isEmpty()) {
                        // TODO: create version number
                        myUpdatedFiles.getGroupById(FileGroup.MERGED_ID).add(resolvedFiles.get(0), TFSVcs.getKey(), null);
                    }

                    // update progress
                    IdeaHelper.setProgress(ProgressManager.getInstance().getProgressIndicator(), 0.5, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_RESOLVING_REFRESH));

                    // reload conflicts
                    findConflicts(model);
                }
            };
            VcsUtil.runVcsProcessWithProgress(resolveRunnable, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_RESOLVING_PROGRESS_BAR), false, myProject);
        }
    }

    public void acceptChanges(final @NotNull List<String> conflicts, final ResolveConflictsCommand.AutoResolveType type) {
        if (type == ResolveConflictsCommand.AutoResolveType.TakeTheirs) {
            acceptTheirs(conflicts);
        } else if (type == ResolveConflictsCommand.AutoResolveType.KeepYours) {
            acceptYours(conflicts);
        }
    }

    public void acceptYours(final @NotNull List<String> conflicts) {
        // treat just like skip
        skip(conflicts);
    }

    public void acceptTheirs(final @NotNull List<String> conflicts) {
        for (final String conflict : conflicts) {
            if (myUpdatedFiles != null) {
                //TODO create version number
                myUpdatedFiles.getGroupById(FileGroup.UPDATED_ID).add(conflict, TFSVcs.getKey(), null);
            }
        }
    }

    public void skip(final @NotNull List<String> conflicts) {
        for (final String conflict : conflicts) {
            if (myUpdatedFiles != null) {
                // Jetbrains used null for version number in this case
                myUpdatedFiles.getGroupById(FileGroup.SKIPPED_ID).add(conflict, TFSVcs.getKey(), null);
            }
        }
    }

//  public Collection<Conflict> getConflicts() {
//    return Collections.unmodifiableCollection(myConflict2Workspace.keySet());
//  }

// TODO: not checking whether or not merge option is available, it's always available
//  public static boolean canMerge(final @NotNull Conflict conflict) {
//    if (conflict.getSrclitem() == null) {
//      return false;
//    }
//
//    final ChangeTypeMask yourChange = new ChangeTypeMask(conflict.getYchg());
//    final ChangeTypeMask yourLocalChange = new ChangeTypeMask(conflict.getYlchg());
//    final ChangeTypeMask baseChange = new ChangeTypeMask(conflict.getBchg());
//
//    boolean isNamespaceConflict =
//      ((conflict.getCtype().equals(ConflictType.Get)) || (conflict.getCtype().equals(ConflictType.Checkin))) && conflict.getIsnamecflict();
//    if (!isNamespaceConflict) {
//      boolean yourRenamedOrModified = yourChange.containsAny(ChangeType_type0.Rename, ChangeType_type0.Edit);
//      boolean baseRenamedOrModified = baseChange.containsAny(ChangeType_type0.Rename, ChangeType_type0.Edit);
//      if (yourRenamedOrModified && baseRenamedOrModified) {
//        return true;
//      }
//    }
//    if ((conflict.getYtype() != ItemType.Folder) && !isNamespaceConflict) {
//      if (conflict.getCtype().equals(ConflictType.Merge) && baseChange.contains(ChangeType_type0.Edit)) {
//        if (yourLocalChange.contains(ChangeType_type0.Edit)) {
//          return true;
//        }
//        if (conflict.getIsforced()) {
//          return true;
//        }
//        if ((conflict.getTlmver() != conflict.getBver()) || (conflict.getYlmver() != conflict.getYver())) {
//          return true;
//        }
//      }
//    }
//    return false;
//  }

//  private void conflictResolved(final Conflict conflict, final Resolution resolution, final @NotNull String newLocalPath, boolean sendPath)
//    throws TfsException, VcsException {
//    WorkspaceInfo workspace = myConflict2Workspace.get(conflict);
//
//    VersionControlServer.ResolveConflictParams resolveConflictParams =
//      new VersionControlServer.ResolveConflictParams(conflict.getCid(), resolution, LockLevel.Unchanged, -2,
//                                                     sendPath ? VersionControlPath.toTfsRepresentation(newLocalPath) : null);
//
//    ResolveResponse response =
//      workspace.getServer().getVCS().resolveConflict(workspace.getName(), workspace.getOwnerName(), resolveConflictParams, myProject,
//                                                     TFSBundle.message("reporting.conflict.resolved"));
//
//    final UpdatedFiles updatedFiles = resolution != Resolution.AcceptMerge ? myUpdatedFiles : null;
//
//    if (response.getResolveResult().getGetOperation() != null) {
//      ApplyGetOperations.DownloadMode downloadMode =
//        resolution == Resolution.AcceptTheirs ? ApplyGetOperations.DownloadMode.FORCE : ApplyGetOperations.DownloadMode.MERGE;
//
//      final Collection<VcsException> applyErrors = ApplyGetOperations
//        .execute(myProject, workspace, Arrays.asList(response.getResolveResult().getGetOperation()), ApplyProgress.EMPTY, updatedFiles,
//                 downloadMode);
//      if (!applyErrors.isEmpty()) {
//        throw TfsUtil.collectExceptions(applyErrors);
//      }
//    }
//
//    if (response.getUndoOperations().getGetOperation() != null) {
//      final Collection<VcsException> applyErrors = ApplyGetOperations
//        .execute(myProject, workspace, Arrays.asList(response.getUndoOperations().getGetOperation()), ApplyProgress.EMPTY, updatedFiles,
//                 ApplyGetOperations.DownloadMode.FORCE);
//      if (!applyErrors.isEmpty()) {
//        throw TfsUtil.collectExceptions(applyErrors);
//      }
//    }
//
//    if (resolution == Resolution.AcceptMerge) {
//      if (myUpdatedFiles != null) {
//        myUpdatedFiles.getGroupById(FileGroup.MERGED_ID).add(newLocalPath, TFSVcs.getKey(), null);
//      }
//    }
//    myConflict2Workspace.remove(conflict);
//  }

//  private static boolean isNameConflict(final @NotNull Conflict conflict) {
//    final ChangeTypeMask yourChange = new ChangeTypeMask(conflict.getYchg());
//    final ChangeTypeMask baseChange = new ChangeTypeMask(conflict.getBchg());
//    return yourChange.contains(ChangeType_type0.Rename) || baseChange.contains(ChangeType_type0.Rename);
//  }
//

    private static boolean isContentConflict(final @NotNull List<ServerStatusType> types) {

        return types.contains(ServerStatusType.EDIT);
    }

    /**
     * Resolve the conflicts based on auto resolve type and then refresh the table model to update the list of conflicts
     *
     * @param conflicts
     * @param type
     */
    public void acceptChange(final List<String> conflicts, final ResolveConflictsCommand.AutoResolveType type, final ResolveConflictsModel model) {
        //   logger.info(String.format("Accepting changes to %s for file %s", type.name(), Arrays.toString(conflicts.toArray())));
        final Task.Backgroundable loadConflictsTask = new Task.Backgroundable(myProject, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_RESOLVING_PROGRESS_BAR),
                true, PerformInBackgroundOption.DEAF) {

            @Override
            public void run(@NotNull final ProgressIndicator progressIndicator) {
                progressIndicator.setText(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_RESOLVING_STATUS));
                try {
                    final List<String> resolved = CommandUtils.resolveConflicts(TFSVcs.getInstance(myProject).getServerContext(false), conflicts, type);
                    acceptChanges(resolved, type);

                    // update status bar
                    IdeaHelper.setProgress(progressIndicator, 0.5, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_RESOLVING_REFRESH));

                    // refresh conflicts so resolved ones are removed
                    findConflicts(model);
                } catch (Exception e) {
                    logger.error("Error while handling merge resolution", e);
                    // TODO: handle if tool fails
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
    public void findConflicts(final ResolveConflictsModel model) {
        for (final String updatePath : updateRoots) {
            final Command<List<String>> conflictsCommand = new FindConflictsCommand(TFSVcs.getInstance(myProject).getServerContext(false), updatePath);
            try {
                final List<String> conflicts = conflictsCommand.runSynchronously();
                Collections.sort(conflicts);
                IdeaHelper.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        model.getConflictsTableModel().setConflicts(conflicts);
                        model.setLoading(false);
                    }
                });
            } catch (Exception e) {
                logger.error("Error while finding conflicts in the workspace", e);
                // TODO: handle if tool fails
            }
        }
    }
}
