// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

/*
 * Copyright 2000-2008 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.microsoft.alm.plugin.idea.tfvc.ui.servertree;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.containers.HashMap;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.BaseDialogImpl;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.util.Map;

public class ServerBrowserDialog extends BaseDialogImpl {
    private static final String PROP_SERVER_CONTEXT = "server_context";
    private static final String PROP_INITIAL_PATH = "initial_path";
    private static final String PROP_FOLDERS_ONLY = "folders_only";
    private static final String PROP_CAN_CREATE_VIRTUAL_FOLDERS = "can_create_virtual_folders";

    private TfsTreeForm treeForm;

    public ServerBrowserDialog(final String title,
                               final Project project,
                               final ServerContext serverContext,
                               @Nullable final String initialPath,
                               final boolean foldersOnly,
                               final boolean canCreateVirtualFolders) {
        super(project, title, TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_SERVER_TREE_SELECT_BUTTON), title,
                true, createProperties(serverContext, initialPath, foldersOnly, canCreateVirtualFolders));
    }

    private static Map<String, Object> createProperties(final ServerContext serverContext, final String initialPath, final boolean foldersOnly, final boolean canCreateVirtualFolders) {
        final Map<String, Object> properties = new HashMap<String, Object>(4);
        properties.put(PROP_SERVER_CONTEXT, serverContext);
        properties.put(PROP_INITIAL_PATH, initialPath);
        properties.put(PROP_FOLDERS_ONLY, foldersOnly);
        properties.put(PROP_CAN_CREATE_VIRTUAL_FOLDERS, canCreateVirtualFolders);
        return properties;
    }

    @Nullable
    protected JComponent createCenterPanel() {
        treeForm = new TfsTreeForm();
        treeForm.initialize((ServerContext) getProperty(PROP_SERVER_CONTEXT), (String) getProperty(PROP_INITIAL_PATH),
                (Boolean) getProperty(PROP_FOLDERS_ONLY), (Boolean) getProperty(PROP_CAN_CREATE_VIRTUAL_FOLDERS), null);
        treeForm.addListener(new TfsTreeForm.SelectionListener() {
            @Override
            public void selectionChanged() {
                setOkEnabled(treeForm.getSelectedItem() != null);
            }
        });
        Disposer.register(getDisposable(), treeForm);
        return treeForm.getContentPane();
    }

    @Nullable
    public String getSelectedPath() {
        return treeForm.getSelectedPath();
    }

    @Nullable
    public TfsTreeForm.SelectedItem getSelectedItem() {
        return treeForm.getSelectedItem();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return treeForm.getPreferredFocusedComponent();
    }
}
