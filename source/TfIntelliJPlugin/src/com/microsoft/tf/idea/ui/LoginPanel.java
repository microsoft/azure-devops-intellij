package com.microsoft.tf.idea.ui;

import com.intellij.ide.BrowserUtil;
import com.intellij.ui.HyperlinkAdapter;
import com.microsoft.tf.common.utils.AuthenticationType;
import com.microsoft.tf.common.utils.UrlHelper;
import com.microsoft.tf.idea.resources.TfPluginBundle;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Created by madhurig on 7/20/2015.
 */
public class LoginPanel {

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

    private AuthenticationType authenticationType;

    @NonNls
    private final String Auth_Alternate_RelativePath = "/_details/security/altcreds";
    @NonNls
    private final String Auth_PAT_RelativePath = "/_details/security/tokens";
    @NonNls
    private final String DefaultInfoMsgText = "Visual Studio Online ";
    @NonNls
    private final String DefaultInfoLink = "https://www.visualstudio.com/products/what-is-visual-studio-online-vs.aspx";


    public LoginPanel() {
        init();
    }

    private final void init() {
        populateAuthenticationCombo();
        setAuthenticationType(AuthenticationType.WINDOWS);
        initInfoPanel();
        setInfo(AuthenticationType.WINDOWS);

        authenticationComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String item = e.getItem().toString();
                    if (item.equals(TfPluginBundle.Auth_Windows)) {
                        setAuthenticationType(AuthenticationType.WINDOWS);
                    } else if (item.equals(TfPluginBundle.Auth_Alternate)) {
                        setAuthenticationType(AuthenticationType.ALTERNATE_CREDENTIALS);
                    } else if (item.equals(TfPluginBundle.Auth_PAT)) {
                        setAuthenticationType(AuthenticationType.PERSONAL_ACCESS_TOKEN);
                    }
                }
            }
        });
    }

    private final void populateAuthenticationCombo() {
        authenticationComboBox.removeAllItems();
        authenticationComboBox.addItem(TfPluginBundle.Auth_Windows);
        authenticationComboBox.addItem(TfPluginBundle.Auth_Alternate);
        authenticationComboBox.addItem(TfPluginBundle.Auth_PAT);
    }

    private final void setAuthenticationType(AuthenticationType authenticationType) {
        this.authenticationType = authenticationType;
        if(authenticationType.equals(AuthenticationType.WINDOWS)) {
            authenticationComboBox.setSelectedItem(TfPluginBundle.Auth_Windows);
            hideTokenFields();
        }
        else if(authenticationType.equals(AuthenticationType.ALTERNATE_CREDENTIALS)) {
            authenticationComboBox.setSelectedItem(TfPluginBundle.Auth_Alternate);
                    hideTokenFields();
        }
        else if(authenticationType.equals((AuthenticationType.PERSONAL_ACCESS_TOKEN))) {
            authenticationComboBox.setSelectedItem(TfPluginBundle.Auth_PAT);
                    showTokenFields();
        }
        else {
            //TODO: log error
        }

        setInfo(authenticationType);
    }

    private final void hideTokenFields() {
        tokenLabel.setVisible(false);
        tokenPasswordField.setVisible(false);

        //ensure user name, password are visible
        userNameLabel.setVisible(true);
        userNameTextField.setVisible(true);

        passwordLabel.setVisible(true);
        passwordField.setVisible(true);

        savePasswordCheckBox.setText(TfPluginBundle.SavePassword);
    }

    private final void showTokenFields() {
        tokenLabel.setVisible(true);
        tokenPasswordField.setVisible(true);

        //ensure user name, password are hidden
        userNameLabel.setVisible(false);
        userNameTextField.setVisible(false);

        passwordLabel.setVisible(false);
        passwordField.setVisible(false);

        savePasswordCheckBox.setText(TfPluginBundle.SaveToken);
    }

    private final void initInfoPanel() {
        infoTextPane.setMargin(new Insets(5, 0, 0, 0));
        infoTextPane.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            protected void hyperlinkActivated(final HyperlinkEvent e) {
                BrowserUtil.browse(e.getURL());
            }
        });
        infoTextPane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        infoTextPane.setText(constructMsgLink(DefaultInfoMsgText, DefaultInfoLink, TfPluginBundle.GetStarted));
    }

    private final void setInfo(AuthenticationType auth) {
        String serverUrl = serverUrlTextField.getText();
        if(UrlHelper.isValidServerUrl(serverUrl)) {
            if(auth.equals(AuthenticationType.WINDOWS)) {
                infoTextPane.setText(constructMsgLink(DefaultInfoMsgText, DefaultInfoLink, TfPluginBundle.GetStarted));
            }
            else if (auth.equals(AuthenticationType.ALTERNATE_CREDENTIALS)) {
                String fullPath = serverUrl.concat(Auth_Alternate_RelativePath);
                String msg = TfPluginBundle.SetupAlternateCredentials;
                infoTextPane.setText(constructMsgLink("", fullPath, msg));
            }
            else if(auth.equals(AuthenticationType.PERSONAL_ACCESS_TOKEN)) {
                String fullPath = serverUrl.concat(Auth_PAT_RelativePath);
                String msg = TfPluginBundle.GenerateToken;
                infoTextPane.setText(constructMsgLink("", fullPath, msg));
            }
        }
        else {
            //fallback message
            infoTextPane.setText(constructMsgLink(DefaultInfoMsgText, DefaultInfoLink, TfPluginBundle.GetStarted));
        }
    }

    private final String constructMsgLink(String text, String link, String msg) {
        return "<html>" + text + "<a href=\"" + link + "\">" + msg + "</a></html>"; //TODO: need sanitizing utils for html
    }

    public final JPanel getPanel() {
        return vsoLoginPanel;
    }

    public final JComponent getPreferredFocusedComponent() {
        return serverUrlTextField;
    }

    public final String getServerUrl() {
        return serverUrlTextField.getText().trim();
    }

    public final AuthenticationType getAuthenticationType() {
        return authenticationType;
    }

    public final String getUserName() {
        return userNameTextField.getText().trim();
    }

    public final String getPassword() {
        if(authenticationType.equals(AuthenticationType.WINDOWS)
            || authenticationType.equals(AuthenticationType.ALTERNATE_CREDENTIALS)) {
            return String.valueOf(passwordField.getPassword());
        }
        else if(authenticationType.equals(AuthenticationType.PERSONAL_ACCESS_TOKEN)) {
            return String.valueOf(tokenPasswordField.getPassword());
        }
        else{
            //unsupport authentication type
            return null;
        }
    }
}
