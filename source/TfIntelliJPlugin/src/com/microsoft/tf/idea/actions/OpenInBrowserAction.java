package com.microsoft.tf.idea.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.tf.common.utils.TFGitUtil;
import com.microsoft.tf.common.utils.UrlHelper;
import com.microsoft.tf.idea.resources.Icons;
import com.microsoft.tf.idea.resources.TfPluginBundle;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;

/**
 * Created by madhurig on 7/18/2015.
 */
public class OpenInBrowserAction extends AbstractVSOOpenInBrowserAction {

    protected OpenInBrowserAction() {
        super(TfPluginBundle.OpenInBrowser, TfPluginBundle.OpenInBrowserMsg, Icons.VSLogo);
    }

    @Override
    public void actionPerformed(final AnActionEvent e) {
        final Project project = e.getData(CommonDataKeys.PROJECT);
        final VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (virtualFile == null || project == null || project.isDisposed()) {
            return;
        }

        final GitRepositoryManager manager = GitUtil.getRepositoryManager(project);
        final GitRepository gitRepository = manager.getRepositoryForFile(virtualFile);

        final String remoteUrl = TFGitUtil.getFirstRemoteUrl(gitRepository);
        if(remoteUrl != null) {
            final String rootPath = gitRepository.getRoot().getPath();
            final String path = virtualFile.getPath();
            final String relativePath = path.substring(rootPath.length());

            final StringBuilder stringBuilder = new StringBuilder(remoteUrl);
            stringBuilder.append("/#path=");
            stringBuilder.append(relativePath);
            final String urlToBrowseTo = stringBuilder.toString();
            if(UrlHelper.isValidServerUrl(urlToBrowseTo)) {
                BrowserUtil.browse(urlToBrowseTo);
            }
        }
    }

}
