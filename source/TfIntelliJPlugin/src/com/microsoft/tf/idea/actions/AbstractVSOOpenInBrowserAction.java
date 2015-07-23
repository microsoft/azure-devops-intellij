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
import com.microsoft.tf.common.utils.TFGitUtil;
import git4idea.GitUtil;
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
        final Project project = e.getData(CommonDataKeys.PROJECT);
        final VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (project == null || project.isDefault() || virtualFile == null) {
            setVisibleEnabled(e, false, false);
            return;
        }
        final GitRepositoryManager manager = GitUtil.getRepositoryManager(project);

        final GitRepository gitRepository = manager.getRepositoryForFile(virtualFile);
        if (gitRepository == null) {
            setVisibleEnabled(e, false, false);
            return;
        }

        if (!TFGitUtil.isTFGitRepository(gitRepository)) {
            setVisibleEnabled(e, false, false);
            return;
        }

        final ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        if (changeListManager.isUnversioned(virtualFile)) {
            setVisibleEnabled(e, true, false);
            return;
        }

        final Change change = changeListManager.getChange(virtualFile);
        if (change != null && change.getType() == Change.Type.NEW) {
            setVisibleEnabled(e, true, false);
            return;
        }

        setVisibleEnabled(e, true, true);
    }

}
