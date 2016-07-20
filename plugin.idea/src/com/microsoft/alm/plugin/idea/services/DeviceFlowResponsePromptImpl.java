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
import java.util.Calendar;
import java.util.Date;

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
                                    deviceFlowResponse.requestCancel();
                                    cancellationCallback.call("User cancelled the device flow dialog.");
                                } else if (BaseDialog.CMD_OK.equals(e.getActionCommand())) {
                                    /*
                                     *  If users clicked on OK, it means they should have completed auth flow on the
                                     *  browser.  Shorten the timeout to 5 seconds as this should be enough time to
                                     *  make one server call to get the token.  If left unchanged and user clicked on OK
                                     *  without completing the oauth flow in browser, we will hang their session for a
                                     *  long time.
                                     */
                                    deviceFlowResponse.getExpiresAt().setTime(new Date());
                                    deviceFlowResponse.getExpiresAt().add(Calendar.SECOND, 5);
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
