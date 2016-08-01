// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common.tabs;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.ui.components.JBScrollPane;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.common.VcsTabStatus;
import com.microsoft.alm.plugin.idea.ui.controls.Hyperlink;
import com.microsoft.alm.plugin.idea.ui.workitem.WorkItemsTableModel;
import com.microsoft.alm.plugin.operations.Operation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.event.ActionListener;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ActionManager.class})
@PowerMockIgnore("javax.swing.*")
public class TabFormImplTest extends IdeaAbstractTest {
    private final String TAB_TITLE = TfPluginBundle.KEY_VCS_WIT_TITLE;
    private final String CREATE_DIALOG_TITLE = TfPluginBundle.KEY_VCS_WIT_CREATE_WIT;
    private final String REFRESH_TOOLTIP = TfPluginBundle.KEY_VCS_WIT_REFRESH_TOOLTIP;
    private final String TOOLBAR_LOCATION = "toolbarLocation";

    TabFormImpl underTest;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        underTest = Mockito.spy(new TabFormImpl<WorkItemsTableModel>(TAB_TITLE,
                CREATE_DIALOG_TITLE,
                REFRESH_TOOLTIP,
                TOOLBAR_LOCATION) {
            @Override
            protected void createCustomView() {
                scrollPanel = new JBScrollPane();
            }

            @Override
            protected void addCustomTools(final JPanel panel) {
            }

            @Override
            protected List<JBMenuItem> getMenuItems(ActionListener listener) {
                return null;
            }

            @Override
            public void setModelForView(WorkItemsTableModel modelView) {

            }

            @Override
            public Operation.CredInputsImpl getOperationInputs() {
                return null;
            }

            @Override
            public void refresh() {

            }
        });
        underTest.statusLabel = new JLabel();
        underTest.statusLink = new Hyperlink();

        // Mock needed for creating DefaultActionGroup in create group tests
        PowerMockito.mockStatic(ActionManager.class);
        ActionManager actionManager = Mockito.mock(ActionManager.class);
        Mockito.when(ActionManager.getInstance()).thenReturn(actionManager);
    }

    @Test
    public void createActionGroup() {
        DefaultActionGroup group = underTest.createActionsGroup();
        assertEquals(2, group.getChildrenCount());
        assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_VCS_WIT_CREATE_WIT), group.getChildActionsOrStubs()[0].getTemplatePresentation().getText());
        assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_VCS_WIT_REFRESH_TOOLTIP), group.getChildActionsOrStubs()[1].getTemplatePresentation().getText());
    }

    @Test
    public void createFeedbackGroup() {
        DefaultActionGroup group = underTest.createFeedbackGroup();
        assertEquals(1, group.getChildrenCount());
        assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_FEEDBACK_DIALOG_TITLE), group.getChildActionsOrStubs()[0].getTemplatePresentation().getText());
    }

    @Test
    public void testCreateFilterToolbar() {
        underTest.createFilterToolbar();
        assertNotNull(underTest.searchFilter);
        assertNotNull(underTest.timer);
    }

    @Test
    public void testNotConnected() {
        underTest.setStatus(VcsTabStatus.NOT_TF_GIT_REPO);
        assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_ERRORS_NOT_TFS_REPO, TfPluginBundle.message(TAB_TITLE).toLowerCase()), underTest.getStatusText());
        assertEquals(underTest.getStatusLinkText(), TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_DIALOG_TITLE));
    }

    @Test
    public void testNotAuthenticated() {
        underTest.setStatus(VcsTabStatus.NO_AUTH_INFO);
        assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_VCS_NOT_AUTHENTICATED), underTest.getStatusText());
        assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_VCS_SIGN_IN), underTest.getStatusLinkText());
    }

    @Test
    public void testPullRequestsLoading() {
        underTest.setStatus(VcsTabStatus.LOADING_IN_PROGRESS);
        assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_VCS_LOADING), underTest.getStatusText());
        assertEquals("", underTest.getStatusLinkText());
    }

    @Test
    public void testLoadingComplete() {
        underTest.setStatus(VcsTabStatus.LOADING_COMPLETED);
        assertTrue(underTest.getStatusText().startsWith(TfPluginBundle.message(TfPluginBundle.KEY_VCS_LAST_REFRESHED_AT, "")));
        assertEquals(TfPluginBundle.message((TfPluginBundle.KEY_VCS_OPEN_IN_BROWSER)), underTest.getStatusLinkText());
    }

    @Test
    public void testLoadingErrors() {
        underTest.setStatus(VcsTabStatus.LOADING_COMPLETED_ERRORS);
        assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_VCS_LOADING_ERRORS, TfPluginBundle.message(TAB_TITLE).toLowerCase()), underTest.getStatusText());
        assertEquals(TfPluginBundle.message((TfPluginBundle.KEY_VCS_OPEN_IN_BROWSER)), underTest.getStatusLinkText());
    }

    @Test
    public void testCreateMenuItem() {
        JBMenuItem item = underTest.createMenuItem(TAB_TITLE, null, "action", null);
        assertEquals(TfPluginBundle.message(TAB_TITLE), item.getText());
        assertEquals("action", item.getActionCommand());
    }
}
