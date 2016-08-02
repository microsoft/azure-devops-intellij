// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.controls;

import com.intellij.ui.SearchTextField;
import com.intellij.util.ui.JBUI;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;

/**
 * Custom text filter panel
 */
public class SearchFilter extends JPanel {
    private final JLabel filerLabel;
    private final SearchTextField searchField;

    public SearchFilter() {
        filerLabel = new JLabel(TfPluginBundle.message(TfPluginBundle.KEY_TOOLBAR_FILTER_TITLE));
        searchField = new SearchTextField(true);

        setLayout(new BorderLayout(JBUI.scale(3), 0)); //adds vertical padding so search field isn't crammed in the panel
        add(filerLabel, BorderLayout.LINE_START);
        add(searchField, BorderLayout.LINE_END);
    }

    public void addDocumentListener(final DocumentListener listener) {
        searchField.addDocumentListener(listener);
    }

    public String getFilterText() {
        return searchField.getText();
    }

    public void setFilterText(final String filterString) {
        searchField.setText(filterString);
    }
}
