// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.vcsimport;

import com.microsoft.alm.plugin.idea.ui.common.ServerContextTableModel;

import javax.swing.JComponent;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;
import java.awt.event.ActionListener;

/**
 * This interface exists to make testing the Import page controllers possible
 */
public interface ImportPage {
    void addActionListener(final ActionListener listener);

    void setLoginShowing(final boolean showLogin);

    void setLoading(final boolean loading);
    void setAuthenticating(final boolean authenticating);

    void setTeamProjectFilter(final String filter);
    String getTeamProjectFilter();

    void setTeamProjectTable(final ServerContextTableModel tableModel, final ListSelectionModel selectionModel);

    void setRepositoryName(final String name);
    String getRepositoryName();

    void setUserName(final String name);

    void setServerName(final String name);
    String getServerName();

    JComponent getComponent(final String name);
}
