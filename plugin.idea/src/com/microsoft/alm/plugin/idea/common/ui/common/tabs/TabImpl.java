// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.common.tabs;

import com.microsoft.alm.plugin.idea.common.ui.common.FilteredModel;
import com.microsoft.alm.plugin.idea.common.ui.common.VcsTabStatus;
import com.microsoft.alm.plugin.operations.Operation;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.awt.event.ActionListener;

/**
 * Tab class to setup common functionality such as metrics and getters/setters
 */
public class TabImpl<T extends FilteredModel> implements Tab<T> {
    protected TabForm form;

    public TabImpl(@NotNull final TabForm form, final String eventName) {
        this.form = form;
    }

    public JComponent getPanel() {
        return form.getPanel();
    }

    public void addActionListener(final ActionListener listener) {
        form.addActionListener(listener);
    }

    public void setStatus(final VcsTabStatus status) {
        form.setStatus(status);
    }

    public void setFilter(final String filterString) {
        form.setFilter(filterString);
    }

    public String getFilter() {
        return form.getFilter();
    }

    public void setAutoRefresh(final boolean autoRefresh) {
        form.setAutoRefresh(autoRefresh);
    }

    public boolean getAutoRefresh() {
        return form.getAutoRefresh();
    }

    public void setViewModel(final T modelView) {
        form.setModelForView(modelView);
    }

    public Operation.CredInputsImpl getOperationInputs() {
        return form.getOperationInputs();
    }

    public void refresh(final boolean isTeamServicesRepository) {
        form.refresh(isTeamServicesRepository);
    }
}
