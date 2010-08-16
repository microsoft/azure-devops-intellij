package org.jetbrains.tfsIntegration.ui.servertree;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.util.Icons;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.Item;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.ItemType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;
import org.jetbrains.tfsIntegration.core.tfs.VersionControlPath;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.ui.UiConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TfsTreeNode extends SimpleNode {

  private static final SimpleTextAttributes VIRTUAL_ATTRS = SimpleTextAttributes.SYNTHETIC_ATTRIBUTES;

  private final TfsTreeContext myTreeContext;
  private final String myPath;
  private final boolean myIsDirectory;
  private final boolean myVirtual;
  private final Collection<TfsTreeNode> myVirtualChildren = new ArrayList<TfsTreeNode>();

  // root node
  public TfsTreeNode(@NotNull Object projectOrComponent,
                     ServerInfo server,
                     boolean foldersOnly,
                     @Nullable Condition<String> pathFilter) {
    super(projectOrComponent instanceof Project ? (Project)projectOrComponent : null);
    myTreeContext = new TfsTreeContext(server, foldersOnly, projectOrComponent, pathFilter);
    myPath = VersionControlPath.ROOT_FOLDER;
    myIsDirectory = true;
    myVirtual = false;
  }

  // child node
  private TfsTreeNode(TfsTreeNode parent, String path, boolean isDirectory, boolean virtual) {
    super(parent);
    myPath = path;
    myIsDirectory = isDirectory;
    myVirtual = virtual;
    myTreeContext = parent.myTreeContext;
  }

  @Override
  public SimpleNode[] getChildren() {
    if (!myIsDirectory) {
      return NO_CHILDREN;
    }

    List<Item> children;
    try {
      children = myTreeContext.getChildItems(myPath);
    }
    catch (TfsException e) {
      return new SimpleNode[]{new TfsErrorTreeNode(this, e.getMessage())};
    }

    final List<TfsTreeNode> result = new ArrayList<TfsTreeNode>(myVirtualChildren);
    for (final Item childItem : children) {
      result.add(new TfsTreeNode(this, childItem.getItem(), childItem.getType() == ItemType.Folder, false));
    }
    return result.toArray(new SimpleNode[result.size()]);
  }

  @Override
  protected void update(PresentationData presentation) {
    if (isRoot()) {
      //noinspection ConstantConditions
      presentation.addText(myTreeContext.myServer.getPresentableUri(), getPlainAttributes());
      presentation.setIcons(UiConstants.ICON_TEAM_SERVER);
    }
    else {
      if (isDirectory()) {
        presentation.setOpenIcon(Icons.DIRECTORY_OPEN_ICON);
        presentation.setClosedIcon(Icons.DIRECTORY_CLOSED_ICON);
      }
      else {
        presentation.setIcons(FileTypeManager.getInstance().getFileTypeByFileName(getFileName()).getIcon());
      }
      SimpleTextAttributes attrs;
      if (myVirtual) {
        attrs = VIRTUAL_ATTRS;
      }
      else if (myTreeContext.isAccepted(myPath)) {
        attrs = getPlainAttributes();
      }
      else {
        attrs = SimpleTextAttributes.GRAYED_ATTRIBUTES;
      }
      presentation.addText(getFileName(), attrs);
    }
  }

  public String getFileName() {
    return VersionControlPath.getLastComponent(myPath);
  }

  public boolean isDirectory() {
    return myIsDirectory;
  }

  public boolean isRoot() {
    return VersionControlPath.ROOT_FOLDER.equals(myPath);
  }

  @NotNull
  public String getPath() {
    return myPath;
  }

  private TfsTreeNode createFakeChild(String name) {
    String childPath = VersionControlPath.getCombinedServerPath(myPath, name);
    return new TfsTreeNode(this, childPath, false, false);
  }

  @Override
  public Object[] getEqualityObjects() {
    return new Object[]{myPath};
  }

  @Nullable
  public TfsTreeNode createForSelection(String serverPath) {
    if (StringUtil.isEmpty(serverPath) || VersionControlPath.ROOT_FOLDER.equals(serverPath)) {
      return this;
    }

    TfsTreeNode result = this;
    String[] components = VersionControlPath.getPathComponents(serverPath);
    for (int i = 1; i < components.length; i++) {
      result = result.createFakeChild(components[i]);
    }
    return result;
  }

  public TfsTreeNode createVirtualSubfolder(String folderName) {
    String childPath = VersionControlPath.getCombinedServerPath(myPath, folderName);
    TfsTreeNode child = new TfsTreeNode(this, childPath, true, true);
    myVirtualChildren.add(child);
    return child;
  }
}
