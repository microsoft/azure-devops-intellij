// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication;

/**
 * Types of authentication methods
 */
public enum AuthTypes {
    CREDS,
    DEVICE_FLOW;

    public static AuthTypes getEnum(final String name) {
        if (DEVICE_FLOW.name().equalsIgnoreCase(name)) {
            return DEVICE_FLOW;
        }
        // default to Creds if we aren't sure
        return CREDS;
    }
}
