package org.jetbrains.tfsIntegration.ui;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItemsQuery;

import java.util.List;

public class PredefinedQueriesGroupNode extends BaseQueryNode {

  public PredefinedQueriesGroupNode(@NotNull QueriesTreeContext context) {
    super(context);
  }

  @Override
  protected void doUpdate() {
    PresentationData presentation = getPresentation();

    presentation.addText("Predefined Queries", SimpleTextAttributes.REGULAR_ATTRIBUTES);
  }

  @NotNull
  @Override
  public Object[] getEqualityObjects() {
    return new Object[]{getServer().getUri(), WorkItemsQuery.class};
  }

  @Override
  public SimpleNode[] getChildren() {
    List<SimpleNode> result = ContainerUtil.newArrayList();

    for (WorkItemsQuery query : WorkItemsQuery.values()) {
      result.add(new PredefinedQueryNode(myQueriesTreeContext, query));
    }

    return ArrayUtil.toObjectArray(result, SimpleNode.class);
  }
}
