// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.services;

import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.services.LocalizationService;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides an implementation for string localization in IntelliJ
 */
public class LocalizationServiceImpl implements LocalizationService {

    private static class Holder {
        private static LocalizationServiceImpl INSTANCE = new LocalizationServiceImpl();
    }

    public static LocalizationServiceImpl getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * This constructor is marked protected for testing
     */
    protected LocalizationServiceImpl() {

    }

    public String getLocalizedMessage(final String key, Object... params) {
        return TfPluginBundle.message(key, params);
    }

    /**
     * Maps a exception message to a key and returns the localized string if key is known, message can't have arguments
     *
     * @param message
     * @return localized message
     */
    public String getServerExceptionMessage(final String message) {
        if (keysMap.containsKey(message)) {
            return getLocalizedMessage(keysMap.get(message));
        } else {
            return message;
        }
    }

    private static final Map<String, String> keysMap = new HashMap<String, String>() {
        {
            put(ExceptionMessageKeys.KEY_TFS_UNSUPPORTED_VERSION, "TFS.UnsupportedVersion");
            put(ExceptionMessageKeys.KEY_VSO_AUTH_SESSION_EXPIRED, "VSO.Auth.SessionExpired");
            put(ExceptionMessageKeys.KEY_VSO_AUTH_FAILED, "VSO.Auth.Failed");
            put(ExceptionMessageKeys.KEY_TFS_AUTH_FAILED, "TFS.Auth.Failed");
        }
    };

}
