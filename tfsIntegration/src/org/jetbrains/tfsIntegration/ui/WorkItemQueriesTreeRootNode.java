package org.jetbrains.tfsIntegration.ui;

import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class WorkItemQueriesTreeRootNode extends BaseQueryNode {

  @NotNull private final PredefinedQueriesGroupNode myPredefinedQueriesGroupNode;

  public WorkItemQueriesTreeRootNode(@NotNull QueriesTreeContext context) {
    super(context);

    myPredefinedQueriesGroupNode = new PredefinedQueriesGroupNode(myQueriesTreeContext);
  }

  @NotNull
  public PredefinedQueriesGroupNode getPredefinedQueriesGroupNode() {
    return myPredefinedQueriesGroupNode;
  }

  @NotNull
  @Override
  public Object[] getEqualityObjects() {
    return new Object[]{getServer().getUri()};
  }

  @Override
  public SimpleNode[] getChildren() {
    List<SimpleNode> result = ContainerUtil.newArrayList();

    result.add(myPredefinedQueriesGroupNode);
    for (String projectPath : getState().getProjectPaths(getServer())) {
      result.add(new SavedQueryFolderNode(myQueriesTreeContext, projectPath));
    }

    return ArrayUtil.toObjectArray(result, SimpleNode.class);
  }
}
