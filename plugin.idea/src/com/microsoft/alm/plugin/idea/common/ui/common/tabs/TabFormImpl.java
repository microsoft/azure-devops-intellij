// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.common.tabs;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.util.ui.JBUI;
import com.microsoft.alm.plugin.idea.common.resources.Icons;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.controls.Hyperlink;
import com.microsoft.alm.plugin.idea.common.ui.common.ActionListenerContainer;
import com.microsoft.alm.plugin.idea.common.ui.common.FeedbackAction;
import com.microsoft.alm.plugin.idea.common.ui.common.FilteredModel;
import com.microsoft.alm.plugin.idea.common.ui.common.SwingHelper;
import com.microsoft.alm.plugin.idea.common.ui.common.ToolbarToggleButton;
import com.microsoft.alm.plugin.idea.common.ui.common.VcsTabStatus;
import com.microsoft.alm.plugin.idea.common.ui.controls.SearchFilter;
import com.microsoft.alm.plugin.operations.Operation;
import org.apache.commons.lang.StringUtils;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.List;

/**
 * Common functionality for all tab views
 */
public abstract class TabFormImpl<T extends FilteredModel> implements TabForm<T> {
    private final String tabTitle;
    private final String createDialogTitle;
    private final String refreshTooltip;
    private final String toolbarLocation;
    private final ActionListenerContainer listenerContainer = new ActionListenerContainer();

    protected JPanel tabPanel;
    protected JScrollPane scrollPanel;
    protected JLabel statusLabel;
    protected Hyperlink statusLink;
    protected SearchFilter searchFilter;
    protected ToolbarToggleButton autoRefreshToggleButton;

    private boolean initialized = false;
    protected Timer timer;

    public TabFormImpl(final String tabTitle, final String createDialogTitle, final String refreshTooltip, final String toolbarLocation) {
        this.tabTitle = tabTitle;
        this.createDialogTitle = createDialogTitle;
        this.refreshTooltip = refreshTooltip;
        this.toolbarLocation = toolbarLocation;
    }

    /**
     * Create and return the tab panel
     *
     * @return
     */
    public JComponent getPanel() {
        ensureInitialized();
        return tabPanel;
    }

    /**
     * Create the custom view for the tab (i.e. tree, table, etc.)
     */
    protected abstract void createCustomView();

    /**
     * Adds custom items to the toolbar
     */
    protected abstract void addCustomTools(final JPanel toolBar);

    /**
     * Sets the view model that is used for the tab
     *
     * @param modelView
     */
    public abstract void setModelForView(final T modelView);

    /**
     * Create the tab view if not already done
     */
    protected void ensureInitialized() {
        if (!initialized) {
            createCustomView();
            createFilterToolbar();

            //toolbars
            final JPanel toolBarPanel;
            if (ApplicationManager.getApplication() != null) {
                final ActionToolbar prActionsToolbar = createToolbar(createActionsGroup());
                final ActionToolbar feedbackActionsToolbar = createToolbar(createFeedbackGroup());
                final ActionToolbar optionsActionsToolbar = createToolbar(createOptionsGroup());

                // left panel of the top toolbar
                final FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT, 0, JBUI.scale(3)); // give vertical padding
                final JPanel toolBarPanelLeft = new JPanel(flowLayout);
                toolBarPanelLeft.add(prActionsToolbar.getComponent());
                toolBarPanelLeft.add(searchFilter);
                addCustomTools(toolBarPanelLeft);

                // middle panel of the top toolbar
                final FlowLayout flowLayout2 = new FlowLayout(FlowLayout.LEFT, 0, JBUI.scale(3)); // give vertical padding
                final JPanel toolBarPanelMiddle = new JPanel(flowLayout2);
                toolBarPanelMiddle.add(optionsActionsToolbar.getComponent());
                SwingHelper.setMargin(toolBarPanelMiddle, new Insets(JBUI.scale(2), JBUI.scale(15), 0, 0));

                //entire top toolbar
                toolBarPanel = new JPanel(new BorderLayout());
                toolBarPanel.add(toolBarPanelLeft, BorderLayout.LINE_START);
                toolBarPanel.add(toolBarPanelMiddle, BorderLayout.CENTER);
                toolBarPanel.add(feedbackActionsToolbar.getComponent(), BorderLayout.LINE_END);
            } else {
                //skip setup when called from unit tests
                toolBarPanel = new JPanel();
            }

            //status panel with label and link
            final JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            statusLabel = new JLabel();
            statusLink = new Hyperlink();
            statusLink.setActionCommand(CMD_STATUS_LINK);
            statusPanel.add(statusLabel);
            statusPanel.add(statusLink);

            //tabPanel
            tabPanel = new JPanel(new BorderLayout());
            tabPanel.add(toolBarPanel, BorderLayout.PAGE_START);
            tabPanel.add(scrollPanel, BorderLayout.CENTER);
            tabPanel.add(statusPanel, BorderLayout.PAGE_END);
            this.initialized = true;
        }
    }

    /**
     * Creates the toolbar for top of the tab
     *
     * @param actions
     * @return
     */
    protected ActionToolbar createToolbar(DefaultActionGroup actions) {
        final ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(toolbarLocation, actions, false);
        toolbar.setOrientation(SwingConstants.HORIZONTAL);
        toolbar.setTargetComponent(scrollPanel);
        return toolbar;
    }

    /**
     * Create the action buttons for the toolbar
     *
     * @return action toolbar
     */
    protected DefaultActionGroup createActionsGroup() {
        final AnAction createAction = new AnAction(TfPluginBundle.message(createDialogTitle),
                TfPluginBundle.message(createDialogTitle),
                AllIcons.ToolbarDecorator.Add) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                listenerContainer.triggerEvent(this, CMD_CREATE_NEW_ITEM);
            }
        };
        createAction.registerCustomShortcutSet(CommonShortcuts.getNew(), scrollPanel); //Ctrl+N on windows or Cmd+M on Mac

        final AnAction refreshAction = new AnAction(TfPluginBundle.message(refreshTooltip),
                TfPluginBundle.message(refreshTooltip), AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                listenerContainer.triggerEvent(this, CMD_REFRESH);
            }
        };
        refreshAction.registerCustomShortcutSet(CommonShortcuts.getRerun(), scrollPanel); //Ctrl+R on windows or Cmd+R on Mac

        return new DefaultActionGroup(createAction, refreshAction);
    }

    /**
     * Create the option buttons for the toolbar
     *
     * @return action group
     */
    protected DefaultActionGroup createOptionsGroup() {
        autoRefreshToggleButton = new ToolbarToggleButton(
                TfPluginBundle.message(TfPluginBundle.KEY_VCS_AUTO_REFRESH),
                true,
                CMD_AUTO_REFRESH_CHANGED);
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(autoRefreshToggleButton);
        return group;
    }


    /**
     * Create the feedback portion of the toolbar
     *
     * @return feedback toolbar
     */
    protected DefaultActionGroup createFeedbackGroup() {
        final AnAction sendFeedback = new AnAction(TfPluginBundle.message(TfPluginBundle.KEY_FEEDBACK_DIALOG_TITLE),
                TfPluginBundle.message(TfPluginBundle.KEY_FEEDBACK_DIALOG_TITLE), Icons.Smile) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                final FeedbackAction action = new FeedbackAction(anActionEvent.getProject(),
                        TfPluginBundle.message(tabTitle));
                action.actionPerformed(new ActionEvent(anActionEvent.getInputEvent().getSource(),
                        anActionEvent.getInputEvent().getID(), CMD_SEND_FEEDBACK));
            }
        };

        return new DefaultActionGroup(sendFeedback);
    }

    /**
     * Create the search filter for the toolbar
     */
    protected void createFilterToolbar() {
        // Create timer for filtering the list
        timer = new Timer(400, null);
        timer.setInitialDelay(400);
        timer.setActionCommand(CMD_FILTER_CHANGED);
        timer.setRepeats(false);

        searchFilter = new SearchFilter();
        //initialize to empty string
        searchFilter.setFilterText(StringUtils.EMPTY);
    }

    /**
     * Set the tab's status in the toolbar
     *
     * @param status
     */
    public void setStatus(final VcsTabStatus status) {
        switch (status) {
            case NOT_TF_GIT_REPO:
                statusLabel.setText(TfPluginBundle.message(TfPluginBundle.KEY_ERRORS_NOT_TFS_REPO, TfPluginBundle.message(tabTitle).toLowerCase()));
                statusLabel.setIcon(AllIcons.General.Error);
                statusLink.setText(TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_DIALOG_TITLE));
                statusLink.setVisible(true);
                break;
            case NO_AUTH_INFO:
                statusLabel.setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_NOT_AUTHENTICATED));
                statusLabel.setIcon(AllIcons.General.Error);
                statusLink.setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_SIGN_IN));
                statusLink.setVisible(true);
                break;
            case LOADING_IN_PROGRESS:
                //Loading in progress
                statusLabel.setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_LOADING));
                statusLabel.setIcon(AllIcons.General.Information);
                statusLink.setText("");
                statusLink.setVisible(false);
                break;
            case LOADING_COMPLETED_ERRORS:
                statusLabel.setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_LOADING_ERRORS, TfPluginBundle.message(tabTitle).toLowerCase()));
                statusLabel.setIcon(AllIcons.General.Warning);
                statusLink.setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_OPEN_IN_BROWSER));
                statusLink.setVisible(true);
                break;
            case LOADING_COMPLETED:
                //loading complete
                statusLabel.setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_LAST_REFRESHED_AT, new Date().toString()));
                statusLabel.setIcon(AllIcons.General.Information);
                statusLink.setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_OPEN_IN_BROWSER));
                statusLink.setVisible(true);

                break;
            default:
                //we shouldn't get here, nothing to do
                break;
        }
    }

    public void addActionListener(final ActionListener listener) {
        timer.addActionListener(listener);
        statusLink.addActionListener(listener);
        autoRefreshToggleButton.addActionListener(listener);
        listenerContainer.add(listener);
    }

    protected void triggerEvent(String event) {
        listenerContainer.triggerEvent(this, event);
    }

    /**
     * Display popup menu on the view
     *
     * @param component
     * @param x
     * @param y
     * @param listener
     */
    protected void showPopupMenu(final Component component, final int x, final int y, final ActionListener listener) {
        final JBPopupMenu menu = new JBPopupMenu();
        final List<JBMenuItem> openMenuItems = getMenuItems(listener);
        for (JBMenuItem menuItem : openMenuItems) {
            menu.add(menuItem);
        }
        menu.show(component, x, y);
    }

    /**
     * Creates and returns the menu items to be shown in the popup menu
     *
     * @param listener
     * @return list of menu items
     */
    protected abstract List<JBMenuItem> getMenuItems(final ActionListener listener);

    /**
     * Creates a menu item to use in the popup menu
     *
     * @param resourceKey
     * @param icon
     * @param actionCommand
     * @param listener
     * @return menu item
     */
    protected JBMenuItem createMenuItem(final String resourceKey, final Icon icon, final String actionCommand, final ActionListener listener) {
        final String text = TfPluginBundle.message(resourceKey);
        final JBMenuItem menuItem = new JBMenuItem(text, icon);
        menuItem.setActionCommand(actionCommand);
        menuItem.addActionListener(listener);
        return menuItem;
    }

    public void setFilter(final String filterString) {
        searchFilter.setFilterText(filterString);
    }

    public String getFilter() {
        return searchFilter.getFilterText();
    }

    public void setAutoRefresh(final boolean autoRefresh) {
        autoRefreshToggleButton.setSelected(null, autoRefresh);
    }

    public boolean getAutoRefresh() {
        return autoRefreshToggleButton.isSelected(null);
    }

    public abstract Operation.CredInputsImpl getOperationInputs();

    public abstract void refresh();

    @VisibleForTesting
    String getStatusText() {
        return statusLabel.getText();
    }

    @VisibleForTesting
    String getStatusLinkText() {
        return statusLink.getText();
    }
}
