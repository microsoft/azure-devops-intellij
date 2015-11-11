// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common.forms;

import javax.swing.JComponent;
import java.awt.event.ActionListener;

public interface LoginForm extends BasicForm {
    String CMD_SIGN_IN = "signIn";
    String CMD_CREATE_ACCOUNT = "createAccount";
    String CMD_LEARN_MORE = "learnMore";
    String CMD_ENTER_KEY = "enterKey";

    String getServerName();

    void setServerName(String name);

    JComponent getServerNameComponent();

    void addActionListener(ActionListener listener);

    void setAuthenticating(boolean inProgress);
}
