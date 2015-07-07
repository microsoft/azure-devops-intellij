package org.jetbrains.tfsIntegration.ui;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryDefinition;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryFolder;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItem;
import com.microsoft.tfs.core.ws.runtime.exceptions.ProxyException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.VersionControlPath;
import org.jetbrains.tfsIntegration.ui.servertree.TfsErrorTreeNode;

import java.util.List;

public class SavedQueryFolderNode extends BaseQueryNode {

  @Nullable private final QueryFolder myQueryFolder;
  @Nullable private final String myProjectName;

  public SavedQueryFolderNode(@NotNull QueriesTreeContext context, @NotNull QueryFolder folder) {
    super(context);

    myQueryFolder = folder;
    myProjectName = null;
  }

  public SavedQueryFolderNode(@NotNull QueriesTreeContext context, @NotNull String projectPath) {
    super(context);

    myQueryFolder = null;
    myProjectName = VersionControlPath.getTeamProject(projectPath);
  }

  @Override
  protected void doUpdate() {
    PresentationData presentation = getPresentation();

    presentation.addText(getQueryFolderName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
  }

  @Override
  public boolean isAlwaysShowPlus() {
    return true;
  }

  @NotNull
  @Override
  public Object[] getEqualityObjects() {
    return new Object[]{getQueryFolderName()};
  }

  public boolean isProject() {
    return myQueryFolder == null;
  }

  @Override
  public SimpleNode[] getChildren() {
    List<SimpleNode> result = ContainerUtil.newArrayList();

    try {
      result.addAll(getChildren(getQueryFolder()));
    }
    catch (VcsException e) {
      result.add(buildErrorNode(e));
    }
    catch (ProxyException e) {
      result.add(buildErrorNode(e));
    }

    return ArrayUtil.toObjectArray(result, SimpleNode.class);
  }

  @NotNull
  private List<SimpleNode> getChildren(@NotNull QueryFolder folder) {
    List<SimpleNode> result = ContainerUtil.newArrayList();

    for (QueryItem item : folder.getItems()) {
      SimpleNode child;

      if (item instanceof QueryDefinition) {
        child = new SavedQueryDefinitionNode(myQueriesTreeContext, (QueryDefinition)item);
      }
      else if (item instanceof QueryFolder) {
        child = new SavedQueryFolderNode(myQueriesTreeContext, (QueryFolder)item);
      }
      else {
        throw new IllegalArgumentException("Unknown query item " + item);
      }

      result.add(child);
    }

    return result;
  }

  @NotNull
  private SimpleNode buildErrorNode(@NotNull Exception e) {
    return new TfsErrorTreeNode(this, e.getMessage());
  }

  @NotNull
  private String getQueryFolderName() {
    return ObjectUtils.assertNotNull(myQueryFolder != null ? myQueryFolder.getName() : myProjectName);
  }

  @NotNull
  private QueryFolder getQueryFolder() throws VcsException, ProxyException {
    QueryFolder result;

    if (myQueryFolder != null) {
      result = myQueryFolder;
    }
    else {
      Project project = getWorkItemClient().getProjects().get(myProjectName);

      if (project == null) {
        throw new VcsException("Could not find project " + myProjectName + " in " + getServer().getPresentableUri());
      }

      result = project.getQueryHierarchy();
    }

    return result;
  }
}
