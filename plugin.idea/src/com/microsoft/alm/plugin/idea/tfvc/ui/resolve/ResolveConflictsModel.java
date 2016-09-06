// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.resolve;

import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.microsoft.alm.plugin.external.commands.ResolveConflictsCommand;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.AbstractModel;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.conflicts.ResolveConflictHelper;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Model for resolving conflicts in a workspace
 */
public class ResolveConflictsModel extends AbstractModel {
    private static final Logger logger = LoggerFactory.getLogger(ResolveConflictsModel.class);

    public final static String PROP_LOADING = "loading";

    private final Project project;
    private final ConflictsTableModel conflictsTableModel;
    private final ResolveConflictHelper conflictHelper;

    private boolean isLoading = false;

    public ResolveConflictsModel(final Project project, final ResolveConflictHelper conflictHelper) {
        this.project = project;
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
    public void setLoading(final boolean isLoading) {
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
                conflictHelper.findConflicts(ResolveConflictsModel.this);
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
        conflictHelper.acceptChange(getSelectedConflicts(rows), ResolveConflictsCommand.AutoResolveType.KeepYours, this);
    }

    /**
     * Accept server changes
     *
     * @param rows
     */
    public void acceptTheirs(final int[] rows) {
        conflictHelper.acceptChange(getSelectedConflicts(rows), ResolveConflictsCommand.AutoResolveType.TakeTheirs, this);
    }

    public void merge(final int[] rows) {
        final List<String> conflicts = getSelectedConflicts(rows);
        logger.debug("Starting merge...");
        for (final String conflict : conflicts) {
            try {
                conflictHelper.acceptMerge(conflict, this);
            } catch (VcsException e) {
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
            selectedConflicts.add(conflictsTableModel.getMyConflicts().get(index));
        }
        return selectedConflicts;
    }
}