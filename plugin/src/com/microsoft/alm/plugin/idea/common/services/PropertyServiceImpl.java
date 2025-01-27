// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.services;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.microsoft.alm.plugin.idea.common.settings.SettingsChangedNotifier;
import com.microsoft.alm.plugin.idea.common.settings.TeamServicesSettingsService;
import com.microsoft.alm.plugin.services.PropertyService;

import java.util.Map;

public class PropertyServiceImpl implements PropertyService {
    private boolean restored = false;
    private Map<String, String> map;

    private static class Holder {
        private static final PropertyServiceImpl INSTANCE = new PropertyServiceImpl();
    }

    /**
     * This constructor is protected to allow for testing
     */
    protected PropertyServiceImpl() {
    }

    public static PropertyServiceImpl getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public synchronized String getProperty(final String propertyName) {
        ensureRestored();
        return map.get(propertyName);
    }

    @Override
    public synchronized void setProperty(final String propertyName, final String value) {
        ensureRestored();
        if (value == null) {
            removeProperty(propertyName);
        } else {
            map.put(propertyName, value);
        }

        Application application = ApplicationManager.getApplication();
        if (application == null) // can happen in the unit tests
            return;

        SettingsChangedNotifier settingsChangedNotifier = application.getMessageBus()
                .syncPublisher(SettingsChangedNotifier.SETTINGS_CHANGED_TOPIC);
        settingsChangedNotifier.afterSettingsChanged(propertyName);
    }

    @Override
    public synchronized void removeProperty(final String propertyName) {
        map.remove(propertyName);
    }

    public synchronized Map<String, String> getProperties() {
        if (map == null) {
            return null;
        }

        return Map.copyOf(map);
    }

    private void ensureRestored() {
        if (!restored) {
            restored = true;
            map = TeamServicesSettingsService.getInstance().restoreProperties();
        }
    }
}
