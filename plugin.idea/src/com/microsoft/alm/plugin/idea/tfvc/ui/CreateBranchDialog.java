// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ui.JBUI;
import com.microsoft.alm.plugin.context.ServerContext;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class CreateBranchDialog extends DialogWrapper {
    private final CreateBranchForm form;

    public CreateBranchDialog(final Project project, final ServerContext serverContext, final String serverPath, final boolean isDirectory) {
        super(project, true);
        form = new CreateBranchForm(project, serverContext, serverPath, isDirectory);
        form.addListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                revalidate();
            }
        });

        // TODO localize
        setTitle("Create Branch");
        setSize(JBUI.scale(380), JBUI.scale(450));

        init();
        revalidate();
    }

    @Nullable
    protected JComponent createCenterPanel() {
        return form.getContentPane();
    }

    private void revalidate() {
        setOKActionEnabled(StringUtil.isNotEmpty(form.getTargetPath()));
    }

    //@Nullable
    //public VersionSpecBase getVersionSpec() {
    //    return form.getVersionSpec();
    //}

    public String getTargetPath() {
        return form.getTargetPath();
    }

    public boolean isCreateWorkingCopies() {
        return form.isCreateWorkingCopies();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return form.getPreferredFocusedComponent();
    }

    @Override
    protected String getDimensionServiceKey() {
        return "TFS.CreateBranch";
    }

}