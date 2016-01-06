// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.starters.checkout;

import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

public class ApplicationStarterBaseTest extends IdeaAbstractTest  {
    public final String VALID_GIT_URL = "https://account.visualstudio.com/DefaultCollection/_git/TestProject";
    public final String INVALID_GIT_URL = "https://account.visualstudio.com/TestProject";
    public ApplicationStarterBase starterBase = new ApplicationStarterBase() {
        @Override
        public String getUsageMessage() {
            return null;
        }

        @Override
        protected void processCommand(String[] args, @Nullable String currentDirectory) throws Exception {
        }
    };

    @Test
    public void testCheckArgumentsHappy() {
        String[] args = {starterBase.CHECKOUT_COMMAND, VALID_GIT_URL};
        Assert.assertTrue(starterBase.checkArguments(args));
    }

    @Test
    public void testCheckArgumentsBadUrl() {
        String[] args = {starterBase.CHECKOUT_COMMAND, INVALID_GIT_URL};
        Assert.assertFalse(starterBase.checkArguments(args));
    }

    @Test
    public void testCheckArgumentsIncorrectCommand() {
        String[] args = {"diff", VALID_GIT_URL};
        Assert.assertFalse(starterBase.checkArguments(args));
    }

    @Test
    public void testCheckArgumentsIncorrectArgsNumber() {
        String[] argsFew = {starterBase.CHECKOUT_COMMAND};
        Assert.assertFalse(starterBase.checkArguments(argsFew));

        String[] argsMany = {starterBase.CHECKOUT_COMMAND, VALID_GIT_URL, VALID_GIT_URL};
        Assert.assertFalse(starterBase.checkArguments(argsMany));
    }
}
