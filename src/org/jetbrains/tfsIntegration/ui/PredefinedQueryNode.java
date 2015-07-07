package org.jetbrains.tfsIntegration.ui;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.tfs.TfsExecutionUtil;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItem;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItemsQuery;
import org.jetbrains.tfsIntegration.exceptions.TfsException;

import java.util.List;

public class PredefinedQueryNode extends BaseQueryNode {

  @NotNull private final WorkItemsQuery myQuery;

  public PredefinedQueryNode(@NotNull QueriesTreeContext context, @NotNull WorkItemsQuery query) {
    super(context);

    myQuery = query;
  }

  @Override
  protected void doUpdate() {
    PresentationData presentation = getPresentation();

    presentation.addText(myQuery.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
  }

  @NotNull
  @Override
  public Object[] getEqualityObjects() {
    return new Object[]{myQuery};
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
  public void handleSelection(@NotNull final SimpleTree tree) {
    myQueriesTreeContext.queryWorkItems(new TfsExecutionUtil.Process<List<WorkItem>>() {
      @NotNull
      @Override
      public List<WorkItem> run() throws TfsException, VcsException {
        return myQuery.queryWorkItems(getServer(), tree, null);
      }
    });
  }
}
