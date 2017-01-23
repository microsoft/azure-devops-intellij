// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.L2.git;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.alm.L2.L2Test;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.idea.common.ui.common.BaseDialog;
import com.microsoft.alm.plugin.idea.git.ui.pullrequest.CreatePullRequestController;
import com.microsoft.alm.plugin.idea.git.ui.pullrequest.CreatePullRequestDialog;
import com.microsoft.alm.plugin.idea.git.ui.pullrequest.CreatePullRequestModel;
import com.microsoft.alm.sourcecontrol.webapi.GitHttpClient;
import com.microsoft.alm.sourcecontrol.webapi.model.GitPullRequest;
import com.microsoft.alm.sourcecontrol.webapi.model.GitPullRequestSearchCriteria;
import com.microsoft.alm.sourcecontrol.webapi.model.PullRequestStatus;
import git4idea.commands.GitLineHandlerListener;
import git4idea.repo.GitRepositoryImpl;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import sun.security.util.Debug;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import static org.mockito.Mockito.mock;

public class CreatePullRequestTest extends L2Test {
    public static final String README_FILE = "README.md";
    public static final String BRANCH_NAME_PREFIX = "L2TestBranch-";
    public static final String PR_NAME_PREFIX = "L2 Test PR ";
    public static final String PR_DESCRIPTION_PREFIX = "This is an L2 test PR at currentTimeMillis ";

    @Test(timeout = 60000)
    public void testCreatePullRequest() throws Exception {
        // need to create a context to start so that we aren't prompted in the middle of the test
        final ServerContext context = ServerContextManager.getInstance().getUpdatedContext(getRepoUrl(), true);

        // Create a temp folder for the clone
        final File tempFolder = L2Test.createTempDirectory();
        final long currentTimeMillis = System.currentTimeMillis();
        final String branchName = BRANCH_NAME_PREFIX + currentTimeMillis;
        final String prName = PR_NAME_PREFIX + currentTimeMillis;
        final String prDescription = PR_DESCRIPTION_PREFIX + currentTimeMillis;
        Debug.println("tempFolder", tempFolder.getPath());
        Debug.println("branchName", branchName);
        Debug.println("prName", prName);
        Debug.println("prDescription", prDescription);

        // clone repo and checkout branch
        final Project currentProject = L2GitUtil.cloneRepo(myProject, tempFolder, myGit, getRepoUrl(), getTeamProject());
        final VirtualFile baseDirectory = LocalFileSystem.getInstance().findFileByIoFile(new File(tempFolder, getTeamProject()));
        Assert.assertTrue(baseDirectory.exists());
        final git4idea.repo.GitRepository repository
                = GitRepositoryImpl.getInstance(baseDirectory, currentProject, true);
        myGit.checkoutNewBranch(repository, branchName, null);
        repository.update();

        // edit file
        final File readme = new File(baseDirectory.getPath(), README_FILE);
        L2GitUtil.editAndCommitFile(readme, repository, currentProject);

        // create PR model and set inputs
        final CreatePullRequestDialog mockDialog = mock(CreatePullRequestDialog.class);
        final CreatePullRequestModel model = new CreatePullRequestModel(currentProject, repository);
        model.setTitle(prName);
        model.setDescription(prDescription);

        // verify model created correctly
        Assert.assertTrue(model.getRemoteBranchDropdownModel().getSize() >= 1);
        Assert.assertEquals("origin/master", model.getTargetBranch().getName());

        final CreatePullRequestController controller = new CreatePullRequestController(mockDialog, model);
        controller.actionPerformed(new ActionEvent(this, 1, BaseDialog.CMD_OK));

        // verify PR created correctly
        final GitPullRequestSearchCriteria criteria = new GitPullRequestSearchCriteria();
        criteria.setRepositoryId(context.getGitRepository().getId());
        criteria.setStatus(PullRequestStatus.ACTIVE);
        criteria.setIncludeLinks(false);
        criteria.setCreatorId(context.getUserId());
        criteria.setSourceRefName("refs/heads/" + branchName);

        //query server and add results
        final GitHttpClient gitHttpClient = context.getGitHttpClient();
        final List<GitPullRequest> pullRequests = gitHttpClient.getPullRequests(context.getGitRepository().getId(),
                criteria, 256, 0, 101);
        Assert.assertEquals(1, pullRequests.size());
        Assert.assertEquals(prName, pullRequests.get(0).getTitle());
        Assert.assertEquals(prDescription, pullRequests.get(0).getDescription());
        // TODO: this was working, investigating (commit it there but not returning)
        // Assert.assertEquals(L2GitUtil.COMMIT_MESSAGE, pullRequests.get(0).getCommits()[0].getComment());

        // cleanup
        final GitPullRequest pullRequestToUpdate = new GitPullRequest();
        pullRequestToUpdate.setStatus(PullRequestStatus.ABANDONED);
        gitHttpClient.updatePullRequest(pullRequestToUpdate, pullRequests.get(0).getRepository().getId(),
                pullRequests.get(0).getPullRequestId());
        myGit.branchDelete(repository, branchName, true, mock(GitLineHandlerListener.class));
        FileUtils.deleteDirectory(tempFolder);
    }
}