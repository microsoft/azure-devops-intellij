// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.checkout.mocks;

import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.alm.plugin.idea.ui.common.BaseDialog;
import com.microsoft.alm.plugin.idea.ui.common.ValidationListener;

import javax.swing.JComponent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class MockBaseDialog implements BaseDialog {
    private List<ActionListener> actionListeners = new ArrayList<ActionListener>();
    private List<ValidationListener> validationListeners = new ArrayList<ValidationListener>();
    private List<String> tabNames = new ArrayList<String>();
    private String displayError;
    private int selectedTabIndex = 0;
    private boolean cloneEnabled = false;

    public MockBaseDialog() {
    }

    public boolean isOkEnabled() {
        return cloneEnabled;
    }

    public String getDisplayError() {
        return displayError;
    }

    public List<ActionListener> getActionListeners() {
        return actionListeners;
    }

    public List<ValidationListener> getValidationListeners() {
        return validationListeners;
    }

    public List<String> getTabNames() {
        return tabNames;
    }

    public void validate() {
        for (ValidationListener vl : validationListeners) {
            ValidationInfo vi = vl.doValidate();
            displayError = vi != null ? vi.message : null;
        }
    }

    @Override
    public void displayError(String message) {
        displayError = message;
    }

    @Override
    public void addTabPage(String text, JComponent component) {
        tabNames.add(text);
    }

    @Override
    public int getSelectedTabIndex() {
        return selectedTabIndex;
    }

    @Override
    public void setSelectedTabIndex(int index) {
        selectedTabIndex = index;
    }

    @Override
    public void setOkEnabled(boolean enabled) {
        cloneEnabled = enabled;
    }

    @Override
    public void addActionListener(ActionListener listener) {
        actionListeners.add(listener);
    }

    @Override
    public void addValidationListener(ValidationListener listener) {
        validationListeners.add(listener);
    }

    @Override
    public boolean showModalDialog() {
        return false;
    }
}
