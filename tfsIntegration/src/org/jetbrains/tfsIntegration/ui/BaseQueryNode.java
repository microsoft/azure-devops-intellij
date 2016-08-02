package org.jetbrains.tfsIntegration.ui;

import com.intellij.ui.treeStructure.SimpleNode;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.checkin.CheckinParameters;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;

public abstract class BaseQueryNode extends SimpleNode {

  @NotNull protected final QueriesTreeContext myQueriesTreeContext;

  protected BaseQueryNode(@NotNull QueriesTreeContext context) {
    myQueriesTreeContext = context;
  }

  @NotNull
  protected WorkItemClient getWorkItemClient() {
    return myQueriesTreeContext.getProjectCollection().getWorkItemClient();
  }

  @NotNull
  protected ServerInfo getServer() {
    return myQueriesTreeContext.getServer();
  }

  @NotNull
  protected CheckinParameters getState() {
    return myQueriesTreeContext.getState();
  }
}