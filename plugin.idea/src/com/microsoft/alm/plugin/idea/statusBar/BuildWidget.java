// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.statusBar;

import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.util.Consumer;
import com.microsoft.alm.plugin.idea.resources.Icons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.awt.event.MouseEvent;

public class BuildWidget implements StatusBarWidget, StatusBarWidget.IconPresentation {
    private BuildStatusModel model = BuildStatusModel.EMPTY_STATUS;

    public static String getID() {
        return BuildWidget.class.getName();
    }

    public void update(BuildStatusModel model) {
        this.model = model != null ? model : BuildStatusModel.EMPTY_STATUS;
    }

    // StatusBarWidget //
    @NotNull
    @Override
    public String ID() {
        return getID();
    }

    @Nullable
    @Override
    public WidgetPresentation getPresentation(@NotNull PlatformType type) {
        // This class implements the WidgetPresentation needed here
        return this;
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {
        // Nothing to do here. This is used if we need a reference to the statusBar for something
    }

    @Override
    public void dispose() {
        // Release any referenced classes
    }

    // IconPresentation //
    @NotNull
    @Override
    public Icon getIcon() {
        if (!model.hasStatusInformation()) {
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
        return model.getDescription();
    }

    @Nullable
    @Override
    public Consumer<MouseEvent> getClickConsumer() {
        return null;
    }
}
