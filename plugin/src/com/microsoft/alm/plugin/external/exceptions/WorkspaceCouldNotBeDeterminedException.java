// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.exceptions;

/**
 * Exception for a case when a workspace could not be determined from any argument paths or the current working directory.
 */
public class WorkspaceCouldNotBeDeterminedException extends ToolException {
    public WorkspaceCouldNotBeDeterminedException() {
        super(ToolException.KEY_TF_WORKSPACE_COULD_NOT_BE_DETERMINED);
    }
}