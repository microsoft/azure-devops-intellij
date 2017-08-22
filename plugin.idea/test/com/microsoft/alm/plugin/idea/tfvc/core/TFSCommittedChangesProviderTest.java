// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.RepositoryLocation;
import com.intellij.openapi.vcs.versionBrowser.ChangeBrowserSettings;
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.AsynchConsumer;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.models.ChangeSet;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TFSVcs.class, CommandUtils.class, TFSCommittedChangesProvider.class})
public class TFSCommittedChangesProviderTest extends IdeaAbstractTest {
    private static final String SERVER_URL = "https://account.visualstudio.com";
    private static final String LOCAL_ROOT_PATH = "/Users/user/root";
    private static final String USER_ME = "me";

    @Mock
    private Project mockProject;
    @Mock
    private TFSVcs mockVcs;
    @Mock
    private FilePath mockRoot;
    @Mock
    private VirtualFile mockVirtualFile;
    @Mock
    private Workspace mockWorkspace;
    @Mock
    private ChangeBrowserSettings mockChangeBrowserSettings;
    @Mock
    private AsynchConsumer<CommittedChangeList> mockAsynchConsumer;
    @Mock
    private ChangeSet mockChangeSet1;
    @Mock
    private ChangeSet mockChangeSet2;
    @Mock
    private ChangeSet mockChangeSet3;
    @Mock
    private TFSChangeListBuilder mockTFSChangeListBuilder;

    private TFSCommittedChangesProvider committedChangesProvider;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(TFSVcs.class, CommandUtils.class);

        when(TFSVcs.getInstance(mockProject)).thenReturn(mockVcs);
        when(mockVirtualFile.getPath()).thenReturn(LOCAL_ROOT_PATH);
        when(mockRoot.getVirtualFile()).thenReturn(mockVirtualFile);
        when(mockWorkspace.getServer()).thenReturn(SERVER_URL);
        when(mockChangeBrowserSettings.getUserFilter()).thenReturn(USER_ME);
        when(CommandUtils.getPartialWorkspace(mockProject)).thenReturn(mockWorkspace);
        whenNew(TFSChangeListBuilder.class).withAnyArguments().thenReturn(mockTFSChangeListBuilder);
        when(mockChangeBrowserSettings.getChangeAfterFilter()).thenReturn(30L);
        when(mockChangeBrowserSettings.getChangeBeforeFilter()).thenReturn(50L);

        when(mockChangeSet1.getIdAsInt()).thenReturn(48);
        when(mockChangeSet2.getIdAsInt()).thenReturn(40);
        when(mockChangeSet3.getIdAsInt()).thenReturn(31);
        when(mockChangeSet1.getDate()).thenReturn("2016-08-15T11:50:09.427-0400");
        when(mockChangeSet2.getDate()).thenReturn("2016-07-11T12:00:00.000-0400");
        when(mockChangeSet3.getDate()).thenReturn("2016-06-23T04:30:00.00-0400");

        committedChangesProvider = new TFSCommittedChangesProvider(mockProject);
    }

    @Test
    public void testGetLocationFor() {
        final RepositoryLocation repositoryLocation = committedChangesProvider.getLocationFor(mockRoot);
        assertEquals(SERVER_URL, repositoryLocation.getKey());
        assertEquals(mockWorkspace, ((TFSRepositoryLocation) repositoryLocation).getWorkspace());
        assertEquals(mockVirtualFile, ((TFSRepositoryLocation) repositoryLocation).getRoot());
    }

    @Test
    public void testLoadCommittedChanges_FoundChanges() throws Exception {
        final List<ChangeSet> changeSetList = ImmutableList.of(mockChangeSet1, mockChangeSet2, mockChangeSet3);
        when(CommandUtils.getHistoryCommand(any(ServerContext.class), eq(LOCAL_ROOT_PATH), eq("C30~C50"),
                eq(20), eq(true), eq(USER_ME))).thenReturn(changeSetList);
        final RepositoryLocation repositoryLocation = new TFSRepositoryLocation(mockWorkspace, mockVirtualFile);
        committedChangesProvider.loadCommittedChanges(mockChangeBrowserSettings, repositoryLocation, 20, mockAsynchConsumer);
        verify(mockAsynchConsumer, times(3)).consume(any(TFSChangeList.class));
        verify(mockTFSChangeListBuilder).createChangeList(eq(mockChangeSet1), eq(40), eq("2016-07-11T12:00:00.000-0400"));
        verify(mockTFSChangeListBuilder).createChangeList(eq(mockChangeSet2), eq(31), eq("2016-06-23T04:30:00.00-0400"));
        verify(mockTFSChangeListBuilder).createChangeList(eq(mockChangeSet3), eq(0), eq(StringUtils.EMPTY));
        verify(mockAsynchConsumer).finished();
        verifyNoMoreInteractions(mockAsynchConsumer);
    }

    @Test
    public void testLoadCommittedChanges_NoChanges() throws Exception {
        final List<ChangeSet> changeSetList = Collections.EMPTY_LIST;
        when(CommandUtils.getHistoryCommand(any(ServerContext.class), eq(LOCAL_ROOT_PATH), eq("C30~C50"),
                eq(20), eq(true), eq(USER_ME))).thenReturn(changeSetList);
        final RepositoryLocation repositoryLocation = new TFSRepositoryLocation(mockWorkspace, mockVirtualFile);
        committedChangesProvider.loadCommittedChanges(mockChangeBrowserSettings, repositoryLocation, 20, mockAsynchConsumer);
        verify(mockAsynchConsumer).finished();
        verifyNoMoreInteractions(mockAsynchConsumer);
    }
}
