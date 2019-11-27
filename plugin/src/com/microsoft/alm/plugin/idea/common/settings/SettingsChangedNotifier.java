// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.settings;

import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

public interface SettingsChangedNotifier {
    Topic<SettingsChangedNotifier> SETTINGS_CHANGED_TOPIC = Topic.create(
            "TFS Settings Changed",
            SettingsChangedNotifier.class);

    void afterSettingsChanged(@NotNull String propertyKey);
}
