// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.operations;

import com.microsoft.alm.plugin.context.RepositoryContext;

/**
 * Other modules have to use this class to create Operation instances.
 * This allows tests in other modules to easily mock operations even if they are completely
 * encapsulated within other classes.
 */
public class OperationFactory {
    //TODO add other operation types
    public static BuildStatusLookupOperation createBuildStatusLookupOperation(
            final RepositoryContext repositoryContext, final boolean forcePrompt) {
        return new BuildStatusLookupOperation(repositoryContext, forcePrompt);
    }

    public static AccountLookupOperation createAccountLookupOperation() {
        return new AccountLookupOperation();
    }
}
