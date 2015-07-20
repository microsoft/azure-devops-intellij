package com.microsoft.vso.idea.ui;

import com.microsoft.vso.idea.resources.VSOLoginBundle;
import com.intellij.ide.BrowserUtil;
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;

/**
 * Created by madhurig on 7/20/2015.
 */
public class VSOLoginPanel {

    private JPanel vsoLoginPanel;

    private JPanel serverInfoPanel;
    private JLabel serverUrlLabel;
    private JTextField serverUrlTextField;
    private JLabel authenticationLabel;
    private JComboBox authenticationComboBox;
    private JCheckBox savePasswordCheckBox;

    private JPanel loginPanel;
    private JLabel userNameLabel;
    private JTextField userNameTextField;
    private JLabel passwordLabel;
    private JPasswordField passwordField;
    private JLabel tokenLabel;
    private JTextField tokenTextField;
    private JTextPane infoTextPane;


    public VSOLoginPanel() {
        init();
    }

    protected void init() {
        authenticationComboBox.addItem(VSOLoginBundle.message("Auth_Windows"));
        authenticationComboBox.addItem(VSOLoginBundle.message("Auth_Alternate"));
        authenticationComboBox.addItem(VSOLoginBundle.message("Auth_PAT"));

        infoTextPane.setText("<html>Visual Studio Online <a href=\"https://www.visualstudio.com/en-us/products/what-is-visual-studio-online-vs.aspx\">Get started for FREE</a>.</html>");
        infoTextPane.setMargin(new Insets(5, 0, 0, 0));
        infoTextPane.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            protected void hyperlinkActivated(final HyperlinkEvent e) {
                BrowserUtil.browse(e.getURL());
            }
        });
        infoTextPane.setBackground(UIUtil.TRANSPARENT_COLOR);
        infoTextPane.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    public JPanel getPanel() {
        return vsoLoginPanel;
    }
}
