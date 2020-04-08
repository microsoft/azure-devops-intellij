// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.management;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.vcsUtil.VcsRunnable;
import com.intellij.vcsUtil.VcsUtil;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.external.exceptions.ToolAuthenticationException;
import com.microsoft.alm.plugin.external.models.Server;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.external.utils.WorkspaceHelper;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.AbstractModel;
import com.microsoft.alm.plugin.idea.common.ui.common.treetable.ContentProvider;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import com.microsoft.alm.plugin.idea.common.utils.VcsHelper;
import com.microsoft.alm.plugin.idea.tfvc.ui.ProxySettingsDialog;
import com.microsoft.alm.plugin.idea.tfvc.ui.workspace.WorkspaceController;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
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

    public void reloadWorkspacesWithProgress(final Server selectedServer) {
        logger.info("Reloading workspaces for server " + selectedServer.getName());

        try {
            VcsUtil.runVcsProcessWithProgress(new VcsRunnable() {
                public void run() throws VcsException {
                    reloadWorkspaces(selectedServer);
                }
            }, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_RELOAD_MSG, selectedServer.getName()), true, project);
        } catch (VcsException e) {
            logger.warn("Exception while trying to reload workspaces", e);
            Messages.showErrorDialog(project, e.getMessage(),
                    TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_RELOAD_ERROR_TITLE));
        } finally {
            // always refresh list
            setChangedAndNotify(REFRESH_SERVER);
        }
    }

    /**
     * Reload the workspaces for the given server
     *
     * @param selectedServer
     * @throws VcsException
     */
    protected void reloadWorkspaces(final Server selectedServer) throws VcsException {
        try {
            // get auth info for the server
            final AuthenticationInfo authInfo = ServerContextManager.getInstance().getAuthenticationInfo(selectedServer.getName(), true);
            if (authInfo != null) {
                // will refresh the cache which populates the menu
                CommandUtils.refreshWorkspacesForServer(authInfo, selectedServer.getName());
            } else {
                logger.warn("Couldn't get auth info so aborting reload command");
                throw new RuntimeException(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_RELOAD_ERROR_MSG,
                        selectedServer.getName()));
            }
        } catch (RuntimeException e) {
            throw new VcsException(e);
        }
    }

    public void deleteWorkspaceWithProgress(final Workspace selectedWorkspace) {
        logger.info("Deleting workspace " + selectedWorkspace.getName());
        // confirm with the user the deletion
        if (Messages.showYesNoDialog(project, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_DELETE_CONFIRM_MSG, selectedWorkspace.getName()),
                TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_DELETE_CONFIRM_TITLE), Messages.getWarningIcon()) != Messages.YES) {
            logger.info("User cancelled workspace delete");
            return;
        }

        try {
            VcsUtil.runVcsProcessWithProgress(new VcsRunnable() {
                public void run() throws VcsException {
                    deleteWorkspace(selectedWorkspace);
                }
            }, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_DELETE_MSG, selectedWorkspace.getName()), true, project);
        } catch (VcsException e) {
            logger.warn("Exception while trying to delete workspace", e);
            Messages.showErrorDialog(project, e.getMessage(),
                    TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_DELETE_ERROR_TITLE));
        } finally {
            // always refresh list
            setChangedAndNotify(REFRESH_WORKSPACE);
        }
    }

    /**
     * Delete the given workspace
     *
     * @param selectedWorkspace
     */
    protected void deleteWorkspace(final Workspace selectedWorkspace) throws VcsException {
        try {
            final Workspace workspace = getPartialWorkspace(selectedWorkspace.getServerDisplayName(), selectedWorkspace.getName());
            if (workspace != null) {
                final String projectName = VcsHelper.getTeamProjectFromTfvcServerPath(
                        workspace.getMappings().size() > 0 ? workspace.getMappings().get(0).getServerPath() : null);

                final ServerContext context = ServerContextManager.getInstance().createContextFromTfvcServerUrl(workspace.getServerUri(), projectName, true);
                CommandUtils.deleteWorkspace(context, selectedWorkspace.getName());
            } else {
                logger.warn("Couldn't find partial workspace so aborting delete command");
                throw new RuntimeException(TfPluginBundle.message(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_DELETE_ERROR_MSG,
                        selectedWorkspace.getName())));
            }
        } catch (RuntimeException e) {
            throw new VcsException(e);
        }
    }

    public void editWorkspaceWithProgress(final Workspace selectedWorkspace, final Runnable update) {
        logger.info("Editing workspace " + selectedWorkspace.getName());

        try {
            VcsUtil.runVcsProcessWithProgress(new VcsRunnable() {
                public void run() throws VcsException {
                    editWorkspace(selectedWorkspace, update);
                }
            }, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_EDIT_MSG, selectedWorkspace.getName()), true, project);
        } catch (VcsException e) {
            logger.warn("Exception while trying to edit workspace", e);
            Messages.showErrorDialog(project, e.getMessage(),
                    TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_EDIT_ERROR_TITLE));
        }
        // no refresh needs to be called here because we pass it to the edit workspace dialog to run after the save is complete
    }

    /**
     * Open the edit workspace dialog with given workspace info
     *
     * @param selectedWorkspace
     * @param update
     */
    protected void editWorkspace(final Workspace selectedWorkspace, final Runnable update) throws VcsException {
        try {
            URI serverUri = selectedWorkspace.getServerUri();
            String serverName = selectedWorkspace.getServerDisplayName();
            final AuthenticationInfo authInfo = ServerContextManager.getInstance().getAuthenticationInfo(serverUri, true);
            final Workspace detailedWorkspace = CommandUtils.getDetailedWorkspace(serverName, selectedWorkspace.getName(), authInfo);
            if (detailedWorkspace != null) {
                final String projectName = VcsHelper.getTeamProjectFromTfvcServerPath(
                        detailedWorkspace.getMappings().size() > 0 ? detailedWorkspace.getMappings().get(0).getServerPath() : null);
                final ServerContext context = ServerContextManager.getInstance().createContextFromTfvcServerUrl(serverUri, projectName, true);
                // use info from the 2 incomplete workspace objects to create a complete one
                final Workspace workspace = new Workspace(serverName, selectedWorkspace.getName(), selectedWorkspace.getComputer(),
                        selectedWorkspace.getOwner(), selectedWorkspace.getComment(), detailedWorkspace.getMappings(), detailedWorkspace.getLocation());

                if (context == null) {
                    logger.warn("Can't edit workspace because context is null");
                    throw new RuntimeException(TfPluginBundle.message(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_EDIT_ERROR_MSG,
                            selectedWorkspace.getName())));
                }

                IdeaHelper.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        // open edit dialog
                        final WorkspaceController controller = new WorkspaceController(project, context, workspace);
                        if (controller.showModalDialog(false)) {
                            controller.saveWorkspace(StringUtils.EMPTY, false, update);
                        }
                    }
                }, true);
            } else {
                logger.warn("Couldn't find partial workspace so aborting edit command");
                throw new RuntimeException(TfPluginBundle.message(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_EDIT_ERROR_MSG,
                        selectedWorkspace.getName())));
            }
        } catch (RuntimeException e) {
            throw new VcsException(e);
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

    /**
     * Get the partially populated workspace object
     *
     * @param serverName
     * @param workspaceName
     * @return
     */
    protected Workspace getPartialWorkspace(final String serverName, final String workspaceName) {
        try {
            return CommandUtils.getPartialWorkspace(serverName, workspaceName);
        } catch (final ToolAuthenticationException e) {
            // if auth error occurs it most likely is because the workspace is a server workspace so pass credentials with the call
            logger.warn("Authentication failed while trying to get the partial workspace. Trying again with credentials");
            final AuthenticationInfo authInfo = ServerContextManager.getInstance().getAuthenticationInfo(serverName, true);
            return CommandUtils.getPartialWorkspace(serverName, workspaceName, authInfo);
        }
    }
}