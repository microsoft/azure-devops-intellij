// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.exceptions;

public class SyncException extends Exception {
    private final boolean isWarning;

    public SyncException(final String message) {
        this(message, false, null);
    }

    public SyncException(final String message, final boolean isWarning) {
        this(message, isWarning, null);
    }

    public SyncException(final String message, final boolean isWarning, final Throwable causedBy) {
        super(message, causedBy);
        this.isWarning = isWarning;
    }

    public boolean isWarning() {
        return isWarning;
    }
}
