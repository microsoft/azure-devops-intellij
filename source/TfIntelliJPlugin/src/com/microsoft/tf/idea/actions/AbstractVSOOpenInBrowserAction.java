package com.microsoft.tf.idea.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Created by jasholl on 7/21/2015.
 */
abstract public class AbstractVSOOpenInBrowserAction extends DumbAwareAction{

    protected AbstractVSOOpenInBrowserAction() {
    }

    protected AbstractVSOOpenInBrowserAction(@Nullable String text) {
        super(text);
    }

    protected AbstractVSOOpenInBrowserAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    protected void setVisibleEnabled(AnActionEvent e, boolean visible, boolean enabled) {
        e.getPresentation().setVisible(visible);
        e.getPresentation().setEnabled(enabled);
    }

    @Override
    public void update(final AnActionEvent e) {
        Project project = e.getData(CommonDataKeys.PROJECT);
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (project == null || project.isDefault() || virtualFile == null) {
            setVisibleEnabled(e, false, false);
            return;
        }
        GitRepositoryManager manager = GitUtil.getRepositoryManager(project);

        final GitRepository gitRepository = manager.getRepositoryForFile(virtualFile);
        if (gitRepository == null) {
            setVisibleEnabled(e, false, false);
            return;
        }

        if (!isVSORepo(gitRepository)) {
            setVisibleEnabled(e, false, false);
            return;
        }

        ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        if (changeListManager.isUnversioned(virtualFile)) {
            setVisibleEnabled(e, true, false);
            return;
        }

        Change change = changeListManager.getChange(virtualFile);
        if (change != null && change.getType() == Change.Type.NEW) {
            setVisibleEnabled(e, true, false);
            return;
        }

        setVisibleEnabled(e, true, true);
    }

    @Override
    public void actionPerformed(final AnActionEvent e) {
        final Project project = e.getData(CommonDataKeys.PROJECT);
        final VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (virtualFile == null || project == null || project.isDisposed()) {
            return;
        }

        GitRepositoryManager manager = GitUtil.getRepositoryManager(project);
        final GitRepository repo = manager.getRepositoryForFile(virtualFile);
        String urlToOpen = getVSOUrl(repo);

        final String rootPath = repo.getRoot().getPath();
        final String path = virtualFile.getPath();
        String relativePath = path.substring(rootPath.length());
        urlToOpen =  urlToOpen + "/#path=" + relativePath;
        if (urlToOpen != null) {
            BrowserUtil.browse(urlToOpen);
        }
    }

    protected String getVSOUrl(GitRepository repo) {
        String url = "https://www.visualstudio.com/";
        for (GitRemote gitRemote : repo.getRemotes()) {
            for (String remoteUrl : gitRemote.getUrls()) {
                url = remoteUrl;
            }
        }
        return url;
    }

    private boolean isVSORepo(GitRepository repo) {
        for (GitRemote gitRemote : repo.getRemotes()) {
            for (String remoteUrl : gitRemote.getUrls()) {
                if(remoteUrl.contains("visualstudio.com") || remoteUrl.contains("tfsallin.net")) {
                    return true; //TODO: how to detect VSO URLs for onprem
                }
            }
        }
        return false;
    }

}
