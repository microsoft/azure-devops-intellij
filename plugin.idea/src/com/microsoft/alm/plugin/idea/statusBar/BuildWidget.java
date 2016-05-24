// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.statusBar;

import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.util.Consumer;
import com.microsoft.alm.client.utils.StringUtil;
import com.microsoft.alm.plugin.idea.resources.Icons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;

public class BuildWidget implements StatusBarWidget, StatusBarWidget.IconPresentation, Consumer<MouseEvent> {
    private BuildStatusModel model;
    private StatusBar statusBar;

    public static String getID() {
        return BuildWidget.class.getName();
    }

    public void update(final BuildStatusModel model) {
        this.model = model;
    }

    // StatusBarWidget //
    @NotNull
    @Override
    public String ID() {
        return getID();
    }

    @Nullable
    @Override
    public WidgetPresentation getPresentation(@NotNull final PlatformType type) {
        // This class implements the WidgetPresentation needed here
        return this;
    }

    @Override
    public void install(@NotNull final StatusBar statusBar) {
        this.statusBar = statusBar;
    }

    @Override
    public void dispose() {
        // Release any referenced classes
        this.statusBar = null;
    }

    // IconPresentation //
    @NotNull
    @Override
    public Icon getIcon() {
        if (model == null || !model.hasStatusInformation()) {
            return Icons.BUILD_STATUS_UNKNOWN;
        } else if (model.isSuccessful()) {
            return Icons.BUILD_STATUS_SUCCEEDED;
        } else {
            return Icons.BUILD_STATUS_FAILED;
        }
    }

    @Nullable
    @Override
    public String getTooltipText() {
        return model != null ? model.getDescription() : StringUtil.EMPTY;
    }

    @Nullable
    @Override
    public Consumer<MouseEvent> getClickConsumer() {
        return this;
    }

    @Override
    public void consume(final MouseEvent mouseEvent) {
        if (model != null) {
            final BuildPopup popup = new BuildPopup(model);
            final Component c = mouseEvent.getComponent();
            if (c != null) {
                // Place the popup above and to the left of the status bar icon
                final Dimension d = popup.getPreferredSize();
                popup.show(c, c.getWidth() - d.width, 0 - d.height);
            }
        }
    }
}
