// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core.tfs.conflicts;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.openapi.vcs.update.FileGroup;
import com.intellij.openapi.vcs.update.UpdatedFiles;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.commands.ResolveConflictsCommand;
import com.microsoft.alm.plugin.external.models.ChangeSet;
import com.microsoft.alm.plugin.external.models.CheckedInChange;
import com.microsoft.alm.plugin.external.models.Conflict;
import com.microsoft.alm.plugin.external.models.MergeResults;
import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.external.models.RenameConflict;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.ModelValidationInfo;
import com.microsoft.alm.plugin.idea.tfvc.core.ClassicTfvcClient;
import com.microsoft.alm.plugin.idea.tfvc.core.TFSVcs;
import com.microsoft.alm.plugin.idea.tfvc.core.TfvcClient;
import com.microsoft.alm.plugin.idea.tfvc.core.revision.TFSContentRevision;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.VersionControlPath;
import com.microsoft.alm.plugin.idea.tfvc.ui.resolve.ConflictsTableModel;
import com.microsoft.alm.plugin.idea.tfvc.ui.resolve.ContentTriplet;
import com.microsoft.alm.plugin.idea.tfvc.ui.resolve.NameMergerResolution;
import com.microsoft.alm.plugin.idea.tfvc.ui.resolve.ResolveConflictsModel;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ResolveConflictHelperTest extends IdeaAbstractTest {
    public final Conflict CONFLICT_RENAME = new RenameConflict("/path/to/fileRename.txt", "$/server/path", "/old/path");
    public final Conflict CONFLICT_CONTEXT = new Conflict("/path/to/fileContent.txt", Conflict.ConflictType.CONTENT);
    public final Conflict CONFLICT_BOTH = new RenameConflict("/path/to/fileBoth.txt", "$/server/path", "/old/path", Conflict.ConflictType.NAME_AND_CONTENT);

    public ResolveConflictHelper helper;
    public final List<String> updateRoots = Arrays.asList("/path");

    @Mock
    public Project mockProject;

    @Mock
    public UpdatedFiles mockUpdatedFiles;

    @Mock
    public FileGroup mockFileGroup;

    @Mock
    public ResolveConflictsModel mockResolveConflictsModel;

    @Mock
    public ProgressManager mockProgressManager;

    @Mock
    public TFSVcs mockTFSVcs;

    @Mock
    public ServerContext mockServerContext;

    @Mock
    public ConflictsTableModel mockConflictsTableModel;

    @Mock
    public FilePath mockFilePath;

    @Mock
    public ProgressIndicator mockProgressIndicator;

    @Mock
    public TFSContentRevision mockTFSContentRevision1;

    @Mock
    public TFSContentRevision mockTFSContentRevision2;

    @Mock
    public CurrentContentRevision mockCurrentContentRevision;

    @Mock
    public File mockFile;

    @Mock
    public NameMerger mockNameMerger;

    @Mock
    public ContentMerger mockContentMerger;

    @Mock
    public ContentTriplet mockContentTriplet;

    @Mock
    private MockedStatic<CommandUtils> commandUtilsStatic;

    @Mock
    private MockedStatic<ConflictsEnvironment> conflictsEnvironmentStatic;

    @Mock
    private MockedStatic<CurrentContentRevision> currentContentRevisionStatic;

    @Mock
    private MockedStatic<ProgressManager> progressManagerStatic;

    @Mock
    private MockedStatic<ServiceManager> serviceManagerStatic;

    @Mock
    private MockedStatic<TFSContentRevision> tfsContentRevisionStatic;

    @Mock
    private MockedStatic<TFSVcs> tfsVcsStatic;

    @Mock
    private MockedStatic<TfsFileUtil> tfsFileUtilStatic;

    @Mock
    private MockedStatic<TfvcClient> tfvcClientStatic;

    @Mock
    private MockedStatic<VcsUtil> vcsUtilStatic;

    @Mock
    private MockedStatic<VersionControlPath> versionControlPathStatic;


    @Before
    public void setUp() throws VcsException {
        when(mockFile.isFile()).thenReturn(true);
        when(mockFile.isDirectory()).thenReturn(false);
        when(mockUpdatedFiles.getGroupById(anyString())).thenReturn(mockFileGroup);
        when(mockTFSVcs.getServerContext(anyBoolean())).thenReturn(mockServerContext);
        when(mockResolveConflictsModel.getConflictsTableModel()).thenReturn(mockConflictsTableModel);
        when(mockProgressManager.getProgressIndicator()).thenReturn(mockProgressIndicator);

        progressManagerStatic.when(ProgressManager::getInstance).thenReturn(mockProgressManager);
        tfvcClientStatic.when(TfvcClient::getInstance).thenReturn(new ClassicTfvcClient());
        tfsVcsStatic.when(() -> TFSVcs.getInstance(mockProject)).thenReturn(mockTFSVcs);
        conflictsEnvironmentStatic.when(ConflictsEnvironment::getNameMerger).thenReturn(mockNameMerger);
        conflictsEnvironmentStatic.when(ConflictsEnvironment::getContentMerger).thenReturn(mockContentMerger);

        helper = new ResolveConflictHelper(mockProject, mockUpdatedFiles, updateRoots);
    }

    @Test
    public void testMerge_Rename_TakeTheirs() throws Exception {
        renameTest(((RenameConflict) CONFLICT_RENAME).getServerPath());
        verify(helper).resolveConflictWithProgress(eq(CONFLICT_RENAME.getLocalPath()), eq(((RenameConflict) CONFLICT_RENAME).getServerPath()), eq(ResolveConflictsCommand.AutoResolveType.TakeTheirs), eq(mockServerContext), eq(mockResolveConflictsModel), eq(true), any(NameMergerResolution.class));
    }

    @Test
    public void testMerge_Rename_KeepYours() throws Exception {
        renameTest(CONFLICT_RENAME.getLocalPath());
        verify(helper).resolveConflictWithProgress(eq(CONFLICT_RENAME.getLocalPath()), eq(ResolveConflictsCommand.AutoResolveType.KeepYours), eq(mockServerContext), eq(mockResolveConflictsModel), eq(true), any(NameMergerResolution.class));
    }

    public void renameTest(final String selectedName) throws Exception {
        helper = spy(new ResolveConflictHelper(mockProject, mockUpdatedFiles, updateRoots));
        FilePath mockRenameFilePath = mock(FilePath.class);
        when(VersionControlPath.getFilePath(CONFLICT_RENAME.getLocalPath(), false)).thenReturn(mockRenameFilePath);
        when(mockNameMerger.mergeName(anyString(), anyString(), eq(mockProject))).thenReturn(selectedName);

        helper.acceptMerge(CONFLICT_RENAME, mockResolveConflictsModel);
        verify(mockNameMerger).mergeName(anyString(), anyString(), eq(mockProject));
        verify(helper, never()).processBothConflicts(any(Conflict.class), any(ServerContext.class), any(ResolveConflictsModel.class), any(File.class), any(ContentTriplet.class), any(NameMergerResolution.class));
        verify(helper, never()).processContentConflict(any(ServerContext.class), any(ResolveConflictsModel.class), any(ContentTriplet.class), any(FilePath.class), any(NameMergerResolution.class));
    }

    @Test
    public void testMerge_Content() throws Exception {
        helper = spy(new ResolveConflictHelper(mockProject, mockUpdatedFiles, updateRoots));
        FilePath mockLocalPath = mock(FilePath.class);
        VirtualFile mockVirtualFile = mock(VirtualFile.class);
        when(mockLocalPath.getPath()).thenReturn(CONFLICT_CONTEXT.getLocalPath());
        when(VersionControlPath.getFilePath(CONFLICT_CONTEXT.getLocalPath(), false)).thenReturn(mockLocalPath);
        when(VcsUtil.getVirtualFileWithRefresh(any(File.class))).thenReturn(mockVirtualFile);
        when(mockContentMerger.mergeContent(any(ContentTriplet.class), eq(mockProject), eq(mockVirtualFile), isNull())).thenReturn(true);

        helper.acceptMerge(CONFLICT_CONTEXT, mockResolveConflictsModel);
        verify(helper).populateThreeWayDiffWithProgress(CONFLICT_CONTEXT, new File(CONFLICT_CONTEXT.getLocalPath()), mockLocalPath, mockServerContext);
        verify(mockContentMerger).mergeContent(any(ContentTriplet.class), eq(mockProject), eq(mockVirtualFile), isNull());
        verify(helper).resolveConflictWithProgress(eq(CONFLICT_CONTEXT.getLocalPath()), eq(ResolveConflictsCommand.AutoResolveType.KeepYours), eq(mockServerContext), eq(mockResolveConflictsModel), eq(true), any());
        verify(helper, never()).processBothConflicts(any(Conflict.class), any(ServerContext.class), any(ResolveConflictsModel.class), any(File.class), any(ContentTriplet.class), any(NameMergerResolution.class));
        verify(helper, never()).processRenameConflict(any(Conflict.class), any(ServerContext.class), any(ResolveConflictsModel.class), any(NameMergerResolution.class));
    }

    @Test(expected = VcsException.class)
    public void testMerge_Content_VirtualFileException() throws Exception {
        FilePath mockLocalPath = mock(FilePath.class);
        when(mockLocalPath.getPath()).thenReturn(CONFLICT_CONTEXT.getLocalPath());
        when(VersionControlPath.getFilePath(CONFLICT_CONTEXT.getLocalPath(), false)).thenReturn(mockLocalPath);
        when(VcsUtil.getVirtualFileWithRefresh(any(File.class))).thenReturn(null);

        helper.acceptMerge(CONFLICT_CONTEXT, mockResolveConflictsModel);
    }

    @Test
    public void testMerge_Both_TakeTheirs() throws Exception {
        bothTest(((RenameConflict) CONFLICT_BOTH).getServerPath());
        verify(helper).resolveConflictWithProgress(eq(CONFLICT_BOTH.getLocalPath()), eq(ResolveConflictsCommand.AutoResolveType.TakeTheirs), eq(mockServerContext), eq(mockResolveConflictsModel), eq(false), any(NameMergerResolution.class));
    }

    @Test
    public void testMerge_Both_KeepYours() throws Exception {
        bothTest(CONFLICT_BOTH.getLocalPath());
    }

    public void bothTest(final String selectedName) throws Exception {
        helper = spy(new ResolveConflictHelper(mockProject, mockUpdatedFiles, updateRoots));

        FilePath mockLocalPath = mock(FilePath.class);
        when(mockLocalPath.getPath()).thenReturn(CONFLICT_BOTH.getLocalPath());

        FilePath mockServerPath = mock(FilePath.class);
        when(mockServerPath.getPath()).thenReturn(((RenameConflict) CONFLICT_BOTH).getServerPath());

        VirtualFile mockVirtualFile = mock(VirtualFile.class);
        when(VcsUtil.getVirtualFileWithRefresh(any(File.class))).thenReturn(mockVirtualFile);

        when(mockNameMerger.mergeName(anyString(), anyString(), eq(mockProject))).thenReturn(selectedName);

        when(VersionControlPath.getFilePath(eq(((RenameConflict) CONFLICT_BOTH).getServerPath()), anyBoolean())).thenReturn(mockServerPath);

        when(VersionControlPath.getFilePath(CONFLICT_BOTH.getLocalPath(), false)).thenReturn(mockLocalPath);
        when(mockContentMerger.mergeContent(any(ContentTriplet.class), eq(mockProject), eq(mockVirtualFile), isNull())).thenReturn(true);

        helper.acceptMerge(CONFLICT_BOTH, mockResolveConflictsModel);
        verify(helper).populateThreeWayDiffWithProgress(CONFLICT_BOTH, new File(CONFLICT_BOTH.getLocalPath()), mockLocalPath, mockServerContext);
        verify(mockNameMerger).mergeName(anyString(), anyString(), eq(mockProject));
        verify(mockContentMerger).mergeContent(any(ContentTriplet.class), eq(mockProject), eq(mockVirtualFile), isNull());
        verify(helper).resolveConflictWithProgress(eq(selectedName), eq(ResolveConflictsCommand.AutoResolveType.KeepYours), eq(mockServerContext), eq(mockResolveConflictsModel), eq(true), any(NameMergerResolution.class));
    }

    @Test
    public void testResolveConflict_Happy() throws Exception {
        when(CommandUtils.resolveConflictsByPath(mockServerContext, Arrays.asList(CONFLICT_BOTH.getLocalPath()), ResolveConflictsCommand.AutoResolveType.KeepYours)).thenReturn(Arrays.asList(CONFLICT_BOTH));
        helper.resolveConflict(CONFLICT_BOTH.getLocalPath(), ((RenameConflict) CONFLICT_BOTH).getServerPath(), ResolveConflictsCommand.AutoResolveType.KeepYours, mockServerContext, mockResolveConflictsModel, true, null);

        verify(mockUpdatedFiles).getGroupById(FileGroup.MERGED_ID);
        verify(mockFileGroup).add(((RenameConflict) CONFLICT_BOTH).getServerPath(), TFSVcs.getKey(), null);
    }

    @Test(expected = VcsException.class)
    public void testResolveConflict_Exception() throws Exception {
        when(CommandUtils.resolveConflictsByPath(mockServerContext, Arrays.asList(CONFLICT_BOTH.getLocalPath()), ResolveConflictsCommand.AutoResolveType.KeepYours)).thenThrow(new RuntimeException("Test Error"));
        helper.resolveConflict(CONFLICT_BOTH.getLocalPath(), CONFLICT_BOTH.getLocalPath(), ResolveConflictsCommand.AutoResolveType.KeepYours, mockServerContext, mockResolveConflictsModel, true, null);
    }

    private void mockGetStatusForFiles(PendingChange pendingChangeOriginal, Conflict conflict) {
        when(
                CommandUtils.getStatusForFiles(
                        eq(mockProject),
                        eq(mockServerContext),
                        eq(Collections.singletonList(conflict.getLocalPath()))))
                .thenReturn(Collections.singletonList(pendingChangeOriginal));
    }

    @Test
    public void testPopulateThreeWayDiff_ContentsChangeOnly() throws Exception {
        PendingChange pendingChangeOriginal = new PendingChange("$/server/path/file.txt", "/path/to/file.txt", "10", "domain/user", "2016-09-14T16:10:08.487-0400", "none", "edit", "workspace1", "computer1", false, StringUtils.EMPTY);
        CheckedInChange checkedInChange = new CheckedInChange("$/server/path/file.txt", "edit", "9", "2016-09-10T16:10:08.487-0400");
        ChangeSet changeSet = new ChangeSet("9", "domain/user", "domain/user", "2016-09-14T16:10:08.487-0400", "comment", Arrays.asList(checkedInChange));

        mockGetStatusForFiles(pendingChangeOriginal, CONFLICT_CONTEXT);
        when(CommandUtils.getLastHistoryEntryForAnyUser(mockServerContext, CONFLICT_CONTEXT.getLocalPath())).thenReturn(changeSet);
        when(TFSContentRevision.create(mockProject, mockFilePath, Integer.parseInt(pendingChangeOriginal.getVersion()), pendingChangeOriginal.getDate())).thenReturn(mockTFSContentRevision1);
        when(TFSContentRevision.create(mockProject, mockFilePath, changeSet.getIdAsInt(), changeSet.getDate())).thenReturn(mockTFSContentRevision2);
        when(CurrentContentRevision.create(mockFilePath)).thenReturn(mockCurrentContentRevision);
        when(mockTFSContentRevision1.getContent()).thenReturn("Content of the original file");
        when(mockTFSContentRevision2.getContent()).thenReturn("Content of the server file");
        when(mockCurrentContentRevision.getContent()).thenReturn("Content of the local file");
        ContentTriplet contentTriplet = new ContentTriplet();

        helper.populateThreeWayDiff(CONFLICT_CONTEXT, mockFile, mockFilePath, mockServerContext, contentTriplet);
        assertEquals("Content of the original file", contentTriplet.baseContent);
        assertEquals("Content of the local file", contentTriplet.localContent);
        assertEquals("Content of the server file", contentTriplet.serverContent);
    }

    @Test
    public void testPopulateThreeWayDiff_ContentBothChange() throws Exception {
        FilePath mockRenameFilePath = mock(FilePath.class);
        PendingChange pendingChangeOriginal = new PendingChange("$/server/path/file.txt", "/path/to/file.txt", "10", "domain/user", "2016-09-14T16:10:08.487-0400", "none", "edit", "workspace1", "computer1", false, StringUtils.EMPTY);
        CheckedInChange checkedInChange = new CheckedInChange("$/server/path/file.txt", "edit", "9", "2016-09-10T16:10:08.487-0400");
        ChangeSet changeSet = new ChangeSet("9", "domain/user", "domain/user", "2016-09-14T16:10:08.487-0400", "comment", Arrays.asList(checkedInChange));

        when(VersionControlPath.getFilePath(CONFLICT_BOTH.getLocalPath(), false)).thenReturn(mockRenameFilePath);
        when(CommandUtils.getLastHistoryEntryForAnyUser(mockServerContext, ((RenameConflict) CONFLICT_BOTH).getServerPath())).thenReturn(changeSet);
        mockGetStatusForFiles(pendingChangeOriginal, CONFLICT_BOTH);
        when(TFSContentRevision.createRenameRevision(mockProject, mockRenameFilePath, Integer.parseInt(pendingChangeOriginal.getVersion()), pendingChangeOriginal.getDate(), ((RenameConflict) CONFLICT_BOTH).getOldPath())).thenReturn(mockTFSContentRevision1);
        when(TFSContentRevision.createRenameRevision(mockProject, mockRenameFilePath, Integer.parseInt(changeSet.getId()), changeSet.getDate(), ((RenameConflict) CONFLICT_BOTH).getServerPath())).thenReturn(mockTFSContentRevision2);
        when(CurrentContentRevision.create(mockFilePath)).thenReturn(mockCurrentContentRevision);
        when(mockTFSContentRevision1.getContent()).thenReturn("Content of the original file");
        when(mockTFSContentRevision2.getContent()).thenReturn("Content of the server file");
        when(mockCurrentContentRevision.getContent()).thenReturn("Content of the local file");
        ContentTriplet contentTriplet = new ContentTriplet();

        helper.populateThreeWayDiff(CONFLICT_BOTH, mockFile, mockFilePath, mockServerContext, contentTriplet);
        assertEquals("Content of the original file", contentTriplet.baseContent);
        assertEquals("Content of the local file", contentTriplet.localContent);
        assertEquals("Content of the server file", contentTriplet.serverContent);
    }

    @Test(expected = VcsException.class)
    public void testPopulateThreeWayDiff_Exception() throws Exception {
        when(CommandUtils.getStatusForFiles(eq(mockProject), eq(mockServerContext), anyList()))
                .thenThrow(new RuntimeException("Test Error"));
        helper.populateThreeWayDiff(CONFLICT_BOTH, mockFile, mockFilePath, mockServerContext, mockContentTriplet);
    }

    @Test
    public void testAcceptTheirs() {
        helper.acceptChanges("/path/to/file_with_conflict", ResolveConflictsCommand.AutoResolveType.TakeTheirs);

        verify(mockUpdatedFiles).getGroupById(FileGroup.UPDATED_ID);
        verify(mockFileGroup).add("/path/to/file_with_conflict", TFSVcs.getKey(), null);
    }

    @Test
    public void testAcceptYours() {
        helper.acceptChanges("/path/to/file_with_conflict", ResolveConflictsCommand.AutoResolveType.KeepYours);

        verify(mockUpdatedFiles).getGroupById(FileGroup.SKIPPED_ID);
        verify(mockFileGroup).add("/path/to/file_with_conflict", TFSVcs.getKey(), null);
    }

    @Test
    public void testSkip() {
        helper.skip(Arrays.asList(new Conflict("/path/to/file1", Conflict.ConflictType.CONTENT), new Conflict("/path/to/file2", Conflict.ConflictType.RENAME)));

        verify(mockUpdatedFiles, times(2)).getGroupById(FileGroup.SKIPPED_ID);
        verify(mockFileGroup).add("/path/to/file1", TFSVcs.getKey(), null);
        verify(mockFileGroup).add("/path/to/file2", TFSVcs.getKey(), null);
        verifyNoMoreInteractions(mockFileGroup, mockUpdatedFiles);
    }

    @Test
    public void testIsNameConflict_TrueRename() {
        assertTrue(helper.isNameConflict(new Conflict("/path/to/file1", Conflict.ConflictType.RENAME)));
    }

    @Test
    public void testIsNameConflict_TrueBoth() {
        assertTrue(helper.isNameConflict(new Conflict("/path/to/file1", Conflict.ConflictType.NAME_AND_CONTENT)));
    }

    @Test
    public void testIsNameConflict_FalseContent() {
        assertFalse(helper.isNameConflict(new Conflict("/path/to/file1", Conflict.ConflictType.CONTENT)));
    }

    @Test
    public void testIsContentConflict_TrueContent() {
        assertTrue(helper.isContentConflict(new Conflict("/path/to/file1", Conflict.ConflictType.CONTENT)));
    }

    @Test
    public void testIsContentConflict_TrueBoth() {
        assertTrue(helper.isContentConflict(new Conflict("/path/to/file1", Conflict.ConflictType.NAME_AND_CONTENT)));
    }

    @Test
    public void testIsContentConflict_FalseRename() {
        assertFalse(helper.isContentConflict(new Conflict("/path/to/file1", Conflict.ConflictType.RENAME)));
    }

    @Test
    public void testAcceptChange_GetConflictsException() {
        when(CommandUtils.getConflicts(any(), anyString(), any())).thenThrow(new RuntimeException("Test Error"));
        when(CommandUtils.resolveConflictsByConflict(any(), eq(Arrays.asList(CONFLICT_RENAME)), eq(ResolveConflictsCommand.AutoResolveType.TakeTheirs))).thenReturn(Arrays.asList(CONFLICT_RENAME));
        helper.acceptChange(Arrays.asList(CONFLICT_RENAME), mock(ProgressIndicator.class), mockProject, ResolveConflictsCommand.AutoResolveType.TakeTheirs, mockResolveConflictsModel);

        ArgumentCaptor<ModelValidationInfo> args = ArgumentCaptor.forClass(ModelValidationInfo.class);
        verify(mockResolveConflictsModel).addError(args.capture());
        assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_LOAD_ERROR), args.getValue().getValidationMessage());
    }

    @Test
    public void testAcceptChange_ResolveConflictException() {
        when(CommandUtils.getConflicts(any(ServerContext.class), anyString(), any(MergeResults.class))).thenReturn(Arrays.asList(CONFLICT_RENAME));
        when(CommandUtils.resolveConflictsByConflict(any(ServerContext.class), eq(Arrays.asList(CONFLICT_RENAME)), eq(ResolveConflictsCommand.AutoResolveType.TakeTheirs))).thenThrow(new RuntimeException("Test Error"));
        helper.acceptChange(Arrays.asList(CONFLICT_RENAME), mock(ProgressIndicator.class), mockProject, ResolveConflictsCommand.AutoResolveType.TakeTheirs, mockResolveConflictsModel);

        ArgumentCaptor<ModelValidationInfo> args = ArgumentCaptor.forClass(ModelValidationInfo.class);
        verify(mockResolveConflictsModel).addError(args.capture());
        assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_MERGE_ERROR, CONFLICT_RENAME.getLocalPath(), "Test Error"), args.getValue().getValidationMessage());
    }

    @Test
    public void testAcceptChange_Happy() {
        when(CommandUtils.getConflicts(any(ServerContext.class), anyString(), any(MergeResults.class))).thenReturn(Arrays.asList(CONFLICT_RENAME, CONFLICT_CONTEXT));
        when(CommandUtils.resolveConflictsByConflict(any(ServerContext.class), eq(Arrays.asList(CONFLICT_CONTEXT)), eq(ResolveConflictsCommand.AutoResolveType.TakeTheirs))).thenReturn(Arrays.asList(CONFLICT_CONTEXT));
        when(CommandUtils.resolveConflictsByConflict(any(ServerContext.class), eq(Arrays.asList(CONFLICT_RENAME)), eq(ResolveConflictsCommand.AutoResolveType.TakeTheirs))).thenReturn(Arrays.asList(CONFLICT_RENAME));
        helper.acceptChange(Arrays.asList(CONFLICT_RENAME, CONFLICT_CONTEXT), mock(ProgressIndicator.class), mockProject, ResolveConflictsCommand.AutoResolveType.TakeTheirs, mockResolveConflictsModel);

        verify(mockResolveConflictsModel, never()).addError(any(ModelValidationInfo.class));
        verify(mockUpdatedFiles, times(2)).getGroupById(FileGroup.UPDATED_ID);
        verify(mockFileGroup).add(((RenameConflict) CONFLICT_RENAME).getServerPath(), TFSVcs.getKey(), null);
        verify(mockFileGroup).add(CONFLICT_CONTEXT.getLocalPath(), TFSVcs.getKey(), null);
    }

    @Test
    public void testAcceptChange_Skipped() {
        when(CommandUtils.getConflicts(any(ServerContext.class), anyString(), any(MergeResults.class))).thenReturn(Arrays.asList(CONFLICT_RENAME));
        // return empty lit since nothing was resolved and instead skipped
        when(CommandUtils.resolveConflictsByConflict(any(ServerContext.class), eq(Arrays.asList(CONFLICT_RENAME)), eq(ResolveConflictsCommand.AutoResolveType.TakeTheirs))).thenReturn(Collections.EMPTY_LIST);
        helper.acceptChange(Arrays.asList(CONFLICT_RENAME), mock(ProgressIndicator.class), mockProject, ResolveConflictsCommand.AutoResolveType.TakeTheirs, mockResolveConflictsModel);

        verify(mockResolveConflictsModel, never()).addError(any(ModelValidationInfo.class));
        verify(mockUpdatedFiles).getGroupById(FileGroup.SKIPPED_ID);
        verify(mockFileGroup).add(CONFLICT_RENAME.getLocalPath(), TFSVcs.getKey(), null);
    }
}