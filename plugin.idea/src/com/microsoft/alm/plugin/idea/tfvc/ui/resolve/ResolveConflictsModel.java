// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.resolve;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.vcsUtil.VcsRunnable;
import com.intellij.vcsUtil.VcsUtil;
import com.microsoft.alm.plugin.external.commands.ResolveConflictsCommand;
import com.microsoft.alm.plugin.external.models.Conflict;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.ModelValidationInfo;
import com.microsoft.alm.plugin.idea.common.ui.common.PageModelImpl;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.conflicts.ResolveConflictHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Model for resolving conflicts in a workspace
 */
public class ResolveConflictsModel extends PageModelImpl {
    private static final Logger logger = LoggerFactory.getLogger(ResolveConflictsModel.class);

    private final Project project;
    private final ConflictsTableModel conflictsTableModel;
    private final ResolveConflictHelper conflictHelper;

    public ResolveConflictsModel(final Project project, final ResolveConflictHelper conflictHelper) {
        this(project, conflictHelper, new ConflictsTableModel());
    }

    public ResolveConflictsModel(final Project project, final ResolveConflictHelper conflictHelper, final ConflictsTableModel conflictsTableModel) {
        this.project = project;
        this.conflictHelper = conflictHelper;
        this.conflictsTableModel = conflictsTableModel;

    }

    /**
     * Load the conflicts into the table model
     */
    public void loadConflicts() {
        logger.debug("Loading conflicts into the table");
        try {
            final VcsRunnable resolveRunnable = new VcsRunnable() {
                public void run() throws VcsException {
                    IdeaHelper.setProgress(ProgressManager.getInstance().getProgressIndicator(), 0.1, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_LOADING_CONFLICTS));
                    conflictHelper.findConflicts(ResolveConflictsModel.this);
                }
            };
            VcsUtil.runVcsProcessWithProgress(resolveRunnable, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_LOADING_PROGRESS_BAR), false, project);
        } catch (VcsException e) {
            logger.error("Error while loading conflicts: " + e.getMessage());
            addError(ModelValidationInfo.createWithMessage(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_LOAD_ERROR)));
        }
    }

    /**
     * Accept your local changes
     *
     * @param rows
     */
    public void acceptYours(final int[] rows) {
        logger.info("Accepting yours for " + rows.length + " conflicts");
        conflictHelper.acceptChangeAsync(getSelectedConflicts(rows), ResolveConflictsCommand.AutoResolveType.KeepYours, this);
    }

    /**
     * Accept server changes
     *
     * @param rows
     */
    public void acceptTheirs(final int[] rows) {
        logger.info("Accepting theirs for " + rows.length + " conflicts");
        conflictHelper.acceptChangeAsync(getSelectedConflicts(rows), ResolveConflictsCommand.AutoResolveType.TakeTheirs, this);
    }

    public void merge(final int[] rows) {
        final List<Conflict> conflicts = getSelectedConflicts(rows);
        logger.info("Starting merge...");
        for (final Conflict conflict : conflicts) {
            try {
                conflictHelper.acceptMerge(conflict, this);
            } catch (VcsException e) {
                logger.error("Error while merging conflicts: " + e.getMessage());
                addError(ModelValidationInfo.createWithMessage(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_MERGE_ERROR, conflict.getLocalPath(), e.getMessage())));
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
    private List<Conflict> getSelectedConflicts(final int[] rows) {
        final List<Conflict> selectedConflicts = new ArrayList<Conflict>();

        for (final int index : rows) {
            selectedConflicts.add(conflictsTableModel.getMyConflicts().get(index));
        }
        return selectedConflicts;
    }
}