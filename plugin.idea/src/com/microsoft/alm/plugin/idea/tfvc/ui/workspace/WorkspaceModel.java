// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.workspace;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.context.RepositoryContext;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.AbstractModel;
import com.microsoft.alm.plugin.idea.common.ui.common.ModelValidationInfo;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import com.microsoft.alm.plugin.idea.common.utils.VcsHelper;
import com.microsoft.alm.plugin.operations.OperationExecutor;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotAuthorizedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WorkspaceModel extends AbstractModel {
    private final Logger logger = LoggerFactory.getLogger(WorkspaceModel.class);

    public static final String PROP_NAME = "name";
    public static final String PROP_COMPUTER = "computer";
    public static final String PROP_OWNER = "owner";
    public static final String PROP_COMMENT = "comment";
    public static final String PROP_SERVER = "server";
    public static final String PROP_MAPPINGS = "mappings";
    public static final String PROP_LOADING = "loading";

    private boolean loading;
    private String name;
    private String computer;
    private String owner;
    private String comment;
    private String server;
    private List<Workspace.Mapping> mappings;

    public WorkspaceModel() {
    }

    public boolean isLoading() {
        return loading;
    }

    private void setLoading(final boolean loading) {
        this.loading = loading;
        setChangedAndNotify(PROP_LOADING);
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
        super.setChangedAndNotify(PROP_NAME);
    }

    public String getComputer() {
        return computer;
    }

    public void setComputer(final String computer) {
        this.computer = computer;
        super.setChangedAndNotify(PROP_COMPUTER);
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(final String owner) {
        this.owner = owner;
        super.setChangedAndNotify(PROP_OWNER);
    }

    public String getComment() {
        return comment;
    }

    public void setComment(final String comment) {
        this.comment = comment;
        super.setChangedAndNotify(PROP_COMMENT);
    }

    public String getServer() {
        return server;
    }

    public void setServer(final String server) {
        this.server = server;
        super.setChangedAndNotify(PROP_SERVER);
    }

    public List<Workspace.Mapping> getMappings() {
        if (mappings == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(mappings);
    }

    public void setMappings(@NotNull final List<Workspace.Mapping> mappings) {
        this.mappings = mappings;
        super.setChangedAndNotify(PROP_MAPPINGS);
    }

    public ModelValidationInfo validate() {
        if (StringUtils.isEmpty(getName())) {
            return ModelValidationInfo.createWithResource(PROP_NAME,
                    TfPluginBundle.KEY_WORKSPACE_DIALOG_ERRORS_NAME_EMPTY);
        }
        if (getMappings().size() == 0) {
            return ModelValidationInfo.createWithResource(PROP_MAPPINGS,
                    TfPluginBundle.KEY_WORKSPACE_DIALOG_ERRORS_MAPPINGS_EMPTY);
        }

        return ModelValidationInfo.NO_ERRORS;
    }


    public void loadWorkspace(final Project project) {
        logger.info("loadWorkspace starting");
        setLoading(true);
        // Load
        OperationExecutor.getInstance().submitOperationTask(new Runnable() {
            @Override
            public void run() {
                try {
                    logger.info("loadWorkspace: getting repository context");
                    final RepositoryContext repositoryContext = VcsHelper.getRepositoryContext(project);
                    if (repositoryContext == null) {
                        logger.warn("loadWorkspace: Could not determine repositoryContext for project");
                        throw new RuntimeException(TfPluginBundle.message(TfPluginBundle.KEY_WORKSPACE_DIALOG_ERRORS_CONTEXT_FAILED));
                    }

                    logger.info("loadWorkspace: getting server context");
                    final ServerContext context = ServerContextManager.getInstance().createContextFromTfvcServerUrl(repositoryContext.getUrl(), repositoryContext.getTeamProjectName(), true);
                    if (context == null) {
                        logger.warn("loadWorkspace: Could not get the context for the repository. User may have canceled.");
                        throw new NotAuthorizedException(TfPluginBundle.message(TfPluginBundle.KEY_WORKSPACE_DIALOG_ERRORS_AUTH_FAILED, repositoryContext.getUrl()));
                    }

                    logger.info("loadWorkspace: getting workspace");
                    final Workspace workspace = CommandUtils.getWorkspace(context, repositoryContext.getName());
                    if (workspace != null) {
                        logger.info("loadWorkspace: got workspace, setting fields");
                        server = workspace.getServer();
                        owner = workspace.getOwner();
                        computer = workspace.getComputer();
                        name = workspace.getName();
                        comment = workspace.getComment();
                        mappings = new ArrayList<Workspace.Mapping>(workspace.getMappings());
                    } else {
                        // This shouldn't happen, so we will log this case, but not throw
                        logger.warn("loadWorkspace: workspace was returned as null");
                    }
                } finally {
                    // Make sure to fire events only on the UI thread
                    IdeaHelper.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            // Update all fields
                            setChangedAndNotify(null);
                            // Set loading to false
                            setLoading(false);
                            logger.info("loadWorkspace: done loading");
                        }
                    });
                }
            }
        });
    }

    public void saveWorkspace() {
    }
}
