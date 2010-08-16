package org.jetbrains.tfsIntegration.ui.servertree;

import com.intellij.ide.DataManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.EventDispatcher;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeSelectionModel;
import java.util.EventListener;
import java.util.Set;

public class TfsTreeForm implements Disposable, DataProvider {
  private boolean myCanCreateVirtualFolders;

  public interface SelectionListener extends EventListener {
    void selectionChanged();
  }

  public static class SelectedItem {
    public final String path;
    public final boolean isDirectory;

    public SelectedItem(final String path, final boolean idDirectory) {
      this.path = path;
      isDirectory = idDirectory;
    }

    public SelectedItem(TfsTreeNode treeNode) {
      this(treeNode.getPath(), treeNode.isDirectory());
    }
  }

  public static final DataKey<TfsTreeForm> KEY = DataKey.create("TfsTreeForm");
  public static final String POPUP_ACTION_GROUP = "TfsTreePopupMenu";
  public static final Icon EMPTY_ICON = new EmptyIcon(0, UIUtil.getBalloonWarningIcon().getIconHeight());

  private JComponent myContentPane;
  private Tree myTree;
  private JTextField myPathField;
  private JLabel myMessageLabel;
  private JPanel myMessagePanel;
  private TfsTreeBuilder myTreeBuider;
  private EventDispatcher<SelectionListener> myEventDispatcher = EventDispatcher.create(SelectionListener.class);
  private SelectedItem mySelectedItem; // have to cache selected item to be available after form is disposed

  public TfsTreeForm() {
    myTree.putClientProperty(DataManager.CLIENT_PROPERTY_DATA_PROVIDER, this);
    new TreeSpeedSearch(myTree);
    myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    myTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent e) {
        mySelectedItem = doGetSelectedItem();
        myPathField.setText(mySelectedItem != null ? mySelectedItem.path : null);
        myEventDispatcher.getMulticaster().selectionChanged();
      }
    });
    PopupHandler.installPopupHandler(myTree, POPUP_ACTION_GROUP, ActionPlaces.REMOTE_HOST_DIALOG_POPUP);
    setMessage(null, false);
  }

  @Nullable
  public SelectedItem getSelectedItem() {
    return mySelectedItem;
  }

  @Nullable
  public String getSelectedPath() {
    return mySelectedItem != null ? mySelectedItem.path : null;
  }

  @Nullable
  private SelectedItem doGetSelectedItem() {
    final Set<Object> selection = myTreeBuider.getSelectedElements();
    if (selection.isEmpty()) {
      return null;
    }

    final Object o = selection.iterator().next();
    return o instanceof TfsTreeNode ? new SelectedItem(((TfsTreeNode)o)) : null;
  }

  public void initialize(@NotNull ServerInfo server,
                         @Nullable String initialSelection,
                         boolean foldersOnly,
                         boolean canCreateVirtualFolders,
                         @Nullable Condition<String> pathFilter) {
    myCanCreateVirtualFolders = canCreateVirtualFolders;
    TfsTreeNode root = new TfsTreeNode(myTree, server, foldersOnly, pathFilter);
    myTreeBuider = TfsTreeBuilder.createInstance(root, myTree);
    Disposer.register(this, myTreeBuider);

    final TfsTreeNode selection = root.createForSelection(initialSelection);
    if (selection != null) {
      myTreeBuider.select(selection);
    }
  }

  public void addListener(SelectionListener selectionListener) {
    myEventDispatcher.addListener(selectionListener, this);
  }

  public JComponent getContentPane() {
    return myContentPane;
  }

  public JComponent getPreferredFocusedComponent() {
    return myTree;
  }

  @Override
  public void dispose() {
  }

  @Override
  public Object getData(@NonNls String dataId) {
    if (KEY.is(dataId)) {
      return this;
    }
    return null;
  }

  public void createVirtualFolder(String folderName) {
    final Set<Object> selection = myTreeBuider.getSelectedElements();
    if (selection.isEmpty()) {
      return;
    }

    final Object o = selection.iterator().next();
    if (!(o instanceof TfsTreeNode)) {
      return;
    }
    TfsTreeNode treeNode = (TfsTreeNode)o;
    final TfsTreeNode child = treeNode.createVirtualSubfolder(folderName);
    myTreeBuider.queueUpdateFrom(treeNode, true).doWhenDone(new Runnable() {
      @Override
      public void run() {
        myTreeBuider.select(child);
      }
    });
  }

  public boolean canCreateVirtualFolders() {
    return myCanCreateVirtualFolders;
  }

  public void setMessage(String text, boolean error) {
    if (text != null) {
      myMessagePanel.setVisible(true);
      myMessageLabel.setText(text);
      myMessageLabel.setIcon(error ? UIUtil.getBalloonWarningIcon() : EMPTY_ICON);
    }
    else {
      myMessagePanel.setVisible(false);
    }
  }

}
