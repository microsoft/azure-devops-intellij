package org.jetbrains.tfsIntegration.ui;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.microsoft.tfs.core.clients.workitem.WorkItemQueryUtils;
import com.microsoft.tfs.core.clients.workitem.exceptions.WorkItemException;
import com.microsoft.tfs.core.clients.workitem.query.*;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryDefinition;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryType;
import com.microsoft.tfs.core.ws.runtime.exceptions.ProxyException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.tfs.TfsExecutionUtil;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItem;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItemField;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItemSerialize;
import org.jetbrains.tfsIntegration.exceptions.TfsException;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class SavedQueryDefinitionNode extends BaseQueryNode {

  private static String[] WORK_ITEM_FIELDS =
    ContainerUtil.map2Array(WorkItemSerialize.FIELDS, String.class, new Function<WorkItemField, String>() {
      @Override
      public String fun(@NotNull WorkItemField field) {
        return field.getSerialized();
      }
    });
  private static String WORK_ITEMS_QUERY = "SELECT " + WorkItemQueryUtils.formatFieldList(WORK_ITEM_FIELDS) + " FROM WorkItems";

  @NotNull private final QueryDefinition myQueryDefinition;

  public SavedQueryDefinitionNode(@NotNull QueriesTreeContext context, @NotNull QueryDefinition definition) {
    super(context);

    myQueryDefinition = definition;
  }

  @Override
  protected void doUpdate() {
    PresentationData presentation = getPresentation();

    presentation.addText(myQueryDefinition.getName(), getPlainAttributes());
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
    final boolean isList = isListQuery();

    myQueriesTreeContext.queryWorkItems(new TfsExecutionUtil.Process<WorkItemsQueryResult>() {
      @NotNull
      @Override
      public WorkItemsQueryResult run() throws TfsException, VcsException {
        try {
          return isList ? runListQuery() : runLinkQuery();
        }
        catch (WorkItemException e) {
          throw new VcsException(e);
        }
        catch (ProxyException e) {
          throw new VcsException(e);
        }
      }
    });
  }

  private boolean isListQuery() {
    return QueryType.LIST.equals(myQueryDefinition.getQueryType());
  }

  @NotNull
  private WorkItemsQueryResult runListQuery() throws WorkItemException, ProxyException {
    WorkItemCollection workItems = getWorkItemClient().query(myQueryDefinition.getQueryText(), buildQueryContext());

    return new WorkItemsQueryResult(toList(workItems));
  }

  @NotNull
  private static List<WorkItem> toList(@NotNull WorkItemCollection workItems) throws WorkItemException, ProxyException {
    List<WorkItem> result = ContainerUtil.newArrayList();

    for (int i = 0; i < workItems.size(); i++) {
      result.add(WorkItem.create(workItems.getWorkItem(i)));
    }

    return result;
  }

  @NotNull
  private WorkItemsQueryResult runLinkQuery() throws WorkItemException, ProxyException {
    Query linksQuery = getWorkItemClient().createQuery(myQueryDefinition.getQueryText(), buildQueryContext());
    List<WorkItemLinkInfo> links = ContainerUtil.newArrayList(linksQuery.runLinkQuery());
    Query workItemsQuery = getWorkItemClient().createQuery(WORK_ITEMS_QUERY, toBatchReadCollection(getWorkItemIds(links)));

    return new WorkItemsQueryResult(toList(workItemsQuery.runQuery()), links);
  }

  @NotNull
  private static BatchReadParameterCollection toBatchReadCollection(@NotNull Iterable<Integer> ids) {
    BatchReadParameterCollection result = new BatchReadParameterCollection();

    for (Integer id : ids) {
      result.add(new BatchReadParameter(id));
    }

    return result;
  }

  @NotNull
  private static Set<Integer> getWorkItemIds(@NotNull Iterable<WorkItemLinkInfo> links) {
    Set<Integer> result = ContainerUtil.newHashSet();

    for (WorkItemLinkInfo link : links) {
      addId(result, link.getSourceID());
      addId(result, link.getTargetID());
    }

    return result;
  }

  private static void addId(@NotNull Set<Integer> ids, int id) {
    if (id > 0) {
      ids.add(id);
    }
  }

  @NotNull
  private Map<String, Object> buildQueryContext() {
    return WorkItemQueryUtils.makeContext(myQueryDefinition.getProject(), null);
  }
}
