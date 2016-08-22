// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.common;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.util.ui.JBUI;
import com.microsoft.alm.plugin.idea.common.services.LocalizationServiceImpl;
import com.microsoft.alm.plugin.telemetry.TfsTelemetryConstants;
import com.microsoft.alm.plugin.telemetry.TfsTelemetryHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This base dialog implementation provides telemetry and a Tab panel for forms to added to.
 * If you do not need tabs, you can simply override createCenterPanel and return your own panel.
 */
public class BaseDialogImpl extends DialogWrapper implements BaseDialog {
    private JTabbedPane tabPanel;
    private ActionListenerContainer listenerContainer = new ActionListenerContainer();
    private ValidationListenerContainer validationListenerContainer = new ValidationListenerContainer();
    private final Project project;
    private final boolean showFeedback;
    private final String feedbackContext;
    private final Map<String, Object> properties;

    public BaseDialogImpl(final Project project, final String title, final String okButtonText, final String feedbackContext) {
        this(project, title, okButtonText, feedbackContext, true, null);
    }

    public BaseDialogImpl(final Project project, final String title, final String okButtonText, final String feedbackContext, final boolean showFeedback, final Map<String, Object> properties) {
        super(project);
        this.showFeedback = showFeedback;
        this.feedbackContext = feedbackContext;
        this.project = project;
        this.properties = properties != null ? new HashMap<String, Object>(properties) : Collections.<String, Object>emptyMap();

        super.setTitle(title);
        super.setOKButtonText(okButtonText);
        super.init();

        // Make a telemetry entry for this UI dialog
        TfsTelemetryHelper.getInstance().sendDialogOpened(this.getClass().getName(),
                new TfsTelemetryHelper.PropertyMapBuilder()
                        .activeServerContext()
                        .pair(TfsTelemetryConstants.PLUGIN_EVENT_PROPERTY_DIALOG, title)
                        .build());
    }

    protected Project getProject() {
        return project;
    }

    protected Object getProperty(final String name) {
        return properties.get(name);
    }

    @NotNull
    @Override
    protected Action[] createLeftSideActions() {
        if (showFeedback) {
            final Action[] actions = new Action[1];
            actions[0] = new FeedbackAction(project, feedbackContext);
            return actions;
        }

        return super.createLeftSideActions();
    }

    /**
     * There is a default implementation here, but subclasses can override this if they don't need tabbed pages.
     */
    @Override
    protected JComponent createCenterPanel() {
        tabPanel = new JTabbedPane();
        tabPanel.setPreferredSize(new Dimension(JBUI.scale(500), JBUI.scale(600)));
        tabPanel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                doTabChangedAction();
            }
        });
        return tabPanel;
    }

    @Nullable
    @Override
    protected String getDimensionServiceKey() {
        return feedbackContext;
    }

    protected void doTabChangedAction() {
        listenerContainer.triggerEvent(this, CMD_TAB_CHANGED);
    }

    @Override
    protected void doOKAction() {
        listenerContainer.triggerEvent(this, CMD_OK);
        super.doOKAction();
    }

    @Override
    public void doCancelAction() {
        listenerContainer.triggerEvent(this, CMD_CANCEL);
        super.doCancelAction();
    }

    @Override
    protected ValidationInfo doValidate() {
        try {
            return validationListenerContainer.doValidate();
        } catch (final Throwable t) {
            return new ValidationInfo(LocalizationServiceImpl.getInstance().getExceptionMessage(t));
        }
    }

    @Override
    public void addTabPage(final String text, final JComponent component) {
        if (tabPanel != null) {
            // Add some margin to the page
            SwingHelper.setMargin(component, JBUI.scale(10));
            tabPanel.addTab(text, component);
        }
    }

    @Override
    public int getSelectedTabIndex() {
        if (tabPanel != null) {
            return tabPanel.getSelectedIndex();
        }

        return -1;
    }

    @Override
    public void setSelectedTabIndex(final int index) {
        if (tabPanel != null) {
            tabPanel.setSelectedIndex(index);
        }
    }

    /**
     * This method returns the correct JComponent to set focus on from within the selected tab.
     */
    @Override
    public JComponent getPreferredFocusedComponent() {
        if (tabPanel != null) {
            final int i = getSelectedTabIndex();
            final Component tab = tabPanel.getComponentAt(i);
            if (tab instanceof FocusableTabPage) {
                return ((FocusableTabPage) tab).getPreferredFocusedComponent();
            }
            return tabPanel;
        }
        return super.getPreferredFocusedComponent();
    }

    @Override
    public void setOkEnabled(final boolean enabled) {
        super.setOKActionEnabled(enabled);
    }

    @Override
    public void addActionListener(final ActionListener listener) {
        listenerContainer.add(listener);
    }

    @Override
    public void addValidationListener(final ValidationListener listener) {
        validationListenerContainer.add(listener);
    }

    @Override
    public void displayError(final String message) {
        this.setErrorText(message);
    }

    @Override
    public boolean showModalDialog() {
        return super.showAndGet();
    }
}
