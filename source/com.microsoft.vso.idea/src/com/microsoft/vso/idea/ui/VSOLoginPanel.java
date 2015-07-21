package com.microsoft.vso.idea.ui;

import com.microsoft.vso.idea.resources.VSOLoginBundle;
import com.intellij.ide.BrowserUtil;
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.util.ui.UIUtil;
import com.microsoft.vso.idea.utils.AuthenticationType;
import com.microsoft.vso.idea.utils.URLHelper;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

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
    private JPasswordField tokenPasswordField;
    private JTextPane infoTextPane;

    private final String Auth_Alternate_RelativePath = "/_details/security/altcreds";
    private final String Auth_PAT_RelativePath = "/_details/security/tokens";
    private final String DefaultInfoMsgText = "Visual Studio Online ";
    private final String DefaultInfoLink = "https://www.visualstudio.com/en-us/products/what-is-visual-studio-online-vs.aspx";


    public VSOLoginPanel() {
        init();
    }

    protected void init() {
        populateAuthenticationCombo();
        setAuthentication(AuthenticationType.WINDOWS);
        initInfoPanel();
        setInfo(AuthenticationType.WINDOWS);

        authenticationComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String item = e.getItem().toString();
                    if (item.equals(VSOLoginBundle.message(VSOLoginBundle.Auth_Windows))) {
                        setAuthentication(AuthenticationType.WINDOWS);
                    } else if (item.equals(VSOLoginBundle.message(VSOLoginBundle.Auth_Alternate))) {
                        setAuthentication(AuthenticationType.ALTERNATE_CREDENTIALS);
                    } else if (item.equals(VSOLoginBundle.message(VSOLoginBundle.Auth_PAT))) {
                        setAuthentication(AuthenticationType.PAT);
                    }
                }
            }
        });
    }

    private void populateAuthenticationCombo() {
        authenticationComboBox.removeAllItems();
        authenticationComboBox.addItem(VSOLoginBundle.message(VSOLoginBundle.Auth_Windows));
        authenticationComboBox.addItem(VSOLoginBundle.message(VSOLoginBundle.Auth_Alternate));
        authenticationComboBox.addItem(VSOLoginBundle.message(VSOLoginBundle.Auth_PAT));
    }

    private void setAuthentication(AuthenticationType auth) {
        if(auth.equals(AuthenticationType.WINDOWS)) {
            authenticationComboBox.setSelectedItem(VSOLoginBundle.message(VSOLoginBundle.Auth_Windows));
            hideTokenFields();
        }
        else if(auth.equals(AuthenticationType.ALTERNATE_CREDENTIALS)) {
            authenticationComboBox.setSelectedItem(VSOLoginBundle.message(VSOLoginBundle.Auth_Alternate));
                    hideTokenFields();
        }
        else if(auth.equals((AuthenticationType.PAT))) {
            authenticationComboBox.setSelectedItem(VSOLoginBundle.message(VSOLoginBundle.Auth_PAT));
                    showTokenFields();
        }
        else {
            //Ignore: unknown authentication
        }
        setInfo(auth);
    }

    private void hideTokenFields() {
        tokenLabel.setVisible(false);
        tokenPasswordField.setVisible(false);

        //ensure user name, password are visible
        userNameLabel.setVisible(true);
        userNameTextField.setVisible(true);

        passwordLabel.setVisible(true);
        passwordField.setVisible(true);
    }

    private void showTokenFields() {
        tokenLabel.setVisible(true);
        tokenPasswordField.setVisible(true);

        //ensure user name, password are hidden
        userNameLabel.setVisible(false);
        userNameTextField.setVisible(false);

        passwordLabel.setVisible(false);
        passwordField.setVisible(false);
    }

    private void initInfoPanel() {
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

    private void setInfo(AuthenticationType auth) {
        String serverUrl = serverUrlTextField.getText();
        if(URLHelper.isValidServerUrl(serverUrl)) {
            if(auth.equals(AuthenticationType.WINDOWS)) {
                infoTextPane.setText(constructMsgLink(DefaultInfoMsgText, DefaultInfoLink, VSOLoginBundle.message(VSOLoginBundle.GetStarted)));
            }
            else if (auth.equals(AuthenticationType.ALTERNATE_CREDENTIALS)) {
                String fullPath = serverUrl.concat(Auth_Alternate_RelativePath);
                String msg = VSOLoginBundle.message(VSOLoginBundle.SetupAlternateCredentials);
                infoTextPane.setText(constructMsgLink("", fullPath, msg));
            }
            else if(auth.equals(AuthenticationType.PAT)) {
                String fullPath = serverUrl.concat(Auth_PAT_RelativePath);
                String msg = VSOLoginBundle.message(VSOLoginBundle.GenerateToken);
                infoTextPane.setText(constructMsgLink("", fullPath, msg));
            }
        }
        else {
            //fallback message
            infoTextPane.setText(constructMsgLink(DefaultInfoMsgText, DefaultInfoLink, VSOLoginBundle.message(VSOLoginBundle.GetStarted)));
        }
    }

    private String constructMsgLink(String text, String link, String msg) {
        return "<html>" + text + "<a href=\"" + link + "\">" + msg + "</a></html>";
    }

    public JPanel getPanel() {
        return vsoLoginPanel;
    }

    public JComponent getPreferredFocusedComponent() {
        return serverUrlTextField;
    }
}
