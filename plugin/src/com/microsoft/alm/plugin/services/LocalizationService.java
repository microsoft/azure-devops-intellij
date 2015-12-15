// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.services;

public interface LocalizationService {

    String getLocalizedMessage(final String key, Object... params);

    class ExceptionMessageKeys {
        public static String KEY_TFS_UNSUPPORTED_VERSION = "KEY_TFS_UNSUPPORTED_VERSION";
        public static String KEY_VSO_AUTH_SESSION_EXPIRED = "KEY_VSO_AUTH_SESSION_EXPIRED";
        public static String KEY_VSO_AUTH_FAILED = "KEY_VSO_AUTH_FAILED";
        public static String KEY_TFS_AUTH_FAILED = "KEY_TFS_AUTH_FAILED";
    }
}
