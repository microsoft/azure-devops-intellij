// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.servertree;

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
import com.microsoft.alm.plugin.context.ServerContext;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeSelectionModel;
import java.util.EventListener;
import java.util.Set;

public class TfsTreeForm implements Disposable, DataProvider {
    private boolean canCreateVirtualFolders;

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

        public SelectedItem(final TfsTreeNode treeNode) {
            this(treeNode.getPath(), treeNode.isDirectory());
        }
    }

    public static final DataKey<TfsTreeForm> KEY = DataKey.create("TfsTreeForm");
    public static final String POPUP_ACTION_GROUP = "TfsTreePopupMenu";
    public static final Icon EMPTY_ICON = new EmptyIcon(0, UIUtil.getBalloonWarningIcon().getIconHeight());

    private JComponent contentPane;
    private Tree tree;
    private JTextField pathField;
    private JLabel messageLabel;
    private JPanel messagePanel;
    private TfsTreeBuilder treeBuider;
    private EventDispatcher<SelectionListener> eventDispatcher = EventDispatcher.create(SelectionListener.class);
    private SelectedItem selectedItem; // have to cache selected item to be available after form is disposed

    public TfsTreeForm() {
        DataManager.registerDataProvider(tree, this);
        new TreeSpeedSearch(tree);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                selectedItem = doGetSelectedItem();
                pathField.setText(selectedItem != null ? selectedItem.path : null);
                eventDispatcher.getMulticaster().selectionChanged();
            }
        });
        PopupHandler.installPopupHandler(tree, POPUP_ACTION_GROUP, ActionPlaces.REMOTE_HOST_DIALOG_POPUP);
        setMessage(null, false);
    }

    @Nullable
    public SelectedItem getSelectedItem() {
        return selectedItem;
    }

    @Nullable
    public String getSelectedPath() {
        return selectedItem != null ? selectedItem.path : null;
    }

    @Nullable
    private SelectedItem doGetSelectedItem() {
        final Set<Object> selection = treeBuider.getSelectedElements();
        if (selection.isEmpty()) {
            return null;
        }

        final Object o = selection.iterator().next();
        return o instanceof TfsTreeNode ? new SelectedItem(((TfsTreeNode) o)) : null;
    }

    public void initialize(@NotNull final ServerContext serverContext,
                           @Nullable final String initialSelection,
                           final boolean foldersOnly,
                           final boolean canCreateVirtualFolders,
                           @Nullable final Condition<String> pathFilter) {
        this.canCreateVirtualFolders = canCreateVirtualFolders;
        final TfsTreeNode root = new TfsTreeNode(tree, serverContext, initialSelection, foldersOnly, pathFilter);
        treeBuider = TfsTreeBuilder.createInstance(root, tree);
        Disposer.register(this, treeBuider);

        final TfsTreeNode selection = root.createForSelection(initialSelection);
        if (selection != null) {
            treeBuider.select(selection);
        }
    }

    public void addListener(SelectionListener selectionListener) {
        eventDispatcher.addListener(selectionListener, this);
    }

    public JComponent getContentPane() {
        return contentPane;
    }

    public JComponent getPreferredFocusedComponent() {
        return tree;
    }

    @Override
    public void dispose() {
    }

    @Override
    public Object getData(@NonNls final String dataId) {
        if (KEY.is(dataId)) {
            return this;
        }
        return null;
    }

    public void createVirtualFolder(final String folderName) {
        final Set<Object> selection = treeBuider.getSelectedElements();
        if (selection.isEmpty()) {
            return;
        }

        final Object o = selection.iterator().next();
        if (!(o instanceof TfsTreeNode)) {
            return;
        }
        final TfsTreeNode treeNode = (TfsTreeNode) o;
        final TfsTreeNode child = treeNode.createVirtualSubfolder(folderName);
        treeBuider.queueUpdateFrom(treeNode, true).doWhenDone(new Runnable() {
            @Override
            public void run() {
                treeBuider.select(child);
            }
        });
    }

    public boolean canCreateVirtualFolders() {
        return canCreateVirtualFolders;
    }

    public void setMessage(final String text, final boolean error) {
        if (text != null) {
            messagePanel.setVisible(true);
            messageLabel.setText(text);
            messageLabel.setIcon(error ? UIUtil.getBalloonWarningIcon() : EMPTY_ICON);
        } else {
            messagePanel.setVisible(false);
        }
    }
}
