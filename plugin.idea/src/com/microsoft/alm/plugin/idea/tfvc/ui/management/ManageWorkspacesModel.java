// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.management;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.external.models.Server;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.external.utils.WorkspaceHelper;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.AbstractModel;
import com.microsoft.alm.plugin.idea.common.ui.common.treetable.ContentProvider;
import com.microsoft.alm.plugin.idea.common.utils.VcsHelper;
import com.microsoft.alm.plugin.idea.tfvc.ui.ProxySettingsDialog;
import com.microsoft.alm.plugin.idea.tfvc.ui.workspace.WorkspaceController;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Model for the Manage Workspace dialog
 * <p>
 * Note: The project passed in here is the currently open project which can be different than the workspace/server selected
 * Do no use the project object to find more info about the workspace because it will be incorrect
 */
public class ManageWorkspacesModel extends AbstractModel {
    private final Logger logger = LoggerFactory.getLogger(ManageWorkspacesModel.class);

    public static final String REFRESH_SERVER = "refreshServer";
    public static final String REFRESH_WORKSPACE = "refreshWorkspace";

    private final Project project;
    private final ContentProvider<Object> contentProvider;

    public ManageWorkspacesModel(final Project project) {
        this.project = project;
        this.contentProvider = new ServerWorkspaceContentProvider();
    }

    public ContentProvider<Object> getContextProvider() {
        return contentProvider;
    }

    public void reloadWorkspaces(final Server selectedServer) {
        logger.info("Reloading workspaces for server " + selectedServer.getName());
        ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
            public void run() {
                ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
                // server always has at least 1 workspace with it or else it wouldn't be listed
                final Workspace workspace = CommandUtils.getPartialWorkspace(selectedServer.getName(), selectedServer.getWorkspaces().get(0).getName());
                if (workspace != null) {
                    final String projectName = VcsHelper.getTeamProjectFromTfvcServerPath(
                            workspace.getMappings().size() > 0 ? workspace.getMappings().get(0).getServerPath() : null);

                    final ServerContext context = ServerContextManager.getInstance().createContextFromTfvcServerUrl(workspace.getServer(), projectName, true);
                    // will refresh the cache which populates the menu
                    CommandUtils.refreshWorkspacesForServer(context);
                }
            }
        }, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_RELOAD_MSG, selectedServer.getName()), false, project);
        setChangedAndNotify(REFRESH_SERVER);
    }

    /**
     * Delete the given workspace
     *
     * @param selectedWorkspace
     */
    public void deleteWorkspace(final Workspace selectedWorkspace) {
        logger.info("Deleting workspace " + selectedWorkspace.getName());
        // confirm with the user the deletion
        if (Messages.showYesNoDialog(project, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_DELETE_CONFIRM_MSG, selectedWorkspace.getName()),
                TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_DELETE_CONFIRM_TITLE), Messages.getWarningIcon()) != Messages.YES) {
            logger.info("User cancelled workspace delete");
            return;
        }

        ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
            public void run() {
                ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
                final Workspace workspace = CommandUtils.getPartialWorkspace(selectedWorkspace.getServer(), selectedWorkspace.getName());
                if (workspace != null) {
                    final String projectName = VcsHelper.getTeamProjectFromTfvcServerPath(
                            workspace.getMappings().size() > 0 ? workspace.getMappings().get(0).getServerPath() : null);

                    final ServerContext context = ServerContextManager.getInstance().createContextFromTfvcServerUrl(workspace.getServer(), projectName, true);
                    CommandUtils.deleteWorkspace(context, selectedWorkspace.getName());
                }
            }
        }, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_DELETE_MSG, selectedWorkspace.getName()), false, project);
        setChangedAndNotify(REFRESH_WORKSPACE);
    }

    /**
     * Open the edit workspace dialog with given workspace info
     *
     * @param selectedWorkspace
     * @param update
     */
    public void editWorkspace(final Workspace selectedWorkspace, final Runnable update) {
        logger.info("Editing workspace " + selectedWorkspace.getName());
        // only retrieving one context and workspace
        final List<ServerContext> contexts = new ArrayList<ServerContext>(1);
        final List<Workspace> workspaces = new ArrayList<Workspace>(1);

        ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
            public void run() {
                ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
                final Workspace partialWorkspace = CommandUtils.getPartialWorkspace(selectedWorkspace.getServer(), selectedWorkspace.getName());
                if (partialWorkspace != null) {
                    final String projectName = VcsHelper.getTeamProjectFromTfvcServerPath(
                            partialWorkspace.getMappings().size() > 0 ? partialWorkspace.getMappings().get(0).getServerPath() : null);
                    contexts.add(ServerContextManager.getInstance().createContextFromTfvcServerUrl(partialWorkspace.getServer(), projectName, true));
                    // use info from the 2 incomplete workspace objects to create a complete one
                    workspaces.add(new Workspace(selectedWorkspace.getServer(), selectedWorkspace.getName(), selectedWorkspace.getComputer(),
                            selectedWorkspace.getOwner(), selectedWorkspace.getComment(), partialWorkspace.getMappings()));
                }
            }
        }, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_EDIT_MSG, selectedWorkspace.getName()), false, project);

        // if no context is found then show error
        if (contexts.size() < 1 || contexts.get(0) == null) {
            Messages.showErrorDialog(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_EDIT_ERROR_MSG, selectedWorkspace.getName()),
                    TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_EDIT_ERROR_TITLE));
            logger.warn("Did not find the context for the workspace to edit");
        } else if (workspaces.size() < 1 || workspaces.get(0) == null) {
            // no workspace found to edit
            Messages.showErrorDialog(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_EDIT_ERROR_MSG, selectedWorkspace.getName()),
                    TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_EDIT_ERROR_TITLE));
            logger.warn("Did not find the workspace to edit");
        } else {
            // open edit dialog
            final WorkspaceController controller = new WorkspaceController(project, contexts.get(0), workspaces.get(0));
            if (controller.showModalDialog(false)) {
                controller.saveWorkspace(StringUtils.EMPTY, false, update);
            }
        }
    }

    /**
     * Open the proxy dialog for editing
     *
     * @param selectedServer
     */
    public void editProxy(final Server selectedServer) {
        logger.info("Editing proxy for " + selectedServer.getName());
        final String currentProxy = getProxy(selectedServer);
        final ProxySettingsDialog dialog = new ProxySettingsDialog(project, selectedServer.getName(), currentProxy);
        if (dialog.showAndGet()) {
            final String newProxy = dialog.getProxyUri();
            setProxy(selectedServer, newProxy);
            setChangedAndNotify(REFRESH_SERVER);
        }
    }

    /**
     * Get the proxy settings for the given server
     *
     * @param server
     * @return
     */
    private String getProxy(final Server server) {
        return WorkspaceHelper.getProxyServer(server.getName());
    }

    /**
     * Save the proxy settings
     *
     * @param server
     * @param newProxy
     */
    private void setProxy(final Server server, final String newProxy) {
        WorkspaceHelper.setProxyServer(server.getName(), newProxy);
    }

    /**
     * The tree content provider that populates the tree's servers and workspaces
     */
    protected class ServerWorkspaceContentProvider implements ContentProvider {
        public Collection<?> getRoots() {
            // pass no context so that all servers saved in the cache are found
            final List<Server> servers = new ArrayList<Server>(CommandUtils.getAllWorkspaces(null));
            Collections.sort(servers, new Comparator<Server>() {
                public int compare(final Server s1, final Server s2) {
                    return s1.getName().compareTo(s2.getName());
                }
            });
            return servers;
        }

        public Collection<?> getChildren(final @NotNull Object parent) {
            if (parent instanceof Server) {
                final List<Workspace> workspaces = new ArrayList<Workspace>(((Server) parent).getWorkspaces());

                Collections.sort(workspaces, new Comparator<Workspace>() {
                    public int compare(final Workspace w1, final Workspace w2) {
                        return w1.getName().compareTo(w2.getName());
                    }
                });
                return workspaces;
            }
            return Collections.emptyList();
        }
    }
}