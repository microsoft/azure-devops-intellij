// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin;

public class TeamServicesException extends RuntimeException {
    private String messageKey;

    public String getMessageKey() {
        return this.messageKey;
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

}
