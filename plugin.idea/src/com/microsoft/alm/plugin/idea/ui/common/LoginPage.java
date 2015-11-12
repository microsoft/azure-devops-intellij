// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common;

import javax.swing.JComponent;
import java.awt.event.ActionListener;

/**
 * This interface represents a page that can hold a LoginForm and another form.
 */
public interface LoginPage {
    /**
     * The setLoginShowing method allows the owner to determine which form should show.
     */
    void setLoginShowing(boolean showLogin);

    /**
     * The getPreferredFocusedComponent method allows the owner to get the control that should be focused.
     */
    JComponent getPreferredFocusedComponent();

    /**
     * The addActionListener method should attach the provided listener to all forms.
     */
    void addActionListener(ActionListener listener);

    /**
     * The getComponent method returns the control that best matches the property name provided.
     */
    JComponent getComponent(String name);

    /**
     * This is a pass thru method to LoginForm.setAuthenticating
     */
    void setAuthenticating(boolean authenticating);

    /**
     * This is a pass thru method to LoginForm.setServerName
     */
    void setServerName(String name);

    /**
     * This is a pass thru method to LoginForm.getServerName
     */
    String getServerName();
}
