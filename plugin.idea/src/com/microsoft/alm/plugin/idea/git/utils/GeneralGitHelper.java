// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import git4idea.GitBranch;
import git4idea.GitRevisionNumber;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

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
        try {
            GitRevisionNumber revision = GitRevisionNumber.resolve(project, gitRepository.getRoot(), branch.getName());
            return revision.asString();
        } catch (VcsException e) {
            throw new VcsException(e.getCause());
        }
    }
}
