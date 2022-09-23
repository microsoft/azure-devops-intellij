// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.vcs.FilePath;
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
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
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
    private static final String PATH_PREFIX = File.separator + "Users" + File.separator +
            "user" + File.separator + "workspaceName" + File.separator;

    private static final String PATH_FILE_1 = PATH_PREFIX + "file1";
    private static final String PATH_FILE_2 = PATH_PREFIX + "file2";
    private static final String PATH_FILE_3 = PATH_PREFIX + "file3";
    private static final String PATH_FILE_4 = PATH_PREFIX + "file4";
    private static final String PATH_FILE_5 = PATH_PREFIX + "file5";
    private static final String PATH_FILE_6 = PATH_PREFIX + "file6";
    private static final String PATH_FILE_7 = PATH_PREFIX + "file7";
    private static final String PATH_FILE_8 = PATH_PREFIX + "file8";

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
    @Mock
    private FilePath file1, file2, file3, file4, file5, file6, file7, file8, file9;

    private TFSChangeListBuilder changeListBuilder;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(TfsFileUtil.class, VcsUtil.class, VersionControlPath.class);

        when(file1.getPath()).thenReturn(PATH_FILE_1);
        when(file2.getPath()).thenReturn(PATH_FILE_2);
        when(file3.getPath()).thenReturn(PATH_FILE_3);
        when(file4.getPath()).thenReturn(PATH_FILE_4);
        when(file5.getPath()).thenReturn(PATH_FILE_5);
        when(file6.getPath()).thenReturn(PATH_FILE_6);
        when(file7.getPath()).thenReturn(PATH_FILE_7);
        when(file8.getPath()).thenReturn(PATH_FILE_8);
        when(file9.getPath()).thenReturn(PATH_FILE_1); // having a duplicate entry on purpose

        when(TfsFileUtil.translateServerItemToLocalItem(any(List.class), eq(checkedInChange1.getServerItem()))).thenReturn(PATH_FILE_1);
        when(TfsFileUtil.translateServerItemToLocalItem(any(List.class), eq(checkedInChange2.getServerItem()))).thenReturn(PATH_FILE_2);
        when(TfsFileUtil.translateServerItemToLocalItem(any(List.class), eq(checkedInChange3.getServerItem()))).thenReturn(PATH_FILE_3);
        when(TfsFileUtil.translateServerItemToLocalItem(any(List.class), eq(checkedInChange4.getServerItem()))).thenReturn(PATH_FILE_4);
        when(TfsFileUtil.translateServerItemToLocalItem(any(List.class), eq(checkedInChange5.getServerItem()))).thenReturn(PATH_FILE_5);
        when(TfsFileUtil.translateServerItemToLocalItem(any(List.class), eq(checkedInChange6.getServerItem()))).thenReturn(PATH_FILE_6);
        when(TfsFileUtil.translateServerItemToLocalItem(any(List.class), eq(checkedInChange7.getServerItem()))).thenReturn(PATH_FILE_7);
        when(TfsFileUtil.translateServerItemToLocalItem(any(List.class), eq(checkedInChange8.getServerItem()))).thenReturn(PATH_FILE_8);
        when(TfsFileUtil.translateServerItemToLocalItem(any(List.class), eq(checkedInChange9.getServerItem()))).thenReturn(PATH_FILE_1);

        when(VcsUtil.getFilePath(eq(PATH_FILE_1), anyBoolean())).thenReturn(file1);
        when(VcsUtil.getFilePath(eq(PATH_FILE_2), anyBoolean())).thenReturn(file2);
        when(VcsUtil.getFilePath(eq(PATH_FILE_3), anyBoolean())).thenReturn(file3);
        when(VcsUtil.getFilePath(eq(PATH_FILE_4), anyBoolean())).thenReturn(file4);
        when(VcsUtil.getFilePath(eq(PATH_FILE_5), anyBoolean())).thenReturn(file5);
        when(VcsUtil.getFilePath(eq(PATH_FILE_6), anyBoolean())).thenReturn(file6);
        when(VcsUtil.getFilePath(eq(PATH_FILE_7), anyBoolean())).thenReturn(file7);
        when(VcsUtil.getFilePath(eq(PATH_FILE_8), anyBoolean())).thenReturn(file8);
        when(VcsUtil.getFilePath(eq(PATH_FILE_1), anyBoolean())).thenReturn(file9);

        when(VersionControlPath.getFilePath(eq(PATH_FILE_1), anyBoolean())).thenReturn(file1);
        when(VersionControlPath.getFilePath(eq(PATH_FILE_2), anyBoolean())).thenReturn(file2);
        when(VersionControlPath.getFilePath(eq(PATH_FILE_3), anyBoolean())).thenReturn(file3);
        when(VersionControlPath.getFilePath(eq(PATH_FILE_4), anyBoolean())).thenReturn(file4);
        when(VersionControlPath.getFilePath(eq(PATH_FILE_5), anyBoolean())).thenReturn(file5);
        when(VersionControlPath.getFilePath(eq(PATH_FILE_6), anyBoolean())).thenReturn(file6);
        when(VersionControlPath.getFilePath(eq(PATH_FILE_7), anyBoolean())).thenReturn(file7);
        when(VersionControlPath.getFilePath(eq(PATH_FILE_8), anyBoolean())).thenReturn(file8);
        when(VersionControlPath.getFilePath(eq(PATH_FILE_1), anyBoolean())).thenReturn(file9);

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