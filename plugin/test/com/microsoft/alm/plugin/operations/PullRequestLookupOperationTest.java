// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.operations;

import com.microsoft.alm.plugin.AbstractTest;
import org.junit.Assert;
import org.junit.Test;

public class PullRequestLookupOperationTest extends AbstractTest {

    PullRequestLookupOperation underTest;

    @Test
    public void tesConstructor() {
        try {
            underTest = new PullRequestLookupOperation(null);
            Assert.fail();
        } catch (AssertionError e) {
            //expected when ServerContext is null
        }

        //construct correctly
        underTest = new PullRequestLookupOperation("gitRepoUrl");
    }
}
