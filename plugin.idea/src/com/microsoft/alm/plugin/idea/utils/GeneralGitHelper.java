// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import git4idea.GitBranch;
import git4idea.GitCommit;
import git4idea.history.GitHistoryUtils;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * General helper class for Git functionality using Git4idea
 */
public class GeneralGitHelper {

    /**
     * Get the hash associated with the last commit on a branch
     *
     * @param project
     * @param gitRepository
     * @param branch
     * @return
     * @throws VcsException
     */
    public static String getLastCommitHash(@NotNull final Project project, @NotNull final GitRepository gitRepository,
                                           @NotNull final GitBranch branch) throws VcsException {
        final List<GitCommit> branchHistory;

        try {
            branchHistory = GitHistoryUtils.history(project, gitRepository.getRoot(), branch.getName());
        } catch (VcsException e) {
            throw new VcsException(e.getCause());
        }

        // check that history exists in branch. If not, throw an exception because there is no acceptable scenario
        // where a branch will have no history. We need the hash to do the diff.
        if (branchHistory.isEmpty()) {
            throw new VcsException(TfPluginBundle.message(TfPluginBundle.KEY_GIT_HISTORY_ERRORS_NO_HISTORY_FOUND,
                    branch.getName()));
        }

        // get hash of last commit
        return branchHistory.get(0).getId().asString();
    }
}
