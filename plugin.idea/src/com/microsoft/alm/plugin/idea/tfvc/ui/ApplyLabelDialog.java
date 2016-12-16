// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.alm.plugin.external.models.ItemInfo;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.BaseDialogImpl;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ApplyLabelDialog extends BaseDialogImpl {
    public static final String PROP_ITEMS = "items";

    private ApplyLabelForm form;

    public ApplyLabelDialog(final Project project, final List<ItemInfo> itemInfos) {
        super(project, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_LABEL_DIALOG_TITLE),
                TfPluginBundle.message(TfPluginBundle.KEY_TFVC_LABEL_DIALOG_APPLY_LABEL),
                TfPluginBundle.KEY_TFVC_LABEL_DIALOG_TITLE, true,
                Collections.<String, Object>singletonMap(PROP_ITEMS, itemInfos));
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return form.getPreferredFocusedComponent();
    }

    @Nullable
    protected JComponent createCenterPanel() {
        form = new ApplyLabelForm(this.getProject(), (List<ItemInfo>) getProperty(PROP_ITEMS));
        setOKActionEnabled(false);

//    getWindow().addComponentListener(new ComponentAdapter() {
//      public void componentShown(final ComponentEvent e) {
//        form.addItems();
//      }
//    });

        form.addListener(new ApplyLabelForm.Listener() {
            public void dataChanged(final String labelName, final int visibleItemsCount) {
                setOKActionEnabled(visibleItemsCount > 0 && labelName.length() > 0);
            }
        });

        return form.getContentPane();
    }

    public String getLabelName() {
        return form.getLabelName();
    }

    public String getLabelComment() {
        return form.getLabelComment();
    }

    public boolean isRecursiveChecked() {
        return form.isRecursiveChecked();
    }

    public List<String> getLabelItemSpecs() {
        return form.getLabelItemSpecs();
    }
}
