package com.microsoft.tf.idea.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.vcs.log.VcsFullCommitDetails;
import com.intellij.vcs.log.VcsLog;
import com.intellij.vcs.log.VcsLogDataKeys;
import com.microsoft.tf.idea.utils.TFGitUtil;
import com.microsoft.tf.common.utils.UrlHelper;
import com.microsoft.tf.idea.resources.Icons;
import com.microsoft.tf.idea.resources.TfPluginBundle;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by madhurig on 7/18/2015.
 */
public class VcsLogActions extends AbstractVSOOpenInBrowserAction {

    public VcsLogActions() {
        super(TfPluginBundle.OpenInBrowser, TfPluginBundle.OpenInBrowserMsg, Icons.VSLogo);
    }
    @Override
    public void update(@NotNull AnActionEvent e) {
        final Project project = e.getData(CommonDataKeys.PROJECT);
        final VcsLog log = e.getData(VcsLogDataKeys.VCS_LOG);
        if(project == null || log == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        final List<VcsFullCommitDetails> commits = log.getSelectedDetails();
        if(commits.size() != 1) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        final GitRepository repository = GitUtil.getRepositoryManager(project).getRepositoryForRoot(commits.get(0).getRoot());
        e.getPresentation().setEnabledAndVisible(repository != null); //TODO: also check if repo is on VSO/TFS
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getRequiredData(CommonDataKeys.PROJECT);
        final VcsFullCommitDetails commit = e.getRequiredData(VcsLogDataKeys.VCS_LOG).getSelectedDetails().get(0);
        final GitRepository gitRepository = GitUtil.getRepositoryManager(project).getRepositoryForRoot(commit.getRoot());

        final String remoteUrl = TFGitUtil.getFirstRemoteUrl(gitRepository);
        if(remoteUrl != null) {
            StringBuilder stringBuilder = new StringBuilder(remoteUrl);
            stringBuilder.append("/commit/");
            stringBuilder.append(commit.getId().asString());
            final String urlToBrowseTo = stringBuilder.toString();
            if(UrlHelper.isValidServerUrl(urlToBrowseTo)) {
                BrowserUtil.browse(urlToBrowseTo);
            }
        }
    }}
