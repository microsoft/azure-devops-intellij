// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.Messages;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.telemetry.TfsTelemetryHelper;
import git4idea.GitVcs;
import git4idea.config.GitExecutableValidator;

import javax.swing.Icon;

/**
 * Represents an base class for all Actions performed by the Team Foundation plugin for IntelliJ.
 *
 * @see javax.swing.Action
 */
public abstract class InstrumentedAction extends DumbAwareAction {
    private final boolean actionUsesGitExe;

    /**
     * Default constructor
     */
    protected InstrumentedAction() {
        actionUsesGitExe = true;
    }

    /**
     * Use this constructor to set the text of the action that is presented to the user
     */
    protected InstrumentedAction(final String text) {
        super(text);
        actionUsesGitExe = true;
    }

    /**
     * Use this constructor to set the text, description,
     * and icon of the action that is presented to the user.
     */
    protected InstrumentedAction(final String text, final String description, final Icon icon) {
        super(text, description, icon);
        actionUsesGitExe = true;
    }

    protected InstrumentedAction(final String text, final String description, final Icon icon, final boolean usesGitExe) {
        super(text, description, icon);
        actionUsesGitExe = usesGitExe;
    }

    /**
     * Override the getActionName method to return the logical (non-localized) name for this action.
     * The default will be the class name.
     */
    protected String getActionName() {
        return this.getClass().getName();
    }

    /**
     * Override the getContextProperties method to add any name value pairs to the
     * context of this action. These name value pairs will be added to the Telemetry
     * data for invocation of the action.
     */
    protected TfsTelemetryHelper.PropertyMapBuilder getContextProperties() {
        final String actionName = getActionName();
        return new TfsTelemetryHelper.PropertyMapBuilder()
                .actionName(actionName);
    }

    /**
     * Override this method to handle the update call.
     */
    public abstract void doUpdate(final AnActionEvent anActionEvent);

    /**
     * Override this method to handle the actionPerformed call.
     */
    public abstract void doActionPerformed(final AnActionEvent anActionEvent);

    /**
     * This inherited method has been finalized to make sure that no subclasses can
     * interfere with the Telemetry gathering.
     */
    @Override
    public final void update(final AnActionEvent anActionEvent) {
        try {
            // For now we don't want to gather telemetry on every update method call
            doUpdate(anActionEvent);
        } catch (Exception ex) {
            // An unhandled exception leaked all the way out to here
            // Let's log the exception and stop letting it bubble up
            TfsTelemetryHelper.getInstance().sendException(ex, getContextProperties().build());
        }
    }

    /**
     * This inherited method has been finalized to make sure that no subclasses can
     * interfere with the Telemetry gathering.
     */
    @Override
    public final void actionPerformed(final AnActionEvent anActionEvent) {
        if(actionUsesGitExe) {
            final GitExecutableValidator validator = GitVcs.getInstance(anActionEvent.getProject()).getExecutableValidator();
            if (!validator.checkExecutableAndNotifyIfNeeded()) {
                //action requires Git.exe but it is not set correctly, show message and return without doing any action
                Messages.showWarningDialog(anActionEvent.getProject(),
                        TfPluginBundle.message(TfPluginBundle.KEY_GIT_NOT_CONFIGURED),
                        TfPluginBundle.message(TfPluginBundle.KEY_TF_GIT));
                return;
            }
        }

        try {
            SendStartActionEvent();
            doActionPerformed(anActionEvent);
        } catch (Exception ex) {
            // An unhandled exception leaked all the way out to here
            // Let's log the exception and stop letting it bubble up
            TfsTelemetryHelper.getInstance().sendException(ex, getContextProperties().build());
        }
    }

    private void SendStartActionEvent() {
        final String eventName = getActionName();
        TfsTelemetryHelper.getInstance().sendEvent(eventName, getContextProperties().build());
    }


}
