// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.servertree;

import com.intellij.ide.util.treeView.AbstractTreeBuilder;
import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.treeStructure.SimpleTreeStructure;
import org.jetbrains.annotations.NotNull;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.Comparator;

public class TfsTreeBuilder extends AbstractTreeBuilder {
    private static final Logger LOG = Logger.getInstance(TfsTreeBuilder.class.getName());

    private static final Comparator<NodeDescriptor> COMPARATOR = new Comparator<NodeDescriptor>() {
        public int compare(NodeDescriptor o1, NodeDescriptor o2) {
            if (o1 instanceof TfsErrorTreeNode) {
                return o2 instanceof TfsErrorTreeNode ? ((TfsErrorTreeNode) o1).getMessage().compareTo(((TfsErrorTreeNode) o2).getMessage()) : -1;
            } else if (o2 instanceof TfsErrorTreeNode) {
                return 1;
            }

            final TfsTreeNode n1 = (TfsTreeNode) o1;
            final TfsTreeNode n2 = (TfsTreeNode) o2;
            if (n1.isDirectory() && !n2.isDirectory()) {
                return -1;
            } else if (!n1.isDirectory() && n2.isDirectory()) {
                return 1;
            }

            return n1.getFileName().compareToIgnoreCase(n2.getFileName());
        }
    };

    public static TfsTreeBuilder createInstance(@NotNull final TfsTreeNode root, @NotNull final JTree tree) {
        final DefaultTreeModel treeModel = new DefaultTreeModel(new DefaultMutableTreeNode(root));
        tree.setModel(treeModel);
        return new TfsTreeBuilder(tree, treeModel, new SimpleTreeStructure.Impl(root) {
            @Override
            public boolean isToBuildChildrenInBackground(Object element) {
                return true;
            }

            @Override
            public boolean isAlwaysLeaf(Object element) {
                if (element instanceof TfsTreeNode) {
                    return !((TfsTreeNode) element).isDirectory();
                } else {
                    LOG.assertTrue(element instanceof TfsErrorTreeNode);
                    return true;
                }
            }
        });
    }

    public TfsTreeBuilder(final JTree tree, final DefaultTreeModel treeModel, final AbstractTreeStructure treeStructure) {
        super(tree, treeModel, treeStructure, COMPARATOR);
    }

    @Override
    protected void runBackgroundLoading(@NotNull final Runnable runnable) {
        if (isDisposed()) return;
        runnable.run();
    }

    @Override
    protected boolean isAutoExpandNode(final NodeDescriptor nodeDescriptor) {
        if (nodeDescriptor instanceof TfsErrorTreeNode) {
            return true;
        }
        if (nodeDescriptor instanceof TfsTreeNode) {
            return !((TfsTreeNode) nodeDescriptor).isDirectory() || ((TfsTreeNode) nodeDescriptor).isRoot();
        }
        return false;
    }

    @Override
    protected boolean isAlwaysShowPlus(final NodeDescriptor descriptor) {
        if (descriptor instanceof TfsTreeNode) {
            return ((TfsTreeNode) descriptor).isDirectory();
        } else {
            LOG.assertTrue(descriptor instanceof TfsErrorTreeNode);
            return false;
        }
    }
}
