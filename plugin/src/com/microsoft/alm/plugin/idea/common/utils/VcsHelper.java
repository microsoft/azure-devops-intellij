// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.RepositoryContext;
import com.microsoft.alm.plugin.context.RepositoryContextManager;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.git.utils.TfGitHelper;
import com.microsoft.alm.plugin.idea.tfvc.core.TFSVcs;
import git4idea.GitUtil;
import git4idea.GitVcs;
import git4idea.branch.GitBranchUtil;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VcsHelper {
    private static final Logger logger = LoggerFactory.getLogger(VcsHelper.class);

    public static final String GIT_BRANCH_PREFIX = "refs/heads/";
    public static final String TFVC_ROOT = "$/";
    public static final String TFVC_SEPARATOR = "/";
    private static final String UNKNOWN = "Unknown";

    private static Pattern pattern = null;

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
     * Find the VCS type of the given project
     *
     * @param project
     * @return
     */
    public static String getVcsType(final Project project) {
        ArgumentHelper.checkNotNull(project, "project");
        final ProjectLevelVcsManager projectLevelVcsManager = ProjectLevelVcsManager.getInstance(project);
        AbstractVcs vcs = null;
        try {
            vcs = projectLevelVcsManager.getVcsFor(project.getBaseDir());
        } catch (Throwable t) {
            // This has been seen in PyCharm as a bug where it tries to open the site-packages as a source root
            // It has been fixed here going forward:
            // https://github.com/JetBrains/intellij-community/commit/7e459335eda7c90b6eb0c7e1c3b35e329dffe197
            logger.warn("Error hit while trying to detect Vcs", t);
        }
        return vcs != null && StringUtils.isNotEmpty(vcs.getName()) ? vcs.getName() : UNKNOWN;
    }

    /**
     * Use this method to check if the given project is a VSTS/TFS project
     *
     * @param project
     * @return
     */
    public static boolean isVstsRepo(final Project project) {
        if (project != null) {
            if (isTfVcs(project)) {
                return true;
            }
            if (!isGitVcs(project)) {
                return false;
            }

            return ContainerUtil.exists(GitUtil.getRepositoryManager(project).getRepositories(), new Condition<GitRepository>() {
                @Override
                public boolean value(GitRepository gitRepository) {
                    return TfGitHelper.isTfGitRepository(gitRepository);
                }
            });
        }
        return false;
    }

    /**
     * Returns the Git repository object for the project or null if this is not a Git repo project.
     *
     * @param project
     * @return
     */
    public static GitRepository getGitRepository(final Project project) {
        if (isGitVcs(project)) {
            final GitRepositoryManager manager = GitUtil.getRepositoryManager(project);
            GitRepository repository = manager.getRepositoryForRootQuick(project.getBaseDir());

            // in the case where the base dir of the Git repo and the base dir of IDEA project don't match this can be null
            if (repository == null) {
                final List<GitRepository> repos = manager.getRepositories();
                if (repos.size() > 0) {
                    repository = repos.get(0);
                    if (repos.size() > 1) {
                        logger.warn("More than 1 Git repo was found. Defaulting to the first returned: " + repository.getRoot().getPath());
                    }
                } else {
                    logger.warn("We are in a Git project that does not have any Git repos. (We may be asking too early.)");
                }
            }

            return repository;
        }
        return null;
    }

    /**
     * This method creates a RepositoryContext object from the local project context.
     * It works for TF Git or TFVC repositories. Any other type of repo will return null.
     *
     * @param project
     * @return
     */
    public static RepositoryContext getRepositoryContext(final Project project) {
        ArgumentHelper.checkNotNull(project, "project");
        try {
            final String projectRootFolder = project.getBasePath();

            // Check the manager first since that's where we cache these things
            //TODO this cache doesn't include the current branch info that could have changed. We should probably only cache stuff for TFVC
            RepositoryContext context = RepositoryContextManager.getInstance().get(projectRootFolder);
            if (context != null) {
                logger.info("getRepositoryContext: cache hit: " + projectRootFolder);
                return context;
            }
            logger.info("getRepositoryContext: cache miss: " + projectRootFolder);

            final ProjectLevelVcsManager projectLevelVcsManager = ProjectLevelVcsManager.getInstance(project);
            // Check for Git, then TFVC
            if (projectLevelVcsManager.checkVcsIsActive(GitVcs.NAME)) {
                // It's Git, so get the repository and remote url to create the context from
                final GitRepository repository = getGitRepository(project);
                if (repository != null && TfGitHelper.isTfGitRepository(repository)) {
                    final GitRemote gitRemote = TfGitHelper.getTfGitRemote(repository);
                    final String gitRemoteUrl = Objects.requireNonNull(gitRemote.getFirstUrl());
                    // TODO: Fix this HACK. There doesn't seem to be a clear way to get the full name of the current branch
                    final String branch = GIT_BRANCH_PREFIX + GitBranchUtil.getDisplayableBranchText(repository);
                    context = RepositoryContext.createGitContext(projectRootFolder, repository.getRoot().getName(), branch, URI.create(gitRemoteUrl));
                }
            } else if (projectLevelVcsManager.checkVcsIsActive(TFSVcs.TFVC_NAME)) {
                final Workspace workspace = CommandUtils.getPartialWorkspace(project, false);
                if (workspace != null) {
                    final String projectName = getTeamProjectFromTfvcServerPath(
                            workspace.getMappings().size() > 0 ? workspace.getMappings().get(0).getServerPath() : null);
                    context = RepositoryContext.createTfvcContext(projectRootFolder, workspace.getName(), projectName, workspace.getServerUri());
                }
            }

            if (context != null) {
                RepositoryContextManager.getInstance().add(context);
                return context;
            }
        } catch (Throwable t) {
            // Don't let errors bubble out here, just return null if something goes wrong
            logger.warn("Unable to get repository context for the project.", t);
        }

        logger.info("getRepositoryContext: We couldn't determine the VCS provider, so returning null.");
        return null;
    }

    /**
     * Use this method to get the team project name from a TFVC server path.
     * The team project name is always the first folder in the path.
     * If no team project name is found an empty string is returned.
     *
     * @param serverPath
     * @return
     */
    public static String getTeamProjectFromTfvcServerPath(final String serverPath) {
        if (StringUtils.isNotEmpty(serverPath) &&
                StringUtils.startsWith(serverPath, TFVC_ROOT) &&
                serverPath.length() > 2) {
            // Find the next separator after the $/
            final int index = serverPath.indexOf(TFVC_SEPARATOR, 2);
            if (index >= 0) {
                return serverPath.substring(2, index);
            } else {
                return serverPath.substring(2);
            }
        }

        logger.info("getTeamProjectFromTfvcServerPath: No project was found.");
        return StringUtils.EMPTY;
    }

    public static List<Integer> getWorkItemIdsFromMessage(final String commitMessage) {
        logger.info("getWorkItemIdsFromMessage: commitMessage = " + commitMessage);
        final List<Integer> workItems = new ArrayList<Integer>(10);
        // We cache the compiled pattern for later use
        if (pattern == null) {
            pattern = Pattern.compile("#(\\d+)");
        }
        final Matcher matcher = pattern.matcher(commitMessage);
        // finds all matches in the string where it is a # followed by an int
        while (matcher.find()) {
            try {
                final int workItemId = Integer.parseInt(StringUtils.removeStart(matcher.group(), "#"));
                workItems.add(workItemId);
            } catch (NumberFormatException e) {
                logger.warn("Error converting work item id into integer: " + matcher.group(1));
            }
        }
        return workItems;
    }

    /**
     * Finds the TFSVcs instance associated with the given file path
     * or null if the file isn't under a TFVC repo
     *
     * @param file
     * @return
     */
    public static TFSVcs getTFSVcsByPath(final VirtualFile file) {
        final ProjectManager projectManager = ProjectManager.getInstance();
        if (projectManager != null) {
            final Project[] projects = projectManager.getOpenProjects();
            for (final Project project : projects) {
                final ProjectLevelVcsManager projectLevelVcsManager = ProjectLevelVcsManager.getInstance(project);
                if (projectLevelVcsManager != null) {
                    final AbstractVcs vcs = projectLevelVcsManager.getVcsFor(file);
                    if (vcs instanceof TFSVcs) {
                        return (TFSVcs) vcs;
                    }
                }
            }
        }
        return null;
    }
}