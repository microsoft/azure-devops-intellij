package org.jetbrains.tfsIntegration.ui;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Created by IntelliJ IDEA.
 * Date: 14.02.2008
 * Time: 21:16:32
 */
public class NoConnectionItemTreeNode extends DefaultMutableTreeNode {
  public String toString() {
    return "Cannot connect to server";
  }
}
