// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.services;

import com.microsoft.alm.plugin.TeamServicesException;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.services.LocalizationService;
import org.apache.commons.lang.StringUtils;

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
     * Gets the localized exception message
     *
     * @param t
     * @return localized string
     */
    public String getExceptionMessage(final Throwable t) {
        if (t instanceof TeamServicesException) {
            final String key = ((TeamServicesException) t).getMessageKey();
            if (keysMap.containsKey(key)) {
                return getLocalizedMessage(keysMap.get(key));
            }
        }

        String message = t.getLocalizedMessage();
        if (StringUtils.isEmpty(message)) {
            if (t.getCause() != null && !StringUtils.isEmpty(t.getCause().getLocalizedMessage())) {
                message = t.getCause().getLocalizedMessage();
            } else {
                message = t.toString();
            }
        }
        return message;
    }

    private static final Map<String, String> keysMap = new HashMap<String, String>() {
        {
            put(TeamServicesException.KEY_TFS_UNSUPPORTED_VERSION, "TFS.UnsupportedVersion");
            put(TeamServicesException.KEY_VSO_AUTH_SESSION_EXPIRED, "VSO.Auth.SessionExpired");
            put(TeamServicesException.KEY_VSO_AUTH_FAILED, "VSO.Auth.Failed");
            put(TeamServicesException.KEY_TFS_AUTH_FAILED, "TFS.Auth.Failed");
        }
    };

}
