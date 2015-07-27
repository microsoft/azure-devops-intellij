package org.jetbrains.tfsIntegration.ui;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.containers.ContainerUtil;
import com.microsoft.tfs.core.clients.workitem.exceptions.WorkItemException;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemCollection;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryDefinition;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryType;
import com.microsoft.tfs.core.ws.runtime.exceptions.ProxyException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.tfs.TfsExecutionUtil;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItem;
import org.jetbrains.tfsIntegration.exceptions.TfsException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SavedQueryDefinitionNode extends BaseQueryNode {

  @NotNull private final QueryDefinition myQueryDefinition;

  public SavedQueryDefinitionNode(@NotNull QueriesTreeContext context, @NotNull QueryDefinition definition) {
    super(context);

    myQueryDefinition = definition;
  }

  @Override
  protected void doUpdate() {
    PresentationData presentation = getPresentation();
    SimpleTextAttributes attributes = isListQuery() ? SimpleTextAttributes.REGULAR_ATTRIBUTES : SimpleTextAttributes.GRAYED_ATTRIBUTES;

    presentation.addText(myQueryDefinition.getName(), attributes);
  }

  @NotNull
  @Override
  public Object[] getEqualityObjects() {
    return new Object[]{myQueryDefinition.getID()};
  }

  @Override
  public SimpleNode[] getChildren() {
    return NO_CHILDREN;
  }

  @Override
  public boolean isAlwaysLeaf() {
    return true;
  }

  @Override
  public void handleSelection(@NotNull SimpleTree tree) {
    if (isListQuery()) {
      myQueriesTreeContext.queryWorkItems(new TfsExecutionUtil.Process<WorkItemsQueryResult>() {
        @NotNull
        @Override
        public WorkItemsQueryResult run() throws TfsException, VcsException {
          return runListQuery();
        }
      });
    }
  }

  private boolean isListQuery() {
    return QueryType.LIST.equals(myQueryDefinition.getQueryType());
  }

  @NotNull
  private WorkItemsQueryResult runListQuery() throws VcsException {
    List<WorkItem> result = ContainerUtil.newArrayList();

    try {
      WorkItemCollection workItems = getWorkItemClient().query(myQueryDefinition.getQueryText(), buildQueryContext());

      for (int i = 0; i < workItems.size(); i++) {
        result.add(WorkItem.create(workItems.getWorkItem(i)));
      }
    }
    catch (WorkItemException e) {
      throw new VcsException(e);
    }
    catch (ProxyException e) {
      throw new VcsException(e);
    }

    return new WorkItemsQueryResult(result);
  }

  @NotNull
  private Map<String, Object> buildQueryContext() {
    HashMap<String, Object> parameters = ContainerUtil.newHashMap();

    parameters.put("project", myQueryDefinition.getProject().getName());

    return parameters;
  }
}
