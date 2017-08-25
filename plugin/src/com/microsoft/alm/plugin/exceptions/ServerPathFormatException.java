// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.exceptions;

/**
 * Exception for invalid server paths
 */
public class ServerPathFormatException extends TeamServicesException {
    final String serverPath;

    public ServerPathFormatException(final String serverPath) {
        super(KEY_TFS_SERVER_PATH_INVALID);
        this.serverPath = serverPath;
    }

    @Override
    public String getMessageKey() {
        return KEY_TFS_SERVER_PATH_INVALID;
    }

    @Override
    public String[] getMessageParameters() {
        return new String[]{serverPath};
    }
}