// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.RepositoryContext;
import com.microsoft.alm.plugin.external.commands.FindWorkspaceCommand;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.idea.git.utils.TfGitHelper;
import com.microsoft.alm.plugin.idea.tfvc.core.TFSVcs;
import git4idea.GitUtil;
import git4idea.GitVcs;
import git4idea.branch.GitBranchUtil;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;

public class VcsHelper {
    /**
     * Use this method to see if the given project is using Git as its version control system.
     */
    public static boolean isGitVcs(final Project project) {
        ArgumentHelper.checkNotNull(project, "project");
        final ProjectLevelVcsManager projectLevelVcsManager = ProjectLevelVcsManager.getInstance(project);
        return projectLevelVcsManager.checkVcsIsActive(GitVcs.NAME);
    }

    /**
     * Use this method to see if the given project is using TFVC as its version control system.
     */
    public static boolean isTfVcs(final Project project) {
        ArgumentHelper.checkNotNull(project, "project");
        final ProjectLevelVcsManager projectLevelVcsManager = ProjectLevelVcsManager.getInstance(project);
        return projectLevelVcsManager.checkVcsIsActive(TFSVcs.TFVC_NAME);
    }

    /**
     * Returns the Git repository object for the project or null if this is not a Git repo project.
     * @param project
     * @return
     */
    public static GitRepository getGitRepository(final Project project) {
        if (isGitVcs(project)) {
            final GitRepositoryManager manager = GitUtil.getRepositoryManager(project);
            final GitRepository repository = manager.getRepositoryForRoot(project.getBaseDir());
            return repository;
        }
        return null;
    }

    /**
     * This method creates a RepositoryContext object from the local project context.
     * It works for TF Git or TFVC repositories. Any other type of repo will return null.
     * @param project
     * @return
     */
    public static RepositoryContext getRepositoryContext(final Project project) {
        ArgumentHelper.checkNotNull(project, "project");
        final ProjectLevelVcsManager projectLevelVcsManager = ProjectLevelVcsManager.getInstance(project);
        // Check for Git, then TFVC
        if (projectLevelVcsManager.checkVcsIsActive(GitVcs.NAME)) {
            // It's Git, so get the repository and remote url to create the context from
            final GitRepositoryManager manager = GitUtil.getRepositoryManager(project);
            final GitRepository repository = manager.getRepositoryForRoot(project.getBaseDir());
            if (repository != null && TfGitHelper.isTfGitRepository(repository)) {
                final GitRemote gitRemote = TfGitHelper.getTfGitRemote(repository);
                final String gitRemoteUrl = gitRemote.getFirstUrl();
                // TODO: Fix this HACK. There doesn't seem to be a clear way to get the full name of the current branch
                final String branch = "refs/heads/" + GitBranchUtil.getDisplayableBranchText(repository);
                return RepositoryContext.createGitContext(repository.getRoot().getName(), branch, gitRemoteUrl);
            }
        } else if (projectLevelVcsManager.checkVcsIsActive(TFSVcs.TFVC_NAME)) {
            // It's TFVC so run the FindWorkspace command to get the workspace object which as the server info
            final FindWorkspaceCommand command = new FindWorkspaceCommand(null, project.getBasePath());
            final Workspace workspace = command.runSynchronously();
            return RepositoryContext.createTfvcContext(workspace.getName(), "$/", workspace.getServer());
        }

        // We couldn't determine the VCS provider, so return null
        return null;
    }
}
