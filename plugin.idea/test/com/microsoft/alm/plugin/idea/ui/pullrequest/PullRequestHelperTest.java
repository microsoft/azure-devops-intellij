// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import git4idea.GitCommit;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class PullRequestHelperTest extends IdeaAbstractTest {

    PullRequestHelper underTest;

    Project projectMock;
    VirtualFile fileMock;

    @Before
    public void setUp() throws Exception {
        underTest = new PullRequestHelper();
        fileMock = Mockito.mock(VirtualFile.class);
        projectMock = Mockito.mock(Project.class);
    }

    @Test
    public void noCommitsShouldReturnEmptyTitle() throws Exception {
        List<GitCommit> commits = new ArrayList<GitCommit>();
        String title = underTest.createDefaultTitle(commits, "source", "target");

        assertEquals(StringUtils.EMPTY, title);

        title = underTest.createDefaultTitle(null, "source", "target");
        assertEquals(StringUtils.EMPTY, title);
    }

    @Test
    public void oneCommitTitleShouldBeCommitSubject() throws Exception {
        List<GitCommit> commits = new ArrayList<GitCommit>();
        commits.add(PRGitObjectMockHelper.getCommit(projectMock, fileMock, "my subject", "my message"));

        String title = underTest.createDefaultTitle(commits, "source", "target");
        assertEquals("my subject", title);
    }

    @Test
    public void multipleCommitsTitleShouldBeMergingFromTo() throws Exception {
        List<GitCommit> commits = new ArrayList<GitCommit>();
        commits.add(PRGitObjectMockHelper.getCommit(projectMock, fileMock, "subject 1", "message 1"));
        commits.add(PRGitObjectMockHelper.getCommit(projectMock, fileMock, "subject 2", "message 2"));

        String title = underTest.createDefaultTitle(commits, "source", "target");
        assertEquals("Merge source to target", title);
    }

    @Test
    public void noCommitsShouldReturnEmptyDescription() throws Exception {
        List<GitCommit> commits = new ArrayList<GitCommit>();
        String desc = underTest.createDefaultDescription(commits);

        assertEquals(StringUtils.EMPTY, desc);

        desc = underTest.createDefaultDescription(null);
        assertEquals(StringUtils.EMPTY, desc);
    }

    @Test
    public void oneCommitDescShouldBeCommitMessage() throws Exception {
        List<GitCommit> commits = new ArrayList<GitCommit>();
        commits.add(PRGitObjectMockHelper.getCommit(projectMock, fileMock, "my subject", "my message"));

        final String desc = underTest.createDefaultDescription(commits);
        assertEquals("my message", desc);
    }

    @Test
    public void multipleCommitsDescShouldBeListOfSubjects() throws Exception {
        List<GitCommit> commits = new ArrayList<GitCommit>();
        commits.add(PRGitObjectMockHelper.getCommit(projectMock, fileMock, "subject 1", "message 1"));
        commits.add(PRGitObjectMockHelper.getCommit(projectMock, fileMock, "subject 2", "message 2"));

        final String desc = underTest.createDefaultDescription(commits);
        final String lineSeparator = System.getProperty("line.separator");
        assertEquals(String.format("-subject 1%s-subject 2%s", lineSeparator, lineSeparator), desc);
    }
}