// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core.tfs.operations;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFS;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.refactoring.rename.RenameUtil;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.IncorrectOperationException;
import com.microsoft.alm.helpers.Path;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.external.models.ServerStatusType;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.tfvc.core.ClassicTfvcClient;
import com.microsoft.alm.plugin.idea.tfvc.core.TFSVcs;
import com.microsoft.alm.plugin.idea.tfvc.core.TfvcClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(MockitoJUnitRunner.class)
public class RenameFileDirectoryTest extends IdeaAbstractTest {
    private final UsageInfo[] usageInfos = new UsageInfo[1];
    private final String NEW_FILE_NAME = "newName.txt";
    private final String NEW_DIRECTORY_NAME = "newDirectory";
    private final String PARENT_PATH = "/path/to/the";
    private final String CURRENT_FILE_PATH = Path.combine(PARENT_PATH, "file.txt");
    private final String NEW_FILE_PATH = Path.combine(PARENT_PATH, NEW_FILE_NAME);

    @Mock
    private PsiFile mockPsiFile;

    @Mock
    private PsiDirectory mockPsiDirectory;

    @Mock
    private VirtualFile mockVirtualFile;

    @Mock
    private VirtualFile mockVirtualParent;

    @Mock
    private RefactoringElementListener mockListener;

    @Mock
    private TFSVcs mockTFSVcs;

    @Mock
    private Project mockProject;

    @Mock
    private ServerContext mockServerContext;

    @Mock
    private PersistentFS mockPersistentFS;

    @Mock
    private PendingChange mockPendingChange;

    @Mock
    private MockedStatic<CommandUtils> commandUtilsStatic;

    @Mock
    private MockedStatic<PersistentFS> persistentFSStatic;

    @Mock
    private MockedStatic<RenameUtil> renameUtilStatic;

    @Mock
    private MockedStatic<ServiceManager> serviceManagerStatic;

    @Mock
    private MockedStatic<TFSVcs> tfsVcsStatic;

    @Mock
    private MockedStatic<TfvcClient> tfvcClientStatic;

    @Before
    public void setUp() {
        when(mockPsiFile.getVirtualFile()).thenReturn(mockVirtualFile);
        when(mockPsiFile.getProject()).thenReturn(mockProject);
        when(mockVirtualParent.getPath()).thenReturn(PARENT_PATH);
        when(mockVirtualFile.getParent()).thenReturn(mockVirtualParent);

        when(mockTFSVcs.getServerContext(anyBoolean())).thenReturn(mockServerContext);
        tfvcClientStatic.when(TfvcClient::getInstance).thenReturn(new ClassicTfvcClient());
        tfsVcsStatic.when(() -> TFSVcs.getInstance(mockProject)).thenReturn(mockTFSVcs);
        //noinspection ResultOfMethodCallIgnored
        persistentFSStatic.when(PersistentFS::getInstance).thenReturn(mockPersistentFS);
    }

    @Test(expected = IncorrectOperationException.class)
    public void testExecute_BadElement() {
        NavigatablePsiElement mockElement = mock(NavigatablePsiElement.class);
        RenameFileDirectory.execute(mockElement, NEW_FILE_NAME, usageInfos, mockListener);
    }

    @Test
    public void testExecute_RenameFileNoChanges() {
        when(mockVirtualFile.getPath()).thenReturn(CURRENT_FILE_PATH);
        when(CommandUtils.getStatusForFiles(any(Project.class), eq(mockServerContext), eq(ImmutableList.of(CURRENT_FILE_PATH))))
                .thenReturn(Collections.EMPTY_LIST);

        RenameFileDirectory.execute(mockPsiFile, NEW_FILE_NAME, usageInfos, mockListener);

        verifyStatic(CommandUtils.class, times(1));
        CommandUtils.renameFile(eq(mockServerContext), eq(CURRENT_FILE_PATH), eq(NEW_FILE_PATH));
        verify(mockPersistentFS).processEvents(anyList());
        verify(mockListener).elementRenamed(mockPsiFile);

        verifyStatic(RenameUtil.class, never());
        RenameUtil.doRenameGenericNamedElement(any(PsiElement.class), any(String.class), any(UsageInfo[].class), any(RefactoringElementListener.class));
    }

    @Test
    public void testExecute_RenameDirectoryNoChanges() {
        String dirName = Path.combine("/path/to/the", "directory");
        when(mockVirtualFile.getPath()).thenReturn(dirName);
        when(CommandUtils.getStatusForFiles(any(Project.class), eq(mockServerContext), eq(ImmutableList.of(dirName))))
                .thenReturn(Collections.EMPTY_LIST);

        RenameFileDirectory.execute(mockPsiFile, NEW_DIRECTORY_NAME, usageInfos, mockListener);

        verifyStatic(CommandUtils.class, times(1));
        CommandUtils.renameFile(eq(mockServerContext), eq(dirName), eq(Path.combine("/path/to/the", "newDirectory")));
        verify(mockPersistentFS).processEvents(anyList());
        verify(mockListener).elementRenamed(mockPsiFile);

        verifyStatic(RenameUtil.class, never());
        RenameUtil.doRenameGenericNamedElement(any(PsiElement.class), any(String.class), any(UsageInfo[].class), any(RefactoringElementListener.class));
    }

    @Test
    public void testExecute_RenameFileEditChanges() {
        when(mockPendingChange.getChangeTypes()).thenReturn(ImmutableList.of(ServerStatusType.EDIT));
        when(mockVirtualFile.getPath()).thenReturn(CURRENT_FILE_PATH);
        when(CommandUtils.getStatusForFiles(any(Project.class), eq(mockServerContext), eq(ImmutableList.of(CURRENT_FILE_PATH))))
                .thenReturn(ImmutableList.of(mockPendingChange));

        RenameFileDirectory.execute(mockPsiFile, NEW_FILE_NAME, usageInfos, mockListener);

        verifyStatic(CommandUtils.class, times(1));
        CommandUtils.renameFile(eq(mockServerContext), eq(CURRENT_FILE_PATH), eq(NEW_FILE_PATH));
        verify(mockPersistentFS).processEvents(anyList());
        verify(mockListener).elementRenamed(mockPsiFile);

        verifyStatic(RenameUtil.class, never());
        RenameUtil.doRenameGenericNamedElement(any(PsiElement.class), any(String.class), any(UsageInfo[].class), any(RefactoringElementListener.class));
    }

    @Test
    public void testExecute_RenameFileEditRenameChanges() {
        when(mockPendingChange.getChangeTypes()).thenReturn(ImmutableList.of(ServerStatusType.EDIT, ServerStatusType.RENAME));
        when(mockVirtualFile.getPath()).thenReturn(CURRENT_FILE_PATH);
        when(CommandUtils.getStatusForFiles(any(Project.class), eq(mockServerContext), eq(ImmutableList.of(CURRENT_FILE_PATH))))
                .thenReturn(ImmutableList.of(mockPendingChange));

        RenameFileDirectory.execute(mockPsiFile, NEW_FILE_NAME, usageInfos, mockListener);

        commandUtilsStatic.verify(
                () -> CommandUtils.renameFile(eq(mockServerContext), eq(CURRENT_FILE_PATH), eq(NEW_FILE_PATH)),
                times(1));
        verify(mockPersistentFS, times(1)).processEvents(any(List.class));
        verify(mockListener).elementRenamed(mockPsiFile);

        renameUtilStatic.verify(() -> RenameUtil.doRenameGenericNamedElement(any(), any(), any(), any()), never());
    }

    @Test
    public void testExecute_RenameFileUnversionedChange() {
        when(mockPendingChange.getChangeTypes()).thenReturn(ImmutableList.of(ServerStatusType.ADD));
        when(mockVirtualFile.getPath()).thenReturn(CURRENT_FILE_PATH);
        when(CommandUtils.getStatusForFiles(any(Project.class), eq(mockServerContext), eq(ImmutableList.of(CURRENT_FILE_PATH))))
                .thenReturn(ImmutableList.of(mockPendingChange));

        RenameFileDirectory.execute(mockPsiFile, NEW_FILE_NAME, usageInfos, mockListener);

        verifyStatic(RenameUtil.class, times(1));
        RenameUtil.doRenameGenericNamedElement(mockPsiFile, NEW_FILE_NAME, usageInfos, mockListener);
        verify(mockListener).elementRenamed(mockPsiFile);

        verifyStatic(CommandUtils.class, never());
        CommandUtils.renameFile(any(ServerContext.class), any(String.class), any(String.class));
        verify(mockPersistentFS, never()).processEvents(any());
    }
}
