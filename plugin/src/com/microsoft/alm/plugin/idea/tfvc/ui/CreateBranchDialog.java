// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.microsoft.alm.plugin.idea.tfvc.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ui.JBUI;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.BaseDialogImpl;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.HashMap;
import java.util.Map;

public class CreateBranchDialog extends BaseDialogImpl {
    private static final String PROP_SERVER_CONTEXT = "server_context";
    private static final String PROP_SERVER_PATH = "server_path";
    private static final String PROP_IS_DIR = "is_directory";

    private CreateBranchForm form;

    public CreateBranchDialog(final Project project, final ServerContext serverContext, final String serverPath, final boolean isDirectory) {
        super(project, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_BRANCH_DIALOG_TITLE),
                TfPluginBundle.message(TfPluginBundle.KEY_TFVC_BRANCH_DIALOG_OK_BUTTON),
                TfPluginBundle.KEY_TFVC_BRANCH_DIALOG_TITLE, true,
                createProperties(serverContext, serverPath, isDirectory));
    }

    private static Map<String, Object> createProperties(final ServerContext serverContext, final String serverPath, final boolean isDirectory) {
        final Map<String, Object> properties = new HashMap<String, Object>(3);
        properties.put(PROP_SERVER_CONTEXT, serverContext);
        properties.put(PROP_SERVER_PATH, serverPath);
        properties.put(PROP_IS_DIR, isDirectory);
        return properties;
    }

    @Nullable
    protected JComponent createCenterPanel() {
        form = new CreateBranchForm(getProject(), (ServerContext) getProperty(PROP_SERVER_CONTEXT),
                (String) getProperty(PROP_SERVER_PATH), (Boolean) getProperty(PROP_IS_DIR));
        form.addListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                revalidate();
            }
        });
        setSize(JBUI.scale(380), JBUI.scale(450));
        revalidate();
        return form.getContentPane();
    }

    private void revalidate() {
        setOkEnabled(StringUtil.isNotEmpty(form.getTargetPath()));
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
}