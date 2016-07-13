// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.workitem;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import com.microsoft.alm.common.artifact.GitRefArtifactID;
import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.branch.CreateBranchController;
import com.microsoft.alm.plugin.idea.ui.common.tabs.TabModelImpl;
import com.microsoft.alm.plugin.idea.utils.EventContextHelper;
import com.microsoft.alm.plugin.idea.utils.TfGitHelper;
import com.microsoft.alm.plugin.operations.Operation;
import com.microsoft.alm.plugin.operations.OperationExecutor;
import com.microsoft.alm.plugin.operations.WorkItemLookupOperation;
import com.microsoft.alm.plugin.telemetry.TfsTelemetryHelper;
import com.microsoft.alm.workitemtracking.webapi.models.Link;
import com.microsoft.alm.workitemtracking.webapi.models.WorkItem;
import com.microsoft.visualstudio.services.webapi.patch.json.JsonPatchDocument;
import com.microsoft.visualstudio.services.webapi.patch.json.JsonPatchOperation;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.event.HyperlinkEvent;
import java.net.URI;
import java.util.HashMap;
import java.util.List;

public class VcsWorkItemsModel extends TabModelImpl<WorkItemsTableModel> {
    private static final Logger logger = LoggerFactory.getLogger(VcsWorkItemsModel.class);

    private static final String DEFAULT_BRANCH_NAME_PATTERN = "workitem%s";
    private static final String LINK_NAME_KEY = "name";
    private static final String LINK_NAME_VALUE = "Branch";
    private static final String ARTIFACT_LINK_RELATION = "ArtifactLink";
    private static final String RELATIONS_PATH = "/relations/-";
    public static final String ASSOCIATE_WORK_ITEM_ACTION = "associate-work-item";

    public VcsWorkItemsModel(final @NotNull Project project) {
        super(project, new WorkItemsTableModel(WorkItemsTableModel.COLUMNS_PLUS_BRANCH), "WorkItemsTab.");
        operationInputs = new WorkItemLookupOperation.WitInputs(WorkItemHelper.getAssignedToMeQuery());
    }

    @VisibleForTesting
    protected VcsWorkItemsModel(final @NotNull Project project, final @NotNull WorkItemsTableModel tableModel) {
        super(project, tableModel, "WorkItemsTab.");
    }

    protected void createDataProvider() {
        dataProvider = new WorkItemsTabLookupListener(this);
    }

    public void openGitRepoLink() {
        // create a new work item and open WIT takes you to the same place at the moment
        createNewItem();
    }

    public void openSelectedItemsLink() {
        if (isTfGitRepository()) {
            final ServerContext context = TfGitHelper.getSavedServerContext(gitRepository);

            if (context != null && context.getTeamProjectURI() != null) {
                final List<WorkItem> workItems = viewForModel.getSelectedWorkItems();
                final URI teamProjectURI = context.getTeamProjectURI();
                if (teamProjectURI != null) {
                    for (WorkItem item : workItems) {
                        super.gotoLink(UrlHelper.getSpecificWorkItemURI(teamProjectURI, item.getId()).toString());
                    }
                } else {
                    logger.warn("Can't goto 'create work item' link: Unable to get team project URI from server context.");
                }
            }
        }
    }

    public void createBranch() {
        if (!isTfGitRepository()) {
            logger.debug("createBranch: cannot associate a work item with a branch in a non-TF repo");
            return;
        }

        final ServerContext context = TfGitHelper.getSavedServerContext(gitRepository);
        final WorkItem workItem = viewForModel.getSelectedWorkItems().get(0); // TODO: associate multiple work items with a branch

        // call the Create Branch dialog and get the branch name from the user
        final CreateBranchController controller = new CreateBranchController(project,
                String.format(DEFAULT_BRANCH_NAME_PATTERN, workItem.getId()), gitRepository);

        if (controller.showModalDialog()) {
            final String branchName = controller.getBranchName();
            try {
                //TODO should this be an IntelliJ background task so we can provide progress information? (if so we should pass the progress indicator to createBranch and create association)
                OperationExecutor.getInstance().submitOperationTask(new Runnable() {
                    @Override
                    public void run() {
                        // do branch creation
                        final boolean wasBranchCreated = controller.createBranch(context);

                        // check if branch creation succeeded before associating the work item to it
                        boolean wasWorkItemAssociated = false;
                        if (wasBranchCreated) {
                            wasWorkItemAssociated = createWorkItemBranchAssociation(context, branchName, workItem.getId());

                            logger.info("Work item association " + (wasWorkItemAssociated ? "succeeded" : "failed"));
                            final String notificationMsg = TfPluginBundle.message(wasWorkItemAssociated ? TfPluginBundle.KEY_WIT_ASSOCIATION_SUCCESSFUL_DESCRIPTION : TfPluginBundle.KEY_WIT_ASSOCIATION_FAILED_DESCRIPTION,
                                    UrlHelper.getSpecificWorkItemURI(context.getTeamProjectURI(), workItem.getId()), workItem.getId(), UrlHelper.getBranchURI(context.getUri(), branchName), branchName);

                            VcsNotifier.getInstance(project).notifyImportantInfo(TfPluginBundle.message(wasWorkItemAssociated ? TfPluginBundle.KEY_WIT_ASSOCIATION_SUCCESSFUL_TITLE : TfPluginBundle.KEY_WIT_ASSOCIATION_FAILED_TITLE),
                                    notificationMsg, new NotificationListener() {
                                        @Override
                                        public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent hyperlinkEvent) {
                                            BrowserUtil.browse(hyperlinkEvent.getURL());
                                        }
                                    });
                        }

                        TfsTelemetryHelper.getInstance().sendEvent(ASSOCIATE_WORK_ITEM_ACTION, new TfsTelemetryHelper.PropertyMapBuilder()
                                .currentOrActiveContext(context)
                                .actionName(ASSOCIATE_WORK_ITEM_ACTION)
                                .success(wasWorkItemAssociated).build());

                        // Update the work items tab and any other listener to WorkItemChanged events
                        EventContextHelper.triggerWorkItemChanged(EventContextHelper.SENDER_ASSOCIATE_BRANCH, project);
                    }
                });
            } catch (Exception e) {
                logger.error("Failed to create a branch and associate it with a work item", e);
            }
        }
    }

    protected boolean createWorkItemBranchAssociation(final ServerContext context, final String branchName, final int workItemId) {
        final GitRefArtifactID gitRefArtifactID = new GitRefArtifactID(context.getTeamProjectReference().getId().toString(),
                context.getGitRepository().getId().toString(), branchName);

        // attributes specify the link type
        final HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(LINK_NAME_KEY, LINK_NAME_VALUE);

        // create link object to add to the work item
        final Link link = new Link();
        link.setUrl(gitRefArtifactID.encodeURI());
        link.setTitle(StringUtils.EMPTY);
        link.setRel(ARTIFACT_LINK_RELATION);
        link.setAttributes(attributes);

        // create the operation that will add the link to the work item
        final JsonPatchOperation operation = new JsonPatchOperation();
        operation.setOp(com.microsoft.visualstudio.services.webapi.patch.Operation.ADD);
        operation.setPath(RELATIONS_PATH);
        operation.setValue(link);

        final JsonPatchDocument doc = new JsonPatchDocument();
        doc.add(operation);

        try {
            context.getWitHttpClient().updateWorkItem(doc, workItemId, false, false);
            return true;
        } catch (Throwable t) {
            if (AuthHelper.isNotAuthorizedError(t)) {
                final ServerContext newContext = ServerContextManager.getInstance().updateAuthenticationInfo(context.getGitRepository().getRemoteUrl());
                if (newContext != null) {
                    //retry creating the branch with new context and authentication info
                    return createWorkItemBranchAssociation(newContext, branchName, workItemId);
                } else {
                    logger.error("createWorkItemBranchAssociation isNotAuthorizedError and failed to create a new context to use");
                    return false;
                }
            } else {
                logger.error("createWorkItemBranchAssociation experienced an exception while associating a work item and branch", t);
                return false;
            }
        }
    }

    public void appendData(final Operation.Results results) {
        final WorkItemLookupOperation.WitResults witResults = (WorkItemLookupOperation.WitResults) results;
        viewForModel.addWorkItems(witResults.getWorkItems());
    }

    public void clearData() {
        viewForModel.clearRows();
    }

    public void createNewItem() {
        if (isTfGitRepository()) {
            final ServerContext context = TfGitHelper.getSavedServerContext(gitRepository);

            if (context != null && context.getTeamProjectURI() != null) {
                final URI teamProjectURI = context.getTeamProjectURI();
                if (teamProjectURI != null) {
                    super.gotoLink(UrlHelper.getCreateWorkItemURI(teamProjectURI).toString());
                } else {
                    logger.warn("Can't goto 'create work item' link: Unable to get team project URI from server context.");
                }
            }
        }
    }
}