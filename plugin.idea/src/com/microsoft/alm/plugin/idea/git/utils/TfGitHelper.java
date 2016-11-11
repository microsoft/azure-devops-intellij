// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.utils;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import git4idea.GitRemoteBranch;
import git4idea.GitUtil;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class TfGitHelper {
    private static final String MASTER_BRANCH_PATTERN = "%s/master";

    /**
     * Returns <code>true</code> if the specified GitRepository is a TF GitRepository (VSO or OnPrem).
     *
     * @param gitRepository must not be <code>null</code>
     * @return
     */
    public static boolean isTfGitRepository(@NotNull final GitRepository gitRepository) {
        return getTfGitRemote(gitRepository) != null;
    }

    public static String getTfGitRemoteUrl(@NotNull final GitRepository gitRepository) {
        final GitRemote gitRemote = getTfGitRemote(gitRepository);
        if (gitRemote != null) {
            return gitRemote.getFirstUrl();
        }
        return null;
    }

    public static GitRemote getTfGitRemote(@NotNull final GitRepository gitRepository) {
        if (gitRepository == null) {
            throw new IllegalArgumentException();
        }
        GitRemote first = null;
        for (GitRemote gitRemote : gitRepository.getRemotes()) {
            if (isTfGitRemote(gitRemote)) {
                if (gitRemote.getName().equals("origin")) {
                    return gitRemote;
                } else if (first == null) {
                    first = gitRemote;
                }
            }
        }
        return first;
    }

    private static boolean isTfGitRemote(final GitRemote gitRemote) {
        if (gitRemote == null) {
            return false;
        }

        final String remoteUrl = gitRemote.getFirstUrl();
        if (remoteUrl != null && (remoteUrl.contains(".visualstudio.com/") || remoteUrl.contains(".tfsallin.net/") || remoteUrl.contains("/_git/"))) {
            //TODO once we have the connections cached, we should rework this to also query those for better OnPrem detection.
            return true;
        }
        return false;
    }

    public static Collection<GitRemote> getTfGitRemotes(@NotNull final GitRepository gitRepository) {
        assert gitRepository != null;
        Collection<GitRemote> gitRemotes = gitRepository.getRemotes();

        return Collections2.filter(gitRemotes, new Predicate<GitRemote>() {
            @Override
            public boolean apply(final GitRemote remote) {
                return TfGitHelper.isTfGitRemote(remote);
            }
        });
    }

    public static GitRepository getTfGitRepository(@NotNull final Project project) {
        if (project != null) {
            final List<GitRepository> repositories = GitUtil.getRepositoryManager(project).getRepositories();
            for (GitRepository repository : repositories) {
                if (TfGitHelper.isTfGitRepository(repository)) {
                    return repository;
                }
            }
        }

        return null;
    }

    /**
     * Returns the server context for the gitRepository if it already exists
     * Will not try to prompt the user and create the server context
     *
     * @param gitRepository
     * @return
     */
    public static ServerContext getSavedServerContext(@NotNull final GitRepository gitRepository) {
        //get saved server context, we don't want to prompt for credentials or handle expired credentials on the UI thread
        final ServerContext context = ServerContextManager.getInstance().get(getTfGitRemoteUrl(gitRepository));
        return context;
    }

    /**
     * This method for now assumes the default branch name is master
     * <p/>
     * If there is no master, return the first branch on the list or null for empty list
     * <p/>
     * We should get the default branch from TF if necessary, but that's a server call
     */
    @Nullable
    public static GitRemoteBranch getDefaultBranch(@NotNull final List<GitRemoteBranch> remoteBranches, @NotNull final Collection<GitRemote> tfGitRemotes) {
        if (remoteBranches.isEmpty() || tfGitRemotes.isEmpty()) {
            return null;
        }

        final GitRemote firstTfRemote = tfGitRemotes.iterator().next();

        final String masterBranchName = String.format(MASTER_BRANCH_PATTERN, firstTfRemote.getName());
        for (GitRemoteBranch remoteBranch : remoteBranches) {
            if (remoteBranch.getName().equals(masterBranchName)) {
                return remoteBranch;
            }
        }

        return remoteBranches.get(0);
    }

    /**
     * This method gets the TFGit remote name for the GitRemote and then forms the remote branch name
     * @param branchName the local branch name without any prefix (refs/heads/)
     * @return returns the remote branch name like origin/branchName
     */
    public static String getRemoteBranchName(GitRemote remote, String branchName) {
        ArgumentHelper.checkNotNull(remote, "remote");
        ArgumentHelper.checkNotEmptyString(branchName, "branchName");
        return remote.getName() + "/" + branchName;
    }

    public static class BranchComparator implements Comparator<GitRemoteBranch>, Serializable {
        private static final long serialVersionUID = 2526372195429182934L;

        @Override
        public int compare(GitRemoteBranch branch1, GitRemoteBranch branch2) {
            return StringUtil.naturalCompare(branch1.getFullName(), branch2.getFullName());
        }
    }
}
