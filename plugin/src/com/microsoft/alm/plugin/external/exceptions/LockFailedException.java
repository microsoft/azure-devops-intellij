// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.exceptions;

public class LockFailedException extends ToolException {
    public LockFailedException() {
        super(ToolException.KEY_TF_LOCK_FAILED);
    }
}
