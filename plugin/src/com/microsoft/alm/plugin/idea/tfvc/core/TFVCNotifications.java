// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.tfvc.actions.AddFileToTfIgnoreAction;
import com.microsoft.alm.plugin.idea.tfvc.ui.settings.EULADialog;
import org.jetbrains.annotations.NotNull;

public class TFVCNotifications {
    private static final NotificationGroup TFS_NOTIFICATIONS = new NotificationGroup(
            TfPluginBundle.message(TfPluginBundle.KEY_TFVC_NOTIFICATIONS),
            NotificationDisplayType.BALLOON,
            true
    );

    private static MergingUpdateQueue ourQueue = new MergingUpdateQueue(
            "TFVCNotifications.myQueue",
            2000,
            false,
            null,
            null,
            null,
            true);

    public static void showInvalidDollarFilePathNotification(@NotNull Project project, @NotNull String serverFilePath) {
        ourQueue.queue(new Update(Pair.create(project, serverFilePath)) {
            @Override
            public void run() {
                ApplicationManager.getApplication().assertIsDispatchThread();

                Notification notification = TFS_NOTIFICATIONS.createNotification(
                        TfPluginBundle.message(TfPluginBundle.KEY_TFVC_NOTIFICATION_FILE_NAME_STARTS_WITH_DOLLAR, serverFilePath),
                        NotificationType.WARNING);
                notification.addAction(new AddFileToTfIgnoreAction(project, serverFilePath));
                Notifications.Bus.notify(notification, project);
            }
        });
        UIUtil.invokeLaterIfNeeded(ourQueue::activate);
    }

    public static Notification createSdkEulaNotification() {
        return TFS_NOTIFICATIONS.createNotification(
                TfPluginBundle.message(TfPluginBundle.KEY_TFVC_EULA_ACCEPTANCE_IS_REQUIRED),
                NotificationType.WARNING
        ).addAction(new EULADialog.ShowTfvcSdkEulaAction());
    }

    public static Notification createCommandLineClientEulaNotification() {
        return TFS_NOTIFICATIONS.createNotification(
                TfPluginBundle.message(TfPluginBundle.KEY_TFVC_EULA_ACCEPTANCE_IS_REQUIRED),
                NotificationType.WARNING
        ).addAction(new EULADialog.ShowTfvcCommandLineClientEulaAction());
    }

    public static class ShowSdkEulaNotification extends AnAction {
        public ShowSdkEulaNotification() {
            super("[Internal] Show SDK License Agreement Notification");
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            createSdkEulaNotification().notify(e.getProject());
        }
    }

    public static class ShowCommandLineClientEulaNotification extends AnAction {
        public ShowCommandLineClientEulaNotification() {
            super("[Internal] Show TF CLC EULA Notification");
        }
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            createCommandLineClientEulaNotification().notify(e.getProject());
        }
    }
}
