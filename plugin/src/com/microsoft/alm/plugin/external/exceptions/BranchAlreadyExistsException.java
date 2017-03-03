// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.exceptions;

/**
 * Exception for when a branch is trying to be created but already exists
 */
public class BranchAlreadyExistsException extends ToolException {
    final String branchName;

    public BranchAlreadyExistsException(final String branchName) {
        super(ToolException.KEY_TF_BRANCH_EXISTS);
        this.branchName = branchName;
    }

    @Override
    public String[] getMessageParameters() {
        return new String[]{branchName};
    }
}