// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.LocalFilePath;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.vcsUtil.VcsUtil;
import com.microsoft.alm.plugin.idea.MockedIdeaApplicationTest;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class TFSChangeListTest extends MockedIdeaApplicationTest {
    private static final int CHANGESET_ID = 123;
    private static final String AUTHOR = "John Smith";
    private static final String CHANGESET_DATE = "2016-08-15T11:50:09.427-0400";
    private static final int PREVIOUS_CHANGESET_ID = 122;
    private static final String PREVIOUS_CHANGESET_DATE = "2016-06-07T11:18:18.790-0400";
    private static final String COMMENT = "Made changes to the utils files";
    private static final String WORKSPACE_NAME = "workspaceName";
    private static final SimpleDateFormat TFVC_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    // paths of changes
    private FilePath addedFilePath1 = new LocalFilePath("/Users/user/workspaceName/addedFile1", true);
    private FilePath addedFilePath2 = new LocalFilePath("/Users/user/workspaceName/addedFile2", false);
    private FilePath addedFilePath3 = new LocalFilePath("/Users/user/workspaceName/addedFile3", false);
    private FilePath deletedFilePath1 = new LocalFilePath("/Users/user/workspaceName/deletedFile1", true);
    private FilePath deletedFilePath2 = new LocalFilePath("/Users/user/workspaceName/deletedFile2", false);
    private FilePath renamedFilePath1 = new LocalFilePath("/Users/user/workspaceName/renameFile1", true);
    private FilePath editedFilePath1 = new LocalFilePath("/Users/user/workspaceName/editedFile1", false);
    private FilePath editedFilePath2 = new LocalFilePath("/Users/user/workspaceName/editedFile2", true);
    private FilePath editedFilePath3 = new LocalFilePath("/Users/user/workspaceName/editedFile3", false);

    private final List<FilePath> addedFiles = ImmutableList.of(addedFilePath1, addedFilePath2, addedFilePath3);
    private final List<FilePath> deletedFiles = ImmutableList.of(deletedFilePath1, deletedFilePath2);
    private final List<FilePath> renamedFiles = ImmutableList.of(renamedFilePath1);
    private final List<FilePath> editedFiles = ImmutableList.of(editedFilePath1, editedFilePath2, editedFilePath3);

    @Mock
    private TFSVcs mockVcs;

    private TFSChangeList changeList;

    @Mock
    private MockedStatic<VcsUtil> vcsUtilStatic;

    @Before
    public void setUp() {
        vcsUtilStatic.when(() -> VcsUtil.getFilePath(addedFilePath1.getPath(), addedFilePath1.isDirectory())).thenReturn(addedFilePath1);
        vcsUtilStatic.when(() -> VcsUtil.getFilePath(addedFilePath2.getPath(), addedFilePath2.isDirectory())).thenReturn(addedFilePath2);
        vcsUtilStatic.when(() -> VcsUtil.getFilePath(addedFilePath3.getPath(), addedFilePath3.isDirectory())).thenReturn(addedFilePath3);
        vcsUtilStatic.when(() -> VcsUtil.getFilePath(deletedFilePath1.getPath(), deletedFilePath1.isDirectory())).thenReturn(deletedFilePath1);
        vcsUtilStatic.when(() -> VcsUtil.getFilePath(deletedFilePath2.getPath(), deletedFilePath2.isDirectory())).thenReturn(deletedFilePath2);
        vcsUtilStatic.when(() -> VcsUtil.getFilePath(renamedFilePath1.getPath(), renamedFilePath1.isDirectory())).thenReturn(renamedFilePath1);
        vcsUtilStatic.when(() -> VcsUtil.getFilePath(editedFilePath1.getPath(), editedFilePath1.isDirectory())).thenReturn(addedFilePath1);
        vcsUtilStatic.when(() -> VcsUtil.getFilePath(editedFilePath2.getPath(), editedFilePath2.isDirectory())).thenReturn(editedFilePath2);
        vcsUtilStatic.when(() -> VcsUtil.getFilePath(editedFilePath3.getPath(), editedFilePath3.isDirectory())).thenReturn(editedFilePath3);

        changeList = new TFSChangeList(addedFiles, deletedFiles, renamedFiles,
                editedFiles, CHANGESET_ID, AUTHOR, COMMENT,
                CHANGESET_DATE, PREVIOUS_CHANGESET_ID, PREVIOUS_CHANGESET_DATE,
                mockVcs, WORKSPACE_NAME);
    }

    @Test
    public void testGetCommitterName() {
        assertEquals(AUTHOR, changeList.getCommitterName());
    }

    @Test
    public void testGetCommitDate() throws Exception {
        assertEquals(TFVC_DATE_FORMAT.parse(CHANGESET_DATE).toString(), changeList.getCommitDate().toString());
    }

    @Test
    public void testGetNumber() {
        assertEquals(CHANGESET_ID, changeList.getNumber());
    }

    @Test
    public void testGetBranch() {
        assertNull(changeList.getBranch());
    }

    @Test
    public void testGetVcs() {
        assertEquals(mockVcs, changeList.getVcs());
    }

    @Test
    public void testGetChanges() {
        final Collection<Change> changes = changeList.getChanges();

        final List<Change> changesList = new ArrayList<Change>(changes);
        assertEquals(9, changesList.size());

        assertEquals(Change.Type.NEW, changesList.get(0).getType());
        assertNull(changesList.get(0).getBeforeRevision());
        assertEquals(addedFilePath1, changesList.get(0).getAfterRevision().getFile());

        assertEquals(Change.Type.NEW, changesList.get(1).getType());
        assertNull(changesList.get(1).getBeforeRevision());
        assertEquals(addedFilePath2, changesList.get(1).getAfterRevision().getFile());

        assertEquals(Change.Type.NEW, changesList.get(2).getType());
        assertNull(changesList.get(2).getBeforeRevision());
        assertEquals(addedFilePath3, changesList.get(2).getAfterRevision().getFile());

        assertEquals(Change.Type.DELETED, changesList.get(3).getType());
        assertEquals(deletedFilePath1, changesList.get(3).getBeforeRevision().getFile());
        assertNull(changesList.get(3).getAfterRevision());

        assertEquals(Change.Type.DELETED, changesList.get(4).getType());
        assertEquals(deletedFilePath2, changesList.get(4).getBeforeRevision().getFile());
        assertNull(changesList.get(4).getAfterRevision());

        assertEquals(Change.Type.NEW, changesList.get(5).getType());
        assertNull(changesList.get(5).getBeforeRevision());
        assertEquals(renamedFilePath1, changesList.get(5).getAfterRevision().getFile());

        assertEquals(Change.Type.MODIFICATION, changesList.get(6).getType());
        assertEquals(editedFilePath1, changesList.get(6).getBeforeRevision().getFile());
        assertEquals(editedFilePath1, changesList.get(6).getAfterRevision().getFile());

        assertEquals(Change.Type.MODIFICATION, changesList.get(7).getType());
        assertEquals(editedFilePath2, changesList.get(7).getBeforeRevision().getFile());
        assertEquals(editedFilePath2, changesList.get(7).getAfterRevision().getFile());

        assertEquals(Change.Type.MODIFICATION, changesList.get(8).getType());
        assertEquals(editedFilePath3, changesList.get(8).getBeforeRevision().getFile());
        assertEquals(editedFilePath3, changesList.get(8).getAfterRevision().getFile());
    }

    @Test
    public void testIsModifiable() {
        assertTrue(changeList.isModifiable());
    }

    @Test
    public void testSetDescription_NotNull() {
        changeList.setDescription("New description");
        assertEquals("New description", changeList.getComment());
    }

    @Test
    public void testSetDescription_Null() {
        changeList.setDescription(null);
        assertEquals(StringUtils.EMPTY, changeList.getComment());
    }

    @Test
    public void testGetName() {
        assertEquals(COMMENT, changeList.getName());
    }

    @Test
    public void testGetComment() {
        assertEquals(COMMENT, changeList.getComment());
    }

    @Test
    public void testReadWriteStream() throws Exception {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final DataOutput streamOut = new DataOutputStream(output);
        changeList.writeToStream(streamOut);

        byte bytes[] = output.toByteArray();
        final InputStream input = new ByteArrayInputStream(bytes);
        final DataInput streamIn = new DataInputStream(input);
        final TFSChangeList newChangeList = new TFSChangeList(mockVcs, streamIn);

        assertTrue(changeList.equals(newChangeList));
    }
}