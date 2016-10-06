// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.exceptions;

/**
 * Exception for when a workspace i trying to be created but another workspace already exists with that name
 */
public class WorkspaceAlreadyExistsException extends ToolException {
    final String workspaceName;

    public WorkspaceAlreadyExistsException(final String workspaceName) {
        super(ToolException.KEY_TF_WORKSPACE_EXISTS);
        this.workspaceName = workspaceName;
    }

    @Override
    public String[] getMessageParameters() {
        return new String[]{workspaceName};
    }
}