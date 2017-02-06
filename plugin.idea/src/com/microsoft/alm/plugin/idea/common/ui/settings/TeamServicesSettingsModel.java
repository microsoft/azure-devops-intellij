// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.settings;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.authentication.TfsAuthenticationProvider;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.AbstractModel;
import com.microsoft.alm.plugin.idea.common.ui.common.ServerContextTableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ListSelectionModel;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * Model for the general settings page
 */
public class TeamServicesSettingsModel extends AbstractModel {
    private static final Logger logger = LoggerFactory.getLogger(TeamServicesSettingsModel.class);

    private final Project project;
    private final ServerContextTableModel tableModel;
    private final List<ServerContext> deleteContexts;

    public TeamServicesSettingsModel(final Project project) {
        ArgumentHelper.checkNotNull(project, "project");
        this.project = project;
        this.tableModel = new ServerContextTableModel(ServerContextTableModel.GENERAL_COLUMNS, ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.deleteContexts = new ArrayList<ServerContext>();
    }

    /**
     * Check if any changes have been made to the page
     *
     * @return
     */
    public boolean isModified() {
        return !deleteContexts.isEmpty();
    }

    /**
     * Load the settings for the page
     */
    public void loadSettings() {
        populateContextTable();
    }

    /**
     * Finds the saved server contexts, filters them for ones that are repo specific, and populates the table with them
     */
    private void populateContextTable() {
        final List<ServerContext> serverContexts = new ArrayList<ServerContext>(ServerContextManager.getInstance().getAllServerContexts());
        final Hashtable contextTable = new Hashtable<String, ServerContext>(serverContexts.size());
        for (final ServerContext context : serverContexts) {
            final String repoName;
            final String accountUrl;

            // find only contexts that have a repo
            if (context.getGitRepository() != null) {
                repoName = context.getGitRepository().getName();
            } else if (context.getTeamProjectReference() != null) {
                repoName = context.getTeamProjectReference().getName();
            } else {
                continue;
            }

            // find the URL with the repo
            if (!context.getUri().toString().equals(TfsAuthenticationProvider.TFS_LAST_USED_URL)) {
                accountUrl = context.getUri().toString();
            } else {
                continue;
            }

            final String key = repoName.concat(accountUrl).toLowerCase();
            if (!contextTable.containsKey(key) && !deleteContexts.contains(context)) {
                contextTable.put(key, context);
            }
        }
        tableModel.clearRows();
        tableModel.addServerContexts(new ArrayList<ServerContext>(contextTable.values()));
    }

    /**
     * Apply actually deletes the contexts that the user selected to delete
     */
    public void apply() {
        for (final ServerContext context : deleteContexts) {
            ServerContextManager.getInstance().remove(context.getKey());
        }
        deleteContexts.clear();
    }

    /**
     * Undo the changes made to the settings
     */
    public void reset() {
        deleteContexts.clear();
        populateContextTable();
    }

    /**
     * Deletes the passwords/contexts from the table only but not permanently
     */
    public void deletePasswords() {
        final ListSelectionModel selectionModel = getTableSelectionModel();
        if (!selectionModel.isSelectionEmpty()) {
            if (Messages.showYesNoDialog(project, TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_MSG), TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_TITLE), Messages.getQuestionIcon()) == Messages.YES) {
                // only delete contexts from the table at the moment and not for good (when Apply is used that is when we actually delete the contexts in deleteContexts)
                deleteContexts.addAll(tableModel.getSelectedContexts());
                populateContextTable();
            }
        } else {
            Messages.showWarningDialog(project, TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_NO_ROWS_SELECTED), TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_TITLE));
        }
    }

    /**
     * Update the auth info for each context selected
     */
    public void updatePasswords() {
        final ListSelectionModel selectionModel = getTableSelectionModel();
        if (!selectionModel.isSelectionEmpty()) {
            if (Messages.showYesNoDialog(project, TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_MSG), TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_TITLE), Messages.getQuestionIcon()) == Messages.YES) {
                final List<ServerContext> contexts = tableModel.getSelectedContexts();
                final Task.Backgroundable updateAuthTask = new Task.Backgroundable(project, TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_UPDATING)) {
                    @Override
                    public void run(final ProgressIndicator indicator) {
                        logger.info("Updating passwords for user. Selected: " + contexts.size());
                        ServerContextManager.getInstance().updateServerContextsAuthInfo(contexts);
                        populateContextTable();
                    }
                };
                updateAuthTask.queue();
            }
        } else {
            Messages.showWarningDialog(project, TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_NO_ROWS_SELECTED), TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_TITLE));
        }
    }

    public ServerContextTableModel getTableModel() {
        return tableModel;
    }

    public ListSelectionModel getTableSelectionModel() {
        return tableModel.getSelectionModel();
    }

    @VisibleForTesting
    protected List<ServerContext> getDeleteContexts() {
        return deleteContexts;
    }

    @VisibleForTesting
    protected void setDeleteContexts(final List<ServerContext> deleteContexts) {
        this.deleteContexts.clear();
        this.deleteContexts.addAll(deleteContexts);
    }
}
