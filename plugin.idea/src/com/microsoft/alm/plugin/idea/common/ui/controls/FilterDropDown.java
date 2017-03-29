// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.controls;

import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.ui.ClickListener;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * Abstract class that helps build the common functionality of a tab picker
 */
public abstract class FilterDropDown extends JPanel {
    protected final DefaultActionGroup group;
    private final ClickListener clickListener;
    private final KeyAdapter keyAdapter;

    protected JLabel titleLabel;
    protected JLabel pickerLabel;
    protected ActionListener listener;
    protected boolean isLoading = false;
    private boolean isEnabled = false;


    public FilterDropDown() {
        this.group = new DefaultActionGroup();

        this.clickListener = new ClickListener() {
            @Override
            public boolean onClick(MouseEvent event, int clickCount) {
                showDropDownMenu();
                return true;
            }
        };

        this.keyAdapter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER || keyEvent.getKeyCode() == KeyEvent.VK_DOWN) {
                    showDropDownMenu();
                }
            }
        };
    }

    protected abstract void populateDropDownMenu();

    public abstract String getSelectedResults();

    /**
     * Setup the UI components for the menu
     *
     * @param titleLabelText
     * @param pickerLabel
     */
    protected void initializeUI(final String titleLabelText, final JLabel pickerLabel) {
        setBorder(BorderFactory
                .createCompoundBorder(BorderFactory.createEmptyBorder(JBUI.scale(2), JBUI.scale(2), JBUI.scale(2), JBUI.scale(2)), JBUI.Borders.empty()));

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        this.titleLabel = new JLabel(titleLabelText + " ");
        this.pickerLabel = pickerLabel;

        add(this.titleLabel);
        add(this.pickerLabel);
        add(Box.createHorizontalStrut(JBUI.scale(3)));
        add(new JLabel(AllIcons.Ide.Statusbar_arrows));
    }

    /**
     * Refreshes the dropdown list (always called on tab initialization from the controller)
     */
    public void refreshDropDown(final boolean isTeamServicesRepository) {
        // checks to see if dorpdown state needs to be changed
        if (isTeamServicesRepository != isEnabled) {
            enableDropDown(isTeamServicesRepository);
        }

        if (isEnabled && !isLoading) {
            populateDropDownMenu();
        }
    }

    /**
     * Displays the dropdown menu with the latest items
     */
    private void showDropDownMenu() {
        if (isEnabled) {
            final ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(null, group, DataManager.getInstance().getDataContext(this), JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, true);
            popup.showUnderneathOf(this);
        }
    }

    /**
     * Add listeners to the component to detect user interactions
     */
    protected void addListeners() {
        // adds listener for mouse click
        clickListener.installOn(this);

        // adds listener for keyboard shortcut (mimics IntelliJ's Log tab shortcuts)
        addKeyListener(keyAdapter);
    }

    /**
     * Remove listeners from the component
     */
    protected void removeListeners() {
        clickListener.uninstall(this);
        removeKeyListener(keyAdapter);
    }

    public void addActionListener(final ActionListener listener) {
        this.listener = listener;
    }

    protected void enableDropDown(final boolean isTeamServicesRepository) {
        if (isTeamServicesRepository) {
            addListeners();
            pickerLabel.setForeground(JBColor.BLACK);
        } else {
            removeListeners();
            pickerLabel.setForeground(JBColor.GRAY);
        }
        isEnabled = isTeamServicesRepository;
    }

    /**
     * Loading action that is just a placeholder in the menu until the real results are returned from the server.
     * Always disabled and is just there to inform the users what is happening
     */
    protected class LoadingAction extends DumbAwareAction {

        public LoadingAction() {
            super(TfPluginBundle.message(TfPluginBundle.KEY_VCS_LOADING));
            this.getTemplatePresentation().setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_LOADING), false);
            this.getTemplatePresentation().setEnabled(false);
        }

        @Override
        public void actionPerformed(final AnActionEvent anActionEvent) {
            // do nothing if clicked on
        }
    }
}
