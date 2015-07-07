package org.jetbrains.tfsIntegration.ui;

import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.TableSpeedSearch;
import com.intellij.ui.treeStructure.NullNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.ui.treeStructure.SimpleTreeStructure;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.CheckinWorkItemAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.checkin.CheckinParameters;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;
import org.jetbrains.tfsIntegration.core.tfs.TfsExecutionUtil;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItem;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author Konstantin Kolosovsky.
 */
public class WorkItemsPanel implements Disposable {

  @SuppressWarnings("unused") private JPanel myMainPanel;
  private JTable myWorkItemsTable;
  private SimpleTree myWorkItemQueriesTree;
  private WorkItemQueriesTreeBuilder myTreeBuilder;
  private final WorkItemsTableModel myWorkItemsTableModel;

  private final CheckinParametersForm myForm;

  public WorkItemsPanel(CheckinParametersForm form) {
    myForm = form;

    myWorkItemsTableModel = new WorkItemsTableModel();
    myWorkItemsTable.setModel(myWorkItemsTableModel);
    myWorkItemsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    for (int i = 0; i < WorkItemsTableModel.Column.values().length; i++) {
      myWorkItemsTable.getColumnModel().getColumn(i).setPreferredWidth(WorkItemsTableModel.Column.values()[i].getWidth());
    }

    final JComboBox actionCombo =
      new JComboBox(new CheckinWorkItemAction[]{CheckinWorkItemAction.Resolve, CheckinWorkItemAction.Associate});

    myWorkItemsTable.getColumnModel().getColumn(WorkItemsTableModel.Column.Checkbox.ordinal())
      .setCellRenderer(new NoBackgroundBooleanTableCellRenderer());
    myWorkItemsTable.getColumnModel().getColumn(WorkItemsTableModel.Column.CheckinAction.ordinal())
      .setCellEditor(new DefaultCellEditor(actionCombo) {
        @Nullable
        public Component getTableCellEditorComponent(final JTable table,
                                                     final Object value,
                                                     final boolean isSelected,
                                                     final int row,
                                                     final int column) {
          WorkItem workItem = ((WorkItemsTableModel)table.getModel()).getWorkItem(row);
          CheckinWorkItemAction action = ((WorkItemsTableModel)table.getModel()).getAction(workItem);
          if (action != null && workItem.isActionPossible(CheckinWorkItemAction.Resolve)) {
            actionCombo.setSelectedItem(action);
            return super.getTableCellEditorComponent(table, value, isSelected, row, column);
          }
          else {
            return null;
          }
        }
      });

    new TableSpeedSearch(myWorkItemsTable);

    setupWorkItemQueries();
  }

  private void setupWorkItemQueries() {
    myTreeBuilder = new WorkItemQueriesTreeBuilder(myWorkItemQueriesTree, new SimpleTreeStructure.Impl(new NullNode()));
    Disposer.register(this, myTreeBuilder);
  }

  public void queryWorkItems(@NotNull TfsExecutionUtil.Process<List<WorkItem>> query) {
    final TfsExecutionUtil.ResultWithError<List<WorkItem>> result =
      TfsExecutionUtil.executeInBackground("Performing Query", getProject(), query);

    final String title = "Query Work Items";
    if (result.cancelled || result.showDialogIfError(title)) {
      return;
    }

    if (result.result.isEmpty()) {
      final String message = "No work items found for the selected query";
      Messages.showInfoMessage(getProject(), message, title);
    }
    getState().getWorkItems(myForm.getSelectedServer()).update(null, result.result);
    updateWorkItemsTable();
  }

  private void updateWorkItemsTable() {
    myWorkItemsTableModel.setContent(getState().getWorkItems(myForm.getSelectedServer()));
  }

  public void update() {
    updateWorkItemsTable();
    updateWorkItemQueries();
  }

  private void updateWorkItemQueries() {
    clearOldTreeStructure();
    setNewTreeStructure();
  }

  private void clearOldTreeStructure() {
    AbstractTreeStructure oldTreeStructure = myTreeBuilder.getTreeStructure();

    if (oldTreeStructure instanceof Disposable) {
      Disposer.dispose((Disposable)oldTreeStructure);
    }

    myTreeBuilder.cleanUp();
  }

  private void setNewTreeStructure() {
    final WorkItemQueriesTreeStructure newTreeStructure = new WorkItemQueriesTreeStructure(this);

    myTreeBuilder.setTreeStructure(newTreeStructure);
    Disposer.register(myTreeBuilder, newTreeStructure);

    myTreeBuilder.queueUpdate().doWhenDone(new Runnable() {
      @Override
      public void run() {
        myTreeBuilder.expand(newTreeStructure.getPredefinedQueriesGroupNode(), null);
      }
    });
  }

  @NotNull
  public CheckinParameters getState() {
    return myForm.getState();
  }

  @NotNull
  public ServerInfo getServer() {
    return myForm.getSelectedServer();
  }

  @NotNull
  public Project getProject() {
    return myForm.getProject();
  }

  @Override
  public void dispose() {
  }
}
