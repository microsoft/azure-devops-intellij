package com.microsoft.tf.idea.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.tf.idea.utils.TFGitHelper;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Created by jasholl on 7/21/2015.
 */
abstract public class AbstractVSOOpenInBrowserAction extends DumbAwareAction{

    protected AbstractVSOOpenInBrowserAction() {
    }

    protected AbstractVSOOpenInBrowserAction(@Nullable final String text) {
        super(text);
    }

    protected AbstractVSOOpenInBrowserAction(@Nullable final String text, @Nullable final String description, @Nullable final Icon icon) {
        super(text, description, icon);
    }

    protected void setVisibleEnabled(@NotNull final AnActionEvent e, final boolean visible, final boolean enabled) {
        e.getPresentation().setVisible(visible);
        e.getPresentation().setEnabled(enabled);
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
        //TODO -- need to refactor this because this is not FAST -- see doc in AnAction.update();
        final Project project = e.getData(CommonDataKeys.PROJECT);
        final VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (project == null || project.isDefault() || virtualFile == null) {
            setVisibleEnabled(e, false, false);
            return;
        }
        final GitRepositoryManager manager = GitUtil.getRepositoryManager(project);

        final GitRepository gitRepository = manager.getRepositoryForFile(virtualFile);
        if (gitRepository == null || !TFGitHelper.isTFGitRepository(gitRepository)) {
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
