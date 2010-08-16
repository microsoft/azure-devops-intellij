package org.jetbrains.tfsIntegration.ui.servertree;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.util.Icons;

public class TfsErrorTreeNode extends SimpleNode {
  private final String myMessage;

  protected TfsErrorTreeNode(SimpleNode parent, String message) {
    super(parent);
    myMessage = message;
  }

  @Override
  protected void update(PresentationData presentation) {
    super.update(presentation);
    presentation.addText(myMessage, getErrorAttributes());
    presentation.setIcons(Icons.ERROR_INTRODUCTION_ICON);
  }

  @Override
  public SimpleNode[] getChildren() {
    return NO_CHILDREN;
  }

  public String getMessage() {
    return myMessage;
  }
}
