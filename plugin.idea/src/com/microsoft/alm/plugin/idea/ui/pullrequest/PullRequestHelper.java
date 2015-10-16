// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitPullRequest;
import git4idea.GitCommit;
import git4idea.GitRemoteBranch;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Helper utility class for pull request related methods
 */
public class PullRequestHelper {

    private static final String WEB_ACCESS_PR_FORMAT = "%s/pullrequest/%d#view=discussion";
    private static final String TF_REF_FORMATTER = "refs/heads/%s";

    public String createDefaultTitle(final List<GitCommit> commits, final String sourceBranchName,
                                     final String targetBranchName) {

        if (commits == null || commits.isEmpty()) {
            return StringUtils.EMPTY;
        }

        if (commits.size() == 1) {
            // if we only have one commit, use it's title as the title of pull request
            final GitCommit commit = commits.get(0);
            final String commitMessage = commit.getSubject();

            //WebAcess use 80 (because it's common code for many things, and 80 is such a magic number),
            //but IMHO 80 is too short for title, set it to 120
            final int titleLength = 120;
            if (commitMessage.length() < titleLength) {
                return commitMessage;
            } else {
                // break at last whitespace right before length 120
                final String shortCommitMessage = commitMessage.substring(0, titleLength);
                return StringUtils.substringBeforeLast(shortCommitMessage, "\\s+");
            }
        }

        // Standard title "merging source branch to target branch"
        return TfPluginBundle.message(TfPluginBundle.KEY_CREATE_PR_DEFAULT_TITLE, sourceBranchName, targetBranchName);
    }

    public String createDefaultDescription(final List<GitCommit> commits) {
        if (commits == null || commits.isEmpty()) {
            return StringUtils.EMPTY;
        }

        if (commits.size() == 1) {
            final GitCommit commit = commits.get(0);
            if (commit.getFullMessage().length() > CreatePullRequestModel.MAX_SIZE_DESCRIPTION) {
                return commit.getFullMessage().substring(0, CreatePullRequestModel.MAX_SIZE_DESCRIPTION - 5) + "...";
            }

            return commit.getFullMessage();
        }

        final StringBuilder descBuilder = new StringBuilder();
        final String lineSeparator = System.getProperty("line.separator");

        // WebAccess limit, at most we look at the last 10 commits
        final int descCommitsLimit = 10;
        for (int i = 0; i < descCommitsLimit && i < commits.size(); ++i) {
            final GitCommit commit = commits.get(i);
            descBuilder.append("-").append(commit.getSubject()).append(lineSeparator);
        }

        // there is a chance there are more than 10 commits
        if (commits.size() > descCommitsLimit) {
            descBuilder.append("...");
        }

        // oh well, if it's longer than 4000 chars, we probably won't care about the last few words
        // we will lose our "..." and may break off in the middle of a word
        return descBuilder.length() < CreatePullRequestModel.MAX_SIZE_DESCRIPTION
                ? descBuilder.toString() : descBuilder.substring(0, CreatePullRequestModel.MAX_SIZE_DESCRIPTION - 10);
    }

    public String getHtmlMsg(final String repositoryRemoteUrl, final int id) {
        final String text = TfPluginBundle.message(TfPluginBundle.KEY_CREATE_PR_CREATED_MESSAGE, id);
        final String webAccessUrl = String.format(WEB_ACCESS_PR_FORMAT, repositoryRemoteUrl, id);
        return String.format(UrlHelper.SHORT_HTTP_LINK_FORMATTER, webAccessUrl, text);
    }


    public GitPullRequest generateGitPullRequest(@NotNull final String title,
                                                  @NotNull final String description,
                                                  @NotNull final String branchNameOnRemoteServer,
                                                  @NotNull final GitRemoteBranch targetBranch) {
        final GitPullRequest pullRequest = new GitPullRequest();
        pullRequest.setTitle(title);
        pullRequest.setDescription(description);
        pullRequest.setSourceRefName(String.format(TF_REF_FORMATTER, branchNameOnRemoteServer));
        pullRequest.setTargetRefName(String.format(TF_REF_FORMATTER, targetBranch.getNameForRemoteOperations()));

        return pullRequest;
    }
}
