// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.servertree;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.util.PlatformIcons;

public class TfsErrorTreeNode extends SimpleNode {
    private final String message;

    public TfsErrorTreeNode(final SimpleNode parent, final String message) {
        super(parent);
        this.message = message;
    }

    @Override
    protected void update(final PresentationData presentation) {
        super.update(presentation);
        presentation.addText(message, getErrorAttributes());
        presentation.setIcon(PlatformIcons.ERROR_INTRODUCTION_ICON);
    }

    @Override
    public SimpleNode[] getChildren() {
        return NO_CHILDREN;
    }

    public String getMessage() {
        return message;
    }
}
