package com.microsoft.vso.idea.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.microsoft.vso.idea.Icons;
import git4idea.repo.GitRepository;

/**
 * Created by madhurig on 7/18/2015.
 */
public class VSOOpenInBrowserAction extends DumbAwareAction {

    protected VSOOpenInBrowserAction() {
        super("Open in Browser", "Open corresponding link in external browser", Icons.VSLogo);
    }



    @Override
    public void actionPerformed(final AnActionEvent e) {
        //TODO: Not implemented - tf git, show item history in browser
    }
}
