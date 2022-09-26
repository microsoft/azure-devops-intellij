// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.common.tabs;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.context.RepositoryContext;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.ui.common.VcsTabStatus;
import com.microsoft.alm.plugin.idea.common.utils.VcsHelper;
import com.microsoft.alm.plugin.idea.git.ui.pullrequest.PullRequestsTreeModel;
import com.microsoft.alm.plugin.operations.Operation;
import git4idea.repo.GitRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.URI;
import java.util.Observer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({VcsHelper.class})
public class TabModelImplTest extends IdeaAbstractTest {
    private final String FILTER = "filter";
    private TabModelImpl underTest;

    @Mock
    private PullRequestsTreeModel mockModel;

    @Mock
    private Project mockProject;

    @Mock
    private GitRepository mockGitRepository;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        PowerMockito.mockStatic(VcsHelper.class);

        underTest = Mockito.spy(new TabModelImpl(mockProject, mockModel, null) {
            protected void createDataProvider() {

            }

            public void openGitRepoLink() {

            }

            public void openSelectedItemsLink() {

            }

            public void appendData(final Operation.Results results) {

            }

            public void clearData() {

            }

            public void createNewItem() {

            }
        });
        underTest.setFilter(FILTER);
        reset(underTest);
        reset(mockModel);
    }

    @Test
    public void testObservable() {
        final Observer observerMock = Mockito.mock(Observer.class);
        underTest.addObserver(observerMock);

        underTest.setTabStatus(underTest.getTabStatus());
        verify(observerMock, never()).update(underTest, TabModel.PROP_TAB_STATUS);
        underTest.setTabStatus(VcsTabStatus.LOADING_COMPLETED);
        verify(observerMock, times(1)).update(underTest, TabModel.PROP_TAB_STATUS);
    }

    @Test
    public void testisTeamServicesRepository_True() {
        when(VcsHelper.getRepositoryContext(any(Project.class)))
                .thenReturn(
                        RepositoryContext.createGitContext(
                                "/root/one",
                                "repo1",
                                "branch1",
                                URI.create("http://repoUrl1")));
        Assert.assertTrue(underTest.isTeamServicesRepository());
    }

    @Test
    public void testisTeamServicesRepository_False() {
        when(VcsHelper.getRepositoryContext(any(Project.class))).thenReturn(null);
        Assert.assertFalse(underTest.isTeamServicesRepository());
    }

    @Test
    public void testSetFilter_New() {
        underTest.setFilter("new_filter");
        verify(mockModel, times(1)).setFilter("new_filter");
        Assert.assertEquals("new_filter", underTest.getFilter());
    }

    @Test
    public void testSetFilter_Same() {
        underTest.setFilter(FILTER);
        verifyNoMoreInteractions(mockModel);
        Assert.assertEquals(FILTER, underTest.getFilter());
    }
}
