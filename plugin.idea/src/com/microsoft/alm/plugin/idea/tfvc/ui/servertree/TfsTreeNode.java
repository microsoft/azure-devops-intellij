// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.servertree;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.util.PlatformIcons;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.common.resources.Icons;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.VersionControlPath;
import com.microsoft.alm.plugin.idea.tfvc.exceptions.TfsException;
import com.microsoft.alm.sourcecontrol.webapi.model.TfvcItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TfsTreeNode extends SimpleNode {

    private static final SimpleTextAttributes VIRTUAL_ATTRS = SimpleTextAttributes.SYNTHETIC_ATTRIBUTES;

    private final TfsTreeContext treeContext;
    private final String path;
    private final boolean isDirectory;
    private final boolean isVirtual;
    private final Collection<TfsTreeNode> virtualChildren = new ArrayList<TfsTreeNode>();

    // root node
    public TfsTreeNode(@NotNull final Object projectOrComponent,
                       final ServerContext serverContext,
                       final String base,
                       final boolean foldersOnly,
                       @Nullable final Condition<String> pathFilter) {
        super(projectOrComponent instanceof Project ? (Project) projectOrComponent : null);
        treeContext = new TfsTreeContext(serverContext, foldersOnly, pathFilter);
        path = VersionControlPath.getPathToProject(base);
        isDirectory = true;
        isVirtual = false;
    }

    // child node
    private TfsTreeNode(final TfsTreeNode parent, final String path, final boolean isDirectory, final boolean virtual) {
        super(parent);
        this.path = path;
        this.isDirectory = isDirectory;
        isVirtual = virtual;
        treeContext = parent.treeContext;
    }

    @Override
    public SimpleNode[] getChildren() {
        if (!isDirectory) {
            return NO_CHILDREN;
        }

        List<TfvcItem> children;
        try {
            children = treeContext.getChildItems(path);
        } catch (TfsException e) {
            return new SimpleNode[]{new TfsErrorTreeNode(this, e.getMessage())};
        }

        final List<TfsTreeNode> result = new ArrayList<TfsTreeNode>(virtualChildren);
        for (final TfvcItem childItem : children) {
            result.add(new TfsTreeNode(this, childItem.getPath(), childItem.isFolder(), false));
        }
        return result.toArray(new SimpleNode[result.size()]);
    }

    @Override
    protected void update(final PresentationData presentation) {
        if (isRoot()) {
            //noinspection ConstantConditions
            presentation.addText(treeContext.serverContext.getUri().getPath(), getPlainAttributes());
            presentation.setIcon(Icons.VSLogoSmall);
        } else {
            if (isDirectory()) {
                presentation.setIcon(PlatformIcons.DIRECTORY_CLOSED_ICON);
            } else {
                presentation.setIcon(FileTypeManager.getInstance().getFileTypeByFileName(getFileName()).getIcon());
            }
            SimpleTextAttributes attrs;
            if (isVirtual) {
                attrs = VIRTUAL_ATTRS;
            } else if (treeContext.isAccepted(path)) {
                attrs = getPlainAttributes();
            } else {
                attrs = SimpleTextAttributes.GRAYED_ATTRIBUTES;
            }
            presentation.addText(getFileName(), attrs);
        }
    }

    public String getFileName() {
        return VersionControlPath.getLastComponent(path);
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public boolean isRoot() {
        return VersionControlPath.ROOT_FOLDER.equals(path);
    }

    @NotNull
    public String getPath() {
        return path;
    }

    private TfsTreeNode createFakeChild(final String name) {
        final String childPath = VersionControlPath.getCombinedServerPath(path, name);
        return new TfsTreeNode(this, childPath, false, false);
    }

    @NotNull
    @Override
    public Object[] getEqualityObjects() {
        return new Object[]{path};
    }

    @Nullable
    public TfsTreeNode createForSelection(final String serverPath) {
        if (StringUtil.isEmpty(serverPath) || VersionControlPath.ROOT_FOLDER.equals(serverPath)) {
            return this;
        }

        TfsTreeNode result = this;
        final String[] components = VersionControlPath.getPathComponents(serverPath);
        for (int i = 1; i < components.length; i++) {
            result = result.createFakeChild(components[i]);
        }
        return result;
    }

    public TfsTreeNode createVirtualSubfolder(final String folderName) {
        final String childPath = VersionControlPath.getCombinedServerPath(path, folderName);
        final TfsTreeNode child = new TfsTreeNode(this, childPath, true, true);
        virtualChildren.add(child);
        return child;
    }
}
