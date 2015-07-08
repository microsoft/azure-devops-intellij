package org.jetbrains.tfsIntegration.ui;

import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.TableSpeedSearch;
import com.intellij.ui.table.TableView;
import com.intellij.ui.treeStructure.NullNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.ui.treeStructure.SimpleTreeStructure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.checkin.CheckinParameters;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;
import org.jetbrains.tfsIntegration.core.tfs.TfsExecutionUtil;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItem;

import javax.swing.*;
import java.util.List;

/**
 * @author Konstantin Kolosovsky.
 */
public class WorkItemsPanel implements Disposable {

  @SuppressWarnings("unused") private JPanel myMainPanel;
  private TableView<WorkItem> myWorkItemsTable;
  private SimpleTree myWorkItemQueriesTree;
  private WorkItemQueriesTreeBuilder myTreeBuilder;
  private final WorkItemsTableModel myWorkItemsTableModel;

  private final CheckinParametersForm myForm;

  public WorkItemsPanel(CheckinParametersForm form) {
    myForm = form;

    myWorkItemsTableModel = new WorkItemsTableModel();
    myWorkItemsTable.setModelAndUpdateColumns(myWorkItemsTableModel);
    myWorkItemsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

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

    getState().getWorkItems(myForm.getSelectedServer()).update(result.result);
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
