// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.exceptions;

import com.intellij.openapi.vcs.VcsException;

public class MergeFailedException extends VcsException {
    public MergeFailedException(final String message, final boolean isWarning) {
        super(message, isWarning);
    }
}
