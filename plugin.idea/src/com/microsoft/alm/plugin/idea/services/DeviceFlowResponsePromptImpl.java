// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.services;

import com.microsoft.alm.auth.oauth.DeviceFlowResponse;
import com.microsoft.alm.helpers.Action;
import com.microsoft.alm.plugin.idea.ui.authentication.DeviceFlowResponsePromptDialog;
import com.microsoft.alm.plugin.idea.ui.common.BaseDialog;
import com.microsoft.alm.plugin.idea.utils.IdeaHelper;
import com.microsoft.alm.plugin.services.DeviceFlowResponsePrompt;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Implements DeviceFlowResponsePrompt which creates a dialog to to show user
 * relevant information from the device flow response.
 */
public class DeviceFlowResponsePromptImpl implements DeviceFlowResponsePrompt {

    /**
     * {@inheritDoc}
     */
    @Override
    public Action<DeviceFlowResponse> getCallback(final Action<String> cancellationCallback) {

        return new Action<DeviceFlowResponse>() {
            @Override
            public void call(final DeviceFlowResponse deviceFlowResponse) {
                IdeaHelper.runOnUIThread(new Runnable() {

                    @Override
                    public void run() {
                        final DeviceFlowResponsePromptDialog promptDialog = new DeviceFlowResponsePromptDialog();;

                        promptDialog.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                if (BaseDialog.CMD_CANCEL.equals(e.getActionCommand())) {
                                    cancellationCallback.call("User cancelled the device flow dialog.");
                                }
                            }
                        });

                        promptDialog.setResponse(deviceFlowResponse);
                        promptDialog.show();
                    }
                }, true);
            }
        };
    }
}
