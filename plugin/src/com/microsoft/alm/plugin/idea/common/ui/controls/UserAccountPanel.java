// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.controls;

import com.microsoft.alm.plugin.idea.common.resources.Icons;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.SwingHelper;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;

/**
 * This class is a Panel that shows an icon, user account, and server name.
 * It also has a "sign out" link that will trigger the CMD_SIGN_OUT action.
 */
public class UserAccountPanel extends JPanel {
    public static final String CMD_SIGN_OUT = "signOut";
    private final Hyperlink hyperlink;
    private final JLabel accountLabel;
    private final JLabel serverLabel;
    private final IconPanel iconPanel;
    private boolean windowsAccount = true;

    public UserAccountPanel() {
        // Create controls
        hyperlink = new Hyperlink();
        hyperlink.setText("");
        hyperlink.setActionCommand(CMD_SIGN_OUT);
        accountLabel = new JLabel();
        serverLabel = new JLabel();
        iconPanel = new IconPanel();

        // Layout controls
        setLayout(new GridBagLayout());
        SwingHelper.addToGridBag(this, iconPanel, 0, 0, 1, 2, 0, 4);
        SwingHelper.addToGridBag(this, serverLabel, 1, 0, 1, 1, 0, 4);
        SwingHelper.addToGridBag(this, accountLabel, 1, 1, 1, 1, 0, 4);
        SwingHelper.addToGridBag(this, hyperlink, 1, 2, 1, 1, 5, 4);

        setWindowsAccount(windowsAccount);
        setPreferredSize(new Dimension(64, 32));
    }

    public boolean isWindowsAccount() {
        return windowsAccount;
    }

    public void setWindowsAccount(boolean windowsAccount) {
        this.windowsAccount = windowsAccount;
        final String signOutText = windowsAccount ?
                TfPluginBundle.message(TfPluginBundle.KEY_USER_ACCOUNT_PANEL_SWITCH_SERVER) :
                TfPluginBundle.message(TfPluginBundle.KEY_USER_ACCOUNT_PANEL_SIGN_OUT);
        hyperlink.setText(signOutText);
        iconPanel.setIcon(windowsAccount ? Icons.WindowsAccount : Icons.VsoAccount);
    }

    public void addActionListener(final ActionListener listener) {
        // Hook up all controls that add listeners
        hyperlink.addActionListener(listener);
    }


    public void setUserName(final String name) {
        accountLabel.setText(name);
    }

    public void setServerName(final String name) {
        serverLabel.setText(name);
    }
}
