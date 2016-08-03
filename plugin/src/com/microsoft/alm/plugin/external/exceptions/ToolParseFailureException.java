// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.exceptions;

public class ToolParseFailureException extends ToolException {

    public ToolParseFailureException() {
        super(ToolException.KEY_TF_PARSE_FAILURE);
    }

    public ToolParseFailureException(Throwable t) {
        super(ToolException.KEY_TF_PARSE_FAILURE, t);
    }
}
