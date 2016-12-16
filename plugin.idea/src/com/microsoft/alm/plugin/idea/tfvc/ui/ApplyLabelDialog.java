// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.alm.plugin.external.models.ItemInfo;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.util.List;

public class ApplyLabelDialog extends DialogWrapper {

    private final Project project;
    private final List<ItemInfo> itemInfos;

    private ApplyLabelForm form;

    public ApplyLabelDialog(final Project project, final List<ItemInfo> itemInfos) {
        super(project, true);
        this.project = project;
        this.itemInfos = itemInfos;

        setTitle(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_LABEL_DIALOG_TITLE));

        init();
        setOKActionEnabled(false);
    }

    @Nullable
    protected JComponent createCenterPanel() {
        form = new ApplyLabelForm(project, itemInfos);

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

    public List<String> getLabelItemSpecs() {
        return form.getLabelItemSpecs();
    }

    @Override
    protected String getDimensionServiceKey() {
        return "TFS.ApplyLabel";
    }

}
