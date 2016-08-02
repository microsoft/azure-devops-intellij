package org.jetbrains.tfsIntegration.ui.servertree;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.tfsIntegration.core.TFSBundle;

public class CreateVirtualFolderAction extends DumbAwareAction {

  @Override
  public void update(AnActionEvent e) {
    boolean isEnabled = isEnabled(e);
    if (ActionPlaces.isPopupPlace(e.getPlace())) {
      e.getPresentation().setVisible(isEnabled);
    }
    else {
      e.getPresentation().setEnabled(isEnabled);
    }
  }

  private static boolean isEnabled(AnActionEvent e) {
    TfsTreeForm form = TfsTreeForm.KEY.getData(e.getDataContext());
    return form != null && form.getSelectedItem() != null && form.canCreateVirtualFolders();
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    TfsTreeForm form = TfsTreeForm.KEY.getData(e.getDataContext());
    String folderName = Messages
      .showInputDialog(form.getContentPane(), TFSBundle.message("create.subfolder.prompt"), TFSBundle.message("create.subfolder.title"),
                       null);
    if (StringUtil.isEmpty(folderName)) {
      return;
    }
    form.createVirtualFolder(folderName);
  }
}
