// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.LocalFilePath;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.vcsUtil.VcsUtil;
import com.microsoft.alm.plugin.external.models.ChangeSet;
import com.microsoft.alm.plugin.external.models.CheckedInChange;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.VersionControlPath;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TfsFileUtil.class, VcsUtil.class, VersionControlPath.class})
public class TFSChangeListBuilderTest extends IdeaAbstractTest {
    private static final int CHANGESET_ID = 123;
    private static final String OWNER = "John Smith";
    private static final String CHANGESET_DATE = "2016-08-15T11:50:09.427-0400";
    private static final int PREVIOUS_CHANGESET_ID = 122;
    private static final String PREVIOUS_CHANGESET_DATE = "2016-06-07T11:18:18.790-0400";
    private static final String COMMENT = "Made changes to the utils files";

    // paths of changes
    private FilePath file1 = new LocalFilePath("/Users/user/workspaceName/file1", true);
    private FilePath file2 = new LocalFilePath("/Users/user/workspaceName/file2", false);
    private FilePath file3 = new LocalFilePath("/Users/user/workspaceName/file3", false);
    private FilePath file4 = new LocalFilePath("/Users/user/workspaceName/file4", false);
    private FilePath file5 = new LocalFilePath("/Users/user/workspaceName/file5", false);
    private FilePath file6 = new LocalFilePath("/Users/user/workspaceName/file6", false);
    private FilePath file7 = new LocalFilePath("/Users/user/workspaceName/file7", false);
    private FilePath file8 = new LocalFilePath("/Users/user/workspaceName/file8", false);
    private FilePath file9 = new LocalFilePath("/Users/user/workspaceName/file1", false);

    private CheckedInChange checkedInChange1 = new CheckedInChange("$/path/to/newFile1", "add",
            String.valueOf(CHANGESET_ID), CHANGESET_DATE);
    private CheckedInChange checkedInChange2 = new CheckedInChange("$/path/to/undeltedFile", "undelete",
            String.valueOf(CHANGESET_ID), CHANGESET_DATE);
    private CheckedInChange checkedInChange3 = new CheckedInChange("$/path/to/branchDir", "branch",
            String.valueOf(CHANGESET_ID), CHANGESET_DATE);
    private CheckedInChange checkedInChange4 = new CheckedInChange("$/path/to/renameFileBefore", "delete, source rename",
            String.valueOf(CHANGESET_ID), CHANGESET_DATE);
    private CheckedInChange checkedInChange5 = new CheckedInChange("$/path/to/deletedFile", "delete",
            String.valueOf(CHANGESET_ID), CHANGESET_DATE);
    private CheckedInChange checkedInChange6 = new CheckedInChange("$/path/to/renameFileAfter", "rename",
            String.valueOf(CHANGESET_ID), CHANGESET_DATE);
    private CheckedInChange checkedInChange7 = new CheckedInChange("$/path/to/editedFile1", "edit",
            String.valueOf(CHANGESET_ID), CHANGESET_DATE);
    private CheckedInChange checkedInChange8 = new CheckedInChange("$/path/to/newFile2", "add",
            String.valueOf(CHANGESET_ID), CHANGESET_DATE);
    private CheckedInChange checkedInChange9 = new CheckedInChange("$/path/to/newFile1", "edit",
            String.valueOf(CHANGESET_ID), CHANGESET_DATE);

    private List<CheckedInChange> checkedInChanges = ImmutableList.of(checkedInChange1, checkedInChange2, checkedInChange3,
            checkedInChange4, checkedInChange5, checkedInChange6, checkedInChange7, checkedInChange8, checkedInChange9);

    @Mock
    private TFSVcs mockVcs;
    @Mock
    private Workspace mockWorkspace;

    private TFSChangeListBuilder changeListBuilder;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(TfsFileUtil.class, VcsUtil.class, VersionControlPath.class);
        when(TfsFileUtil.translateServerItemToLocalItem(any(List.class), eq(checkedInChange1.getServerItem()))).thenReturn(file1.getPath());
        when(TfsFileUtil.translateServerItemToLocalItem(any(List.class), eq(checkedInChange2.getServerItem()))).thenReturn(file2.getPath());
        when(TfsFileUtil.translateServerItemToLocalItem(any(List.class), eq(checkedInChange3.getServerItem()))).thenReturn(file3.getPath());
        when(TfsFileUtil.translateServerItemToLocalItem(any(List.class), eq(checkedInChange4.getServerItem()))).thenReturn(file4.getPath());
        when(TfsFileUtil.translateServerItemToLocalItem(any(List.class), eq(checkedInChange5.getServerItem()))).thenReturn(file5.getPath());
        when(TfsFileUtil.translateServerItemToLocalItem(any(List.class), eq(checkedInChange6.getServerItem()))).thenReturn(file6.getPath());
        when(TfsFileUtil.translateServerItemToLocalItem(any(List.class), eq(checkedInChange7.getServerItem()))).thenReturn(file7.getPath());
        when(TfsFileUtil.translateServerItemToLocalItem(any(List.class), eq(checkedInChange8.getServerItem()))).thenReturn(file8.getPath());
        when(TfsFileUtil.translateServerItemToLocalItem(any(List.class), eq(checkedInChange9.getServerItem()))).thenReturn(file9.getPath());

        when(VcsUtil.getFilePath(file1.getPath(), file1.isDirectory())).thenReturn(file1);
        when(VcsUtil.getFilePath(file2.getPath(), file2.isDirectory())).thenReturn(file2);
        when(VcsUtil.getFilePath(file3.getPath(), file3.isDirectory())).thenReturn(file3);
        when(VcsUtil.getFilePath(file4.getPath(), file4.isDirectory())).thenReturn(file4);
        when(VcsUtil.getFilePath(file5.getPath(), file5.isDirectory())).thenReturn(file5);
        when(VcsUtil.getFilePath(file6.getPath(), file6.isDirectory())).thenReturn(file6);
        when(VcsUtil.getFilePath(file7.getPath(), file7.isDirectory())).thenReturn(file7);
        when(VcsUtil.getFilePath(file8.getPath(), file8.isDirectory())).thenReturn(file8);
        when(VcsUtil.getFilePath(file9.getPath(), file9.isDirectory())).thenReturn(file9);

        when(VersionControlPath.getFilePath(file1.getPath(), file1.isDirectory())).thenReturn(file1);
        when(VersionControlPath.getFilePath(file2.getPath(), file2.isDirectory())).thenReturn(file2);
        when(VersionControlPath.getFilePath(file3.getPath(), file3.isDirectory())).thenReturn(file3);
        when(VersionControlPath.getFilePath(file4.getPath(), file4.isDirectory())).thenReturn(file4);
        when(VersionControlPath.getFilePath(file5.getPath(), file5.isDirectory())).thenReturn(file5);
        when(VersionControlPath.getFilePath(file6.getPath(), file6.isDirectory())).thenReturn(file6);
        when(VersionControlPath.getFilePath(file7.getPath(), file7.isDirectory())).thenReturn(file7);
        when(VersionControlPath.getFilePath(file8.getPath(), file8.isDirectory())).thenReturn(file8);
        when(VersionControlPath.getFilePath(file9.getPath(), file9.isDirectory())).thenReturn(file9);

        changeListBuilder = new TFSChangeListBuilder(mockVcs, mockWorkspace);
    }

    @Test
    public void testCreateChangeList() {
        final ChangeSet changeSet = new ChangeSet(String.valueOf(CHANGESET_ID), OWNER, OWNER, CHANGESET_DATE, COMMENT, checkedInChanges);
        final TFSChangeList changeList = changeListBuilder.createChangeList(changeSet, PREVIOUS_CHANGESET_ID, PREVIOUS_CHANGESET_DATE);

        assertEquals(CHANGESET_ID, changeList.getNumber());
        assertEquals(OWNER, changeList.getCommitterName());
        assertEquals(COMMENT, changeList.getComment());

        final List<Change> changes = new ArrayList<Change>(changeList.getChanges());
        assertEquals(9, changes.size());

        assertEquals(Change.Type.NEW, changes.get(0).getType());
        assertNull(changes.get(0).getBeforeRevision());
        assertEquals(file1.getPath(), changes.get(0).getAfterRevision().getFile().getPath());

        assertEquals(Change.Type.NEW, changes.get(1).getType());
        assertNull(changes.get(1).getBeforeRevision());
        assertEquals(file2.getPath(), changes.get(1).getAfterRevision().getFile().getPath());

        assertEquals(Change.Type.NEW, changes.get(2).getType());
        assertNull(changes.get(2).getBeforeRevision());
        assertEquals(file3.getPath(), changes.get(2).getAfterRevision().getFile().getPath());

        assertEquals(Change.Type.NEW, changes.get(3).getType());
        assertNull(changes.get(3).getBeforeRevision());
        assertEquals(file8.getPath(), changes.get(3).getAfterRevision().getFile().getPath());

        assertEquals(Change.Type.DELETED, changes.get(4).getType());
        assertEquals(file4.getPath(), changes.get(4).getBeforeRevision().getFile().getPath());
        assertNull(changes.get(4).getAfterRevision());

        assertEquals(Change.Type.DELETED, changes.get(5).getType());
        assertEquals(file5.getPath(), changes.get(5).getBeforeRevision().getFile().getPath());
        assertNull(changes.get(5).getAfterRevision());

        assertEquals(Change.Type.NEW, changes.get(6).getType());
        assertNull(changes.get(6).getBeforeRevision());
        assertEquals(file6.getPath(), changes.get(6).getAfterRevision().getFile().getPath());

        assertEquals(Change.Type.MODIFICATION, changes.get(7).getType());
        assertEquals(file7.getPath(), changes.get(7).getBeforeRevision().getFile().getPath());
        assertEquals(file7.getPath(), changes.get(7).getAfterRevision().getFile().getPath());

        assertEquals(Change.Type.MODIFICATION, changes.get(8).getType());
        assertEquals(file9.getPath(), changes.get(8).getBeforeRevision().getFile().getPath());
        assertEquals(file9.getPath(), changes.get(8).getAfterRevision().getFile().getPath());

    }
}