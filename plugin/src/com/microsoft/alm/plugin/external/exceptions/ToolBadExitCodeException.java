// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.exceptions;

public class ToolBadExitCodeException extends ToolException {
    private final int exitCode;

    public ToolBadExitCodeException(int exitCode) {
        super(ToolException.KEY_TF_BAD_EXIT_CODE);
        this.exitCode = exitCode;
    }

    public ToolBadExitCodeException(int exitCode, Throwable t) {
        super(ToolException.KEY_TF_BAD_EXIT_CODE, t);
        this.exitCode = exitCode;
    }

    @Override
    public String[] getMessageParameters() {
        return new String[] {Integer.toString(exitCode)};
    }
}
