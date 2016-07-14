// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.ui.pullrequest.PullRequestHelper.PRCreateStatus;
import com.microsoft.alm.sourcecontrol.webapi.GitHttpClient;
import com.microsoft.alm.sourcecontrol.webapi.model.GitPullRequest;
import com.microsoft.alm.sourcecontrol.webapi.model.GitPullRequestSearchCriteria;
import com.microsoft.alm.sourcecontrol.webapi.model.GitRepository;
import git4idea.GitCommit;
import git4idea.GitRemoteBranch;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class PullRequestHelperTest extends IdeaAbstractTest {

    PullRequestHelper underTest;

    Project projectMock;
    GitRepository gitRepositoryMock;
    VirtualFile fileMock;

    @Before
    public void setUp() throws Exception {
        underTest = new PullRequestHelper();
        fileMock = Mockito.mock(VirtualFile.class);
        projectMock = Mockito.mock(Project.class);
        gitRepositoryMock = Mockito.mock(GitRepository.class);

    }

    @Test
    public void noCommitsShouldReturnEmptyTitle() throws Exception {
        final List<GitCommit> commits = new ArrayList<GitCommit>();
        final String title = underTest.createDefaultTitle(commits, "source", "target");

        assertEquals(StringUtils.EMPTY, title);

        final String title2 = underTest.createDefaultTitle(null, "source", "target");
        assertEquals(StringUtils.EMPTY, title2);
    }

    @Test
    public void oneCommitTitleShouldBeCommitSubject() throws Exception {
        final List<GitCommit> commits = new ArrayList<GitCommit>();
        commits.add(PRGitObjectMockHelper.getCommit(projectMock, fileMock, "my subject", "my message"));

        final String title = underTest.createDefaultTitle(commits, "source", "target");
        assertEquals("my subject", title);
    }

    @Test
    public void multipleCommitsTitleShouldBeMergingFromTo() throws Exception {
        final List<GitCommit> commits = new ArrayList<GitCommit>();
        commits.add(PRGitObjectMockHelper.getCommit(projectMock, fileMock, "subject 1", "message 1"));
        commits.add(PRGitObjectMockHelper.getCommit(projectMock, fileMock, "subject 2", "message 2"));

        final String title = underTest.createDefaultTitle(commits, "source", "target");
        assertEquals("Merge source to target", title);
    }

    @Test
    public void noCommitsShouldReturnEmptyDescription() throws Exception {
        final List<GitCommit> commits = new ArrayList<GitCommit>();
        String desc = underTest.createDefaultDescription(commits);

        assertEquals(StringUtils.EMPTY, desc);

        desc = underTest.createDefaultDescription(null);
        assertEquals(StringUtils.EMPTY, desc);
    }

    @Test
    public void oneCommitDescShouldBeCommitMessage() throws Exception {
        final List<GitCommit> commits = new ArrayList<GitCommit>();
        commits.add(PRGitObjectMockHelper.getCommit(projectMock, fileMock, "my subject", "my message"));

        final String desc = underTest.createDefaultDescription(commits);
        assertEquals("my message", desc);
    }

    @Test
    public void multipleCommitsDescShouldBeListOfSubjects() throws Exception {
        final List<GitCommit> commits = new ArrayList<GitCommit>();
        commits.add(PRGitObjectMockHelper.getCommit(projectMock, fileMock, "subject 1", "message 1"));
        commits.add(PRGitObjectMockHelper.getCommit(projectMock, fileMock, "subject 2", "message 2"));

        final String desc = underTest.createDefaultDescription(commits);
        final String lineSeparator = System.getProperty("line.separator");
        assertEquals(String.format("-subject 1%s-subject 2%s", lineSeparator, lineSeparator), desc);
    }

    @Test
    public void parseNullErrorReturnsEmpty() {
        final Pair<PRCreateStatus, String> parsed = underTest.parseException(null, "source", null, null, null);
        assertEquals(PRCreateStatus.UNKNOWN, parsed.getFirst());
        assertEquals(StringUtils.EMPTY, parsed.getSecond());
    }

    @Test
    public void nonPRExistErrorWillReturnError() {
        final String error = "This exception doesn't Git Pull request Exists string description";
        final RuntimeException exception = new RuntimeException(error);
        final Pair<PRCreateStatus, String> parsed = underTest.parseException(exception, "source", null, null, null);
        assertEquals(PRCreateStatus.FAILED, parsed.getFirst());
        assertEquals(error, parsed.getSecond());
    }

    @Test
    public void parseGitExistExceptionShouldReturnLinkToExistingPR_Code() {
        parseExceptionForError("This exception contains " + PullRequestHelper.PR_EXISTS_EXCEPTION_CODE);
    }

    @Test
    public void parseGitExistExceptionShouldReturnLinkToExistingPR_Exception() {
        parseExceptionForError("This exception contains " + PullRequestHelper.PR_EXISTS_EXCEPTION_NAME);
    }

    public void parseExceptionForError(final String error) {
        final RuntimeException exception = new RuntimeException(error);

        final ServerContext context = Mockito.mock(ServerContext.class);
        when(context.getGitRepository()).thenReturn(gitRepositoryMock);

        final UUID repoId = UUID.randomUUID();
        when(gitRepositoryMock.getId()).thenReturn(repoId);

        final GitRemoteBranch targetBranch = PRGitObjectMockHelper.createRemoteBranch("target", null);
        final GitHttpClient gitClient = Mockito.mock(GitHttpClient.class);
        final GitPullRequest pr = new GitPullRequest();
        pr.setPullRequestId(100);
        final List<GitPullRequest> pullRequests = Collections.singletonList(pr);

        when(gitClient.getPullRequests(eq(repoId), any(GitPullRequestSearchCriteria.class), anyInt(), eq(0), eq(1))).thenReturn(pullRequests);

        final Pair<PRCreateStatus, String> parsed
                = underTest.parseException(exception, "source", targetBranch, context, gitClient);

        assertEquals(PRCreateStatus.DUPLICATE, parsed.getFirst());
    }

}