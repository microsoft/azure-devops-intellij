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
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.AbstractModel;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Model for resolving conflicts in a workspace
 */
public class ResolveConflictsModel extends AbstractModel {
    public final static String PROP_LOADING = "loading";

    private final Project project;
    private final ServerContext context;
    private final List<String> filePaths;
    private final ConflictsTableModel conflictsTableModel;

    private boolean isLoading = false;

    public ResolveConflictsModel(final Project project, final ServerContext context, final List<String> filePaths) {
        this.project = project;
        this.context = context;
        this.filePaths = filePaths;
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

    private void loadConflicts() {
        setLoading(true);

        final Task.Backgroundable loadConflictsTask = new Task.Backgroundable(project, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_LOADING_PROGRESS_BAR),
                true, PerformInBackgroundOption.DEAF) {

            @Override
            public void run(@NotNull final ProgressIndicator progressIndicator) {
                progressIndicator.setText(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_LOADING_PROGRESS_BAR));
                // get context from manager, and store in active context
                for (final String updatePath : filePaths) {
                    Command<List<String>> conflictsCommand = new FindConflictsCommand(context, updatePath);
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
        };
        loadConflictsTask.queue();
    }

    public ConflictsTableModel getConflictsTableModel() {
        return conflictsTableModel;
    }
}