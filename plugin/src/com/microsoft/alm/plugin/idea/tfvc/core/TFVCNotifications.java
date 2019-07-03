package com.microsoft.alm.plugin.idea.tfvc.core;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.tfvc.actions.AddFileToTfIgnoreAction;
import org.jetbrains.annotations.NotNull;

public class TFVCNotifications {
    private static final NotificationGroup TFS_NOTIFICATIONS = new NotificationGroup(
            TfPluginBundle.message(TfPluginBundle.KEY_TFVC_NOTIFICATIONS),
            NotificationDisplayType.BALLOON,
            true
    );

    public static void showInvalidDollarFilePathNotification(@NotNull Project project, @NotNull String serverFilePath) {
        Notification notification = TFS_NOTIFICATIONS.createNotification(
                TfPluginBundle.message(TfPluginBundle.KEY_TFVC_NOTIFICATION_FILE_NAME_STARTS_WITH_DOLLAR, serverFilePath),
                NotificationType.WARNING);
        notification.addAction(new AddFileToTfIgnoreAction(project, serverFilePath));
        Notifications.Bus.notify(notification);
    }
}
