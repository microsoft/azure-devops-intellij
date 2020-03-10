// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.exceptions;

public class NoPendingChangesFoundException extends ToolException {

    public NoPendingChangesFoundException() {
        super(KEY_TF_NO_PENDING_CHANGES_FOUND);
    }
}
