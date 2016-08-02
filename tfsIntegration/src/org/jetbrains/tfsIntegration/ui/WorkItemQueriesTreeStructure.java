package org.jetbrains.tfsIntegration.ui;

import com.intellij.openapi.Disposable;
import com.intellij.ui.treeStructure.SimpleTreeStructure;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.checkin.CheckinParameters;
import org.jetbrains.tfsIntegration.core.TfsSdkManager;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;
import org.jetbrains.tfsIntegration.core.tfs.TfsExecutionUtil;

public class WorkItemQueriesTreeStructure extends SimpleTreeStructure implements QueriesTreeContext, Disposable {

  @NotNull private final WorkItemQueriesTreeRootNode myRootNode;
  @NotNull private final TFSTeamProjectCollection myProjectCollection;
  @NotNull private final CheckinParameters myState;
  @NotNull private final ServerInfo myServer;
  @NotNull private final WorkItemsPanel myPanel;

  public WorkItemQueriesTreeStructure(@NotNull WorkItemsPanel panel) {
    myPanel = panel;
    myState = panel.getState();
    myServer = panel.getServer();
    myRootNode = new WorkItemQueriesTreeRootNode(this);
    myProjectCollection = new TFSTeamProjectCollection(myServer.getUri(), TfsSdkManager.getCredentials(myServer));
  }

  @NotNull
  public PredefinedQueriesGroupNode getPredefinedQueriesGroupNode() {
    return myRootNode.getPredefinedQueriesGroupNode();
  }

  @Override
  public Object getRootElement() {
    return myRootNode;
  }

  @Override
  public boolean isToBuildChildrenInBackground(Object element) {
    return element instanceof SavedQueryFolderNode && ((SavedQueryFolderNode)element).isProject();
  }

  @NotNull
  @Override
  public CheckinParameters getState() {
    return myState;
  }

  @NotNull
  @Override
  public ServerInfo getServer() {
    return myServer;
  }

  @NotNull
  @Override
  public TFSTeamProjectCollection getProjectCollection() {
    return myProjectCollection;
  }

  @Override
  public void queryWorkItems(@NotNull TfsExecutionUtil.Process<WorkItemsQueryResult> query) {
    myPanel.queryWorkItems(query);
  }

  @Override
  public void dispose() {
    myProjectCollection.close();
  }
}
