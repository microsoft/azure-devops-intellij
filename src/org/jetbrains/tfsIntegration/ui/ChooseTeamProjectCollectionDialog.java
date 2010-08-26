package org.jetbrains.tfsIntegration.ui;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.config.TfsServerConnectionHelper;
import org.jetbrains.tfsIntegration.core.TFSBundle;
import org.jetbrains.tfsIntegration.core.configuration.TFSConfigurationManager;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.util.Collection;

public class ChooseTeamProjectCollectionDialog extends DialogWrapper {

  private final ChooseTeamProjectCollectionForm myForm;

  public ChooseTeamProjectCollectionDialog(JComponent parent, String serverAddress,
                                           Collection<TfsServerConnectionHelper.TeamProjectCollectionDescriptor> items) {
    super(parent, false);

    setTitle(TFSBundle.message("choose.team.project.collection.dialog.title"));
    setSize(500, 400);

    myForm = new ChooseTeamProjectCollectionForm(serverAddress, items);
    myForm.addChangeListener(new ChooseTeamProjectCollectionForm.Listener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        revalidate();
      }

      @Override
      public void selected() {
        if (getErrorMessage() == null) {
          doOKAction();
        }
      }
    });

    init();

    revalidate();
  }

  private void revalidate() {
    String message = getErrorMessage();
    setOKActionEnabled(message == null);
    myForm.setErrorMessage(message);
  }

  @Nullable
  private String getErrorMessage() {
    TfsServerConnectionHelper.TeamProjectCollectionDescriptor selected = getSelectedItem();
    if (selected == null) {
      return TFSBundle.message("no.team.project.collection.selected");
    }
    if (TFSConfigurationManager.getInstance().serverKnown(selected.instanceId)) {
      return TFSBundle.message("duplicate.team.project.collection", selected.name);
    }
    return null;
  }

  @Nullable
  public TfsServerConnectionHelper.TeamProjectCollectionDescriptor getSelectedItem() {
    return myForm.getSelectedItem();
  }

  @Override
  protected JComponent createCenterPanel() {
    return myForm.getContentPane();
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myForm.getPreferredFocusedComponent();
  }

  @Override
  protected String getDimensionServiceKey() {
    return "TFS.ChooseTeamProjectCollection";
  }
}
