// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication;

import com.microsoft.alm.plugin.TeamServicesException;

public class ProfileDoesNotExistException extends TeamServicesException {
    // The constructor has to match what the Exception mapper expects (we don't use the message param)
    public ProfileDoesNotExistException(final String message, final Exception innerException) {
        super(TeamServicesException.KEY_VSO_NO_PROFILE_ERROR, innerException);
    }
}
