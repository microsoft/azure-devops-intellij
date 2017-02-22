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

package com.microsoft.alm.plugin.idea.tfvc.ui;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.BaseDialogImpl;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.util.Collections;

/**
 * Allows the user to add/remove/edit the Proxy URI for the current TFVC workspace.
 */
public class ProxySettingsDialog extends BaseDialogImpl {
    private static final String PROP_PROXY_URI = "proxyUri";

    private ProxySettingsForm form;

    public ProxySettingsDialog(final Project project, final String serverUri, final String proxyUri) {
        super(project, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_PROXY_DIALOG_TITLE, serverUri),
                TfPluginBundle.message(TfPluginBundle.KEY_TFVC_PROXY_DIALOG_OK_BUTTON),
                TfPluginBundle.KEY_TFVC_PROXY_DIALOG_TITLE, true,
                Collections.<String, Object>singletonMap(PROP_PROXY_URI, proxyUri));
    }

    @Nullable
    protected JComponent createCenterPanel() {
        form = new ProxySettingsForm((String) getProperty(PROP_PROXY_URI));
        return form.getContentPane();
    }

    private void updateButtons() {
        final String errorMessage = form.isValid() ? null : TfPluginBundle.message(TfPluginBundle.KEY_TFVC_PROXY_DIALOG_ERRORS_INVALID_URI);
        form.setMessage(errorMessage);
        setOkEnabled(form.isValid());
    }

    @Override
    protected void doOKAction() {
        if (form.isValid()) {
            super.doOKAction();
        } else {
            updateButtons();
            form.addListener(new ProxySettingsForm.Listener() {
                public void stateChanged() {
                    updateButtons();
                }
            });
        }
    }

    @Nullable
    public String getProxyUri() {
        return form.getProxyUri();
    }
}
