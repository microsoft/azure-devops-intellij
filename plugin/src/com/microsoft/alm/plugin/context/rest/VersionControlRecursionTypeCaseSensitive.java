// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context.rest;

/**
 * VersionControlRecursionType used from the 0.4.3 generated client is incorrect due to case sensitivity. These are the
 * correct values for the APIs to receive
 */
public enum VersionControlRecursionTypeCaseSensitive {

    NONE(0),
    ONE_LEVEL(1),
    FULL(120),;

    private int value;

    private VersionControlRecursionTypeCaseSensitive(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        final String name = super.toString();

        if (name.equals("NONE")) {
            return "None";
        }

        if (name.equals("ONE_LEVEL")) {
            return "OneLevel";
        }

        if (name.equals("FULL")) {
            return "Full";
        }
        return null;
    }
}
