package org.jetbrains.tfsIntegration.ui;

import com.intellij.ui.TableSpeedSearch;
import com.intellij.ui.table.TableView;
import com.intellij.util.EventDispatcher;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.config.TfsServerConnectionHelper;
import org.jetbrains.tfsIntegration.core.TFSBundle;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.*;

public class ChooseTeamProjectCollectionForm {
  private JPanel myContentPane;
  private JTextField myAddressField;
  private TableView myTable;
  private JLabel myMessageLabel;

  private final EventDispatcher<ChangeListener> myEventDispatcher = EventDispatcher.create(ChangeListener.class);

  public ChooseTeamProjectCollectionForm(String serverAddress,
                                         Collection<TfsServerConnectionHelper.TeamProjectCollectionDescriptor> items) {
    myAddressField.setText(serverAddress);

    ColumnInfo<TfsServerConnectionHelper.TeamProjectCollectionDescriptor, String> displayNameColumn =
      new ColumnInfo<TfsServerConnectionHelper.TeamProjectCollectionDescriptor, String>(
        TFSBundle.message("team.project.collection.table.column.display.name")) {
        @Override
        public String valueOf(TfsServerConnectionHelper.TeamProjectCollectionDescriptor teamProjectCollectionDescriptor) {
          return teamProjectCollectionDescriptor.name;
        }
      };

    List<TfsServerConnectionHelper.TeamProjectCollectionDescriptor> sorted =
      new ArrayList<TfsServerConnectionHelper.TeamProjectCollectionDescriptor>(items);
    Collections.sort(sorted, new Comparator<TfsServerConnectionHelper.TeamProjectCollectionDescriptor>() {
      @Override
      public int compare(TfsServerConnectionHelper.TeamProjectCollectionDescriptor o1,
                         TfsServerConnectionHelper.TeamProjectCollectionDescriptor o2) {
        return o1.name.compareToIgnoreCase(o2.name);
      }
    });

    myTable.setTableHeader(null);
    myTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    myTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        myEventDispatcher.getMulticaster().stateChanged(new ChangeEvent(this));
      }
    });


    myMessageLabel.setIcon(UIUtil.getBalloonWarningIcon());

    new TableSpeedSearch(myTable);

    myTable.setModel(
      new ListTableModel<TfsServerConnectionHelper.TeamProjectCollectionDescriptor>(new ColumnInfo[]{displayNameColumn}, sorted, 0));
  }

  public JPanel getContentPane() {
    return myContentPane;
  }

  @Nullable
  public TfsServerConnectionHelper.TeamProjectCollectionDescriptor getSelectedItem() {
    return (TfsServerConnectionHelper.TeamProjectCollectionDescriptor)myTable.getSelectedObject();
  }

  public void addChangeListener(ChangeListener listener) {
    myEventDispatcher.addListener(listener);
  }

  public JComponent getPreferredFocusedComponent() {
    return myTable;
  }

  public void setErrorMessage(@Nullable String errorMessage) {
    if (errorMessage != null && !errorMessage.endsWith(".")) {
      errorMessage += ".";
    }
    myMessageLabel.setText(errorMessage);
    myMessageLabel.setVisible(errorMessage != null);
  }

}
