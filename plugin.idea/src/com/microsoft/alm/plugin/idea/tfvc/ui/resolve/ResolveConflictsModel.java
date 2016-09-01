// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.resolve;

import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.commands.Command;
import com.microsoft.alm.plugin.external.commands.FindConflictsCommand;
import com.microsoft.alm.plugin.external.commands.ResolveConflictsCommand;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.AbstractModel;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.conflicts.ResolveConflictHelper;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Model for resolving conflicts in a workspace
 */
public class ResolveConflictsModel extends AbstractModel {
    private static final Logger logger = LoggerFactory.getLogger(ResolveConflictsModel.class);

    public final static String PROP_LOADING = "loading";

    private final Project project;
    private final ServerContext context;
    private final List<String> filePaths;
    private final ConflictsTableModel conflictsTableModel;
    private final ResolveConflictHelper conflictHelper;

    private boolean isLoading = false;

    public ResolveConflictsModel(final Project project, final ServerContext context, final List<String> filePaths,
                                 final ResolveConflictHelper conflictHelper) {
        this.project = project;
        this.context = context;
        this.filePaths = filePaths;
        this.conflictHelper = conflictHelper;

        this.conflictsTableModel = new ConflictsTableModel();

        loadConflicts();
    }

    /**
     * Set loading for UI. Special model changes need to be made to displaying loading message but nothing needs to be
     * done in the case of not loading. The loaded conflicts will take care of removing the loading message. The
     * notification still needs to take place though to change the text color of the table.
     *
     * @param isLoading
     */
    private void setLoading(final boolean isLoading) {
        if (this.isLoading != isLoading) {
            if (isLoading) {
                conflictsTableModel.setLoading();
            }
            // no need to clear anything in the case of not loading because the contents being loaded takes care of it
            this.isLoading = isLoading;
            setChangedAndNotify(PROP_LOADING);
        }
    }

    public boolean isLoading() {
        return isLoading;
    }

    /**
     * Load the conflicts into the table model
     */
    private void loadConflicts() {
        setLoading(true);

        final Task.Backgroundable loadConflictsTask = new Task.Backgroundable(project, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_LOADING_PROGRESS_BAR),
                true, PerformInBackgroundOption.DEAF) {

            @Override
            public void run(@NotNull final ProgressIndicator progressIndicator) {
                logger.debug("Loading conflicts into the table");
                progressIndicator.setText(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_LOADING_PROGRESS_BAR));
                findConflicts();
            }
        };
        loadConflictsTask.queue();
    }

    /**
     * Accept your local changes
     *
     * @param rows
     */
    public void acceptYours(final int[] rows) {
        acceptChange(rows, ResolveConflictsCommand.AutoResolveType.KeepYours);
    }

    /**
     * Accept server changes
     *
     * @param rows
     */
    public void acceptTheirs(final int[] rows) {
        acceptChange(rows, ResolveConflictsCommand.AutoResolveType.TakeTheirs);
    }

    /**
     * Resolve the conflicts based on auto resolve type and then refresh the table model to update the list of conflicts
     *
     * @param rows
     * @param type
     */
    private void acceptChange(final int[] rows, final ResolveConflictsCommand.AutoResolveType type) {
        final List<String> conflicts = getSelectedConflicts(rows);
        logger.info(String.format("Accepting changes to %s for file %s", type.name(), Arrays.toString(conflicts.toArray())));
        final Task.Backgroundable loadConflictsTask = new Task.Backgroundable(project, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_RESOLVING_PROGRESS_BAR),
                true, PerformInBackgroundOption.DEAF) {

            @Override
            public void run(@NotNull final ProgressIndicator progressIndicator) {
                progressIndicator.setText(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_RESOLVING_STATUS));
                final Command<List<String>> conflictsCommand = new ResolveConflictsCommand(context, conflicts, type);
                try {
                    List<String> resolved = (conflictsCommand.runSynchronously());
                    conflictHelper.acceptChanges(resolved, type);

                    // update status bar
                    progressIndicator.setText(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_RESOLVING_REFRESH));
                    progressIndicator.setFraction(.5);

                    // refresh conflicts so resolved ones are removed
                    findConflicts();
                } catch (Exception e) {
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
    private void findConflicts() {
        for (final String updatePath : filePaths) {
            final Command<List<String>> conflictsCommand = new FindConflictsCommand(context, updatePath);
            try {
                final List<String> conflicts = conflictsCommand.runSynchronously();
                Collections.sort(conflicts);
                IdeaHelper.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        conflictsTableModel.setConflicts(conflicts);
                        setLoading(false);
                    }
                });
            } catch (Exception e) {
                // TODO: handle if tool fails
            }
        }
    }

    public void processSkippedConflicts() {
        conflictHelper.skip(conflictsTableModel.getMyConflicts());
    }

    public ConflictsTableModel getConflictsTableModel() {
        return conflictsTableModel;
    }

    /**
     * Find the values of the selected rows
     *
     * @param rows
     * @return
     */
    private List<String> getSelectedConflicts(final int[] rows) {
        final List<String> selectedConflicts = new ArrayList<String>();

        for (final int index : rows) {
            // only 1 column so only need to worry about rows
            selectedConflicts.add(conflictsTableModel.getValueAt(index, 0).toString());
        }
        return selectedConflicts;
    }
}