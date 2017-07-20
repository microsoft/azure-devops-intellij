// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.exceptions;

public class ToolAuthenticationException extends ToolException {
    public ToolAuthenticationException() {
        super(ToolException.KEY_TF_AUTH_FAIL);
    }
}