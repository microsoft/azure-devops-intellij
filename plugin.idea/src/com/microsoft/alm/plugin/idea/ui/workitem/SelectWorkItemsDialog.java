// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.workitem;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.common.BaseDialogImpl;
import org.apache.commons.lang.StringUtils;

import javax.swing.JComponent;

public class SelectWorkItemsDialog extends BaseDialogImpl {
    private SelectWorkItemsController controller;

    public SelectWorkItemsDialog(final Project project) {
        super(project,
                TfPluginBundle.message(TfPluginBundle.KEY_WIT_SELECT_DIALOG_TITLE),
                TfPluginBundle.message(TfPluginBundle.KEY_WIT_SELECT_DIALOG_SELECT_BUTTON),
                TfPluginBundle.KEY_WIT_SELECT_DIALOG_TITLE);
    }

    @Override
    protected JComponent createCenterPanel() {
        if (controller == null) {
            controller = new SelectWorkItemsController(getProject());
        }
        return controller.getContentPanel();
    }

    public String getComment() {
        return controller.getComment();
    }

    @Override
    protected ValidationInfo doValidate() {
        if (StringUtils.isEmpty(getComment())) {
            return new ValidationInfo(TfPluginBundle.message(TfPluginBundle.KEY_WIT_SELECT_DIALOG_ERRORS_WORK_ITEM_NOT_SELECTED));
        }

        return super.doValidate();
    }
}
