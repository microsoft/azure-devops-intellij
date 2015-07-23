package com.microsoft.tf.idea.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.microsoft.tf.idea.resources.Icons;
import com.microsoft.tf.idea.resources.TfPluginBundle;

/**
 * Created by madhurig on 7/18/2015.
 */
public class OpenInBrowserAction extends AbstractVSOOpenInBrowserAction {

    protected OpenInBrowserAction() {
        super(TfPluginBundle.OpenInBrowser, TfPluginBundle.OpenInBrowserMsg, Icons.VSLogo);
    }

}
