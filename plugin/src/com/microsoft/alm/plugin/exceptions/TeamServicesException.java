// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.exceptions;


public class TeamServicesException extends RuntimeException implements LocalizedException {

    private String messageKey;

    @Override
    public String getMessageKey() {
        return this.messageKey;
    }

    @Override
    public String[] getMessageParameters() {
        return new String[0];
    }

    public TeamServicesException(final String key) {
        super(key);
        this.messageKey = key;
    }

    public TeamServicesException(final String key, final Throwable t) {
        super(key, t);
        this.messageKey = key;
    }

    //Keys for exception messages
    public static String KEY_TFS_UNSUPPORTED_VERSION = "KEY_TFS_UNSUPPORTED_VERSION";
    public static String KEY_VSO_AUTH_SESSION_EXPIRED = "KEY_VSO_AUTH_SESSION_EXPIRED";
    public static String KEY_VSO_AUTH_FAILED = "KEY_VSO_AUTH_FAILED";
    public static String KEY_TFS_AUTH_FAILED = "KEY_TFS_AUTH_FAILED";
    public static String KEY_OPERATION_ERRORS = "KEY_OPERATION_ERRORS";
    public static String KEY_VSO_NO_PROFILE_ERROR = "KEY_VSO_NO_PROFILE_ERROR";
    public static String KEY_TFS_MALFORMED_SERVER_URI = "KEY_TFS_MALFORMED_SERVER_URI";
    public static String KEY_ERROR_UNKNOWN = "KEY_ERROR_UNKNOWN";
}
