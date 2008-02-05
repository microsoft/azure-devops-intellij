package org.jetbrains.tfsIntegration.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

/**
 * Created by IntelliJ IDEA.
 * User: senin
 * Date: 27.01.2008
 * Time: 22:38:51
 */
public class AddAction extends AnAction {
  public void actionPerformed(AnActionEvent e) {
    System.out.println("TFS.AddAction.actionPerformed()");
  }
}
