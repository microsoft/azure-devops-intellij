// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.idea.resources.Icons;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.utils.TfGitHelper;
import git4idea.GitLocalBranch;
import git4idea.GitRemoteBranch;
import git4idea.GitUtil;
import git4idea.GitVcs;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class OpenFileInBrowserAction extends InstrumentedAction {

    private static final Logger logger = LoggerFactory.getLogger(OpenFileInBrowserAction.class);

    protected OpenFileInBrowserAction() {
        super(TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_OPEN_BROWSER),
                TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_OPEN_BROWSER_MSG),
                Icons.VSLogoSmall);
    }

    @Override
    public void doUpdate(@NotNull final AnActionEvent anActionEvent) {
        final Presentation presentation = anActionEvent.getPresentation();
        final Project project = anActionEvent.getProject();
        if (project == null || project.isDisposed()) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        final VirtualFile[] vFiles = anActionEvent.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        if (vFiles == null || vFiles.length == 0 || vFiles[0] == null) {
            // quick exit if no valid file selected
            presentation.setEnabledAndVisible(false);
            return;
        } else if (vFiles.length > 1) {
            // only supporting one file for now, so disable the action
            presentation.setEnabled(false);

            // however we do want to leave a breadcrumb if any of the files selected individually are valid
            final GitRepositoryManager manager = GitUtil.getRepositoryManager(project);
            for (VirtualFile vFile : vFiles) {
                final GitRepository repository = manager.getRepositoryForFile(vFile);
                if (repository != null && TfGitHelper.isTfGitRepository(repository)) {
                    // show the action if any of the files are TF
                    presentation.setVisible(true);
                    return;
                }
            }

            // no valid selection, hide the action.
            presentation.setVisible(false);
            return;
        }

        final VirtualFile vFile = vFiles[0];

        final GitRepositoryManager manager = GitUtil.getRepositoryManager(project);

        final GitRepository repository = manager.getRepositoryForFile(vFile);

        if (repository == null || !TfGitHelper.isTfGitRepository(repository)) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        final ChangeListManager changeListManager = ChangeListManager.getInstance(project);

        // ignored via .gitignore
        if (changeListManager.isIgnoredFile(vFile)) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        // OK show the action
        presentation.setVisible(true);

        // Now check if should be enabled
        presentation.setEnabled(isEnabled(changeListManager, project, vFile));
    }

    /**
     * Returns true if the action should be enabled, false otherwise
     *
     * @param project, vFile
     * @return
     */
    private boolean isEnabled(final ChangeListManager changeListManager, final Project project, final VirtualFile vFile) {
        if (vFile.isDirectory()) {
            /* Empty directories are not yet supported by Git.  Recursive scanning works, but could be error prone
               (e.g.) symbolic links.  Approach here is to always show it. */
            // TODO we may want to revisit this approach
            return true;
        }

        final GitVcs vcs = GitVcs.getInstance(project);

        if (!ProjectLevelVcsManager.getInstance(project).checkAllFilesAreUnder(vcs, new VirtualFile[]{vFile})) {
            return false;
        }

        if (changeListManager.isUnversioned(vFile)) {
            return false;
        }

        final Change change = changeListManager.getChange(vFile);
        if (change != null && change.getType() == Change.Type.NEW) {
            // a new file that has not yet been checked in
            return false;
        }

        return true;
    }

    @Override
    public void doActionPerformed(final AnActionEvent anActionEvent) {
        final Project project = anActionEvent.getRequiredData(CommonDataKeys.PROJECT);
        final VirtualFile virtualFile = anActionEvent.getRequiredData(CommonDataKeys.VIRTUAL_FILE);

        final GitRepositoryManager manager = GitUtil.getRepositoryManager(project);
        final GitRepository gitRepository = manager.getRepositoryForFile(virtualFile);
        final GitRemote gitRemote = TfGitHelper.getTfGitRemote(gitRepository);

        // guard for null so findbugs doesn't complain
        if (gitRemote == null || gitRepository == null || gitRepository.getRoot() == null) {
            return;
        }

        final StringBuilder stringBuilder = new StringBuilder(gitRemote.getFirstUrl());

        stringBuilder.append("#path=");
        final String rootPath = gitRepository.getRoot().getPath();
        final String path = virtualFile.getPath();
        final String relativePath = path.substring(rootPath.length());
        stringBuilder.append(encodeVirtualFilePath(relativePath));

        final GitLocalBranch gitLocalBranch = gitRepository.getCurrentBranch();
        if (gitLocalBranch != null) {
            final GitRemoteBranch gitRemoteBranch = gitLocalBranch.findTrackedBranch(gitRepository);
            if (gitRemoteBranch != null) {
                stringBuilder.append("&version=GB");
                stringBuilder.append(gitRemoteBranch.getNameForRemoteOperations());
            }
        }

        final String urlToBrowseTo = stringBuilder.toString();
        if (UrlHelper.isValidServerUrl(urlToBrowseTo)) {
            logger.info("Browsing to url " + urlToBrowseTo);
            BrowserUtil.browse(urlToBrowseTo);
        } else {
            logger.warn("Invalid server url to browse to " + urlToBrowseTo);
        }
    }

    /**
     * Returns a UTF-8 encoded version of the specified virtual file path.
     */
    private String encodeVirtualFilePath(final String virtualFilePath) {
        final String UTF_8 = "UTF-8";

        // ensures we are dealing with '/' separators (they should be used already)
        final String path = FileUtil.toSystemIndependentName(virtualFilePath);
        try {
            return URLEncoder.encode(path, UTF_8);
        } catch (UnsupportedEncodingException e) {
            // eat it; UTF-8 is a required charset and is always present
            logger.warn(virtualFilePath, e);
        }
        return path;
    }

}
