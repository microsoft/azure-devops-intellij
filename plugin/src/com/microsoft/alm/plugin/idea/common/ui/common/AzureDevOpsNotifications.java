// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.common;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import git4idea.remote.GitConfigureRemotesAction;
import org.jetbrains.annotations.NotNull;

public class AzureDevOpsNotifications {
    private static final NotificationGroup AZURE_DEVOPS_NOTIFICATIONS = new NotificationGroup(
            TfPluginBundle.message(TfPluginBundle.KEY_PLUGIN_AZURE_DEVOPS),
            NotificationDisplayType.BALLOON,
            true);

    public static void showManageRemoteUrlsNotification(@NotNull Project project, @NotNull String hostName) {
        Notification notification = AZURE_DEVOPS_NOTIFICATIONS.createNotification(
                TfPluginBundle.message(TfPluginBundle.KEY_GIT_NOTIFICATION_REMOTE, hostName),
                MessageType.ERROR);
        GitConfigureRemotesAction gitConfigureRemotesAction = new GitConfigureRemotesAction();
        gitConfigureRemotesAction.getTemplatePresentation().setText(
                TfPluginBundle.message(TfPluginBundle.KEY_GIT_CONFIGURE_REMOTES));
        notification.addAction(gitConfigureRemotesAction);
        Notifications.Bus.notify(notification, project);
    }
}
