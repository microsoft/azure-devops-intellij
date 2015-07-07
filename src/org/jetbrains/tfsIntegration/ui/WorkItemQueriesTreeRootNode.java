package org.jetbrains.tfsIntegration.ui;

import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class WorkItemQueriesTreeRootNode extends BaseQueryNode {

  public WorkItemQueriesTreeRootNode(@NotNull QueriesTreeContext context) {
    super(context);
  }

  @NotNull
  @Override
  public Object[] getEqualityObjects() {
    return new Object[]{getServer().getUri()};
  }

  @Override
  public SimpleNode[] getChildren() {
    List<SimpleNode> result = ContainerUtil.newArrayList();

    for (String projectPath : getState().getProjectPaths(getServer())) {
      result.add(new SavedQueryFolderNode(myQueriesTreeContext, projectPath));
    }

    return ArrayUtil.toObjectArray(result, SimpleNode.class);
  }
}
