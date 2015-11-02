// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.resources.Icons;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.pullrequest.CreatePullRequestController;
import com.microsoft.alm.plugin.idea.utils.TfGitHelper;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;

import java.util.List;

/**
 * This class adds a "Create Pull Request" menu item to git menu.
 */
public class CreatePullRequestAction extends InstrumentedAction {

    public CreatePullRequestAction() {
        super(TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_CREATE_PULL_REQUEST),
                TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_CREATE_PULL_REQUEST_MSG),
                Icons.VSLogoSmall);
    }

    @Override
    public void doUpdate(AnActionEvent anActionEvent) {
        final Project project = anActionEvent.getData(CommonDataKeys.PROJECT);
        final GitRepository gitRepository = getTfGitRepository(project);

        if (project == null || project.isDefault() || gitRepository == null) {
            anActionEvent.getPresentation().setVisible(false);
            anActionEvent.getPresentation().setEnabled(false);
        } else {
            anActionEvent.getPresentation().setVisible(true);
            anActionEvent.getPresentation().setEnabled(true);
        }
    }

    @Override
    public void doActionPerformed(AnActionEvent anActionEvent) {
        final Project project = anActionEvent.getData(CommonDataKeys.PROJECT);
        final GitRepository gitRepository = getTfGitRepository(project);

        if (gitRepository != null) {
            CreatePullRequestController createPullRequestController
                    = new CreatePullRequestController(project, gitRepository);
            createPullRequestController.showModalDialog();
        }
    }

    private GitRepository getTfGitRepository(final Project project) {
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

}
