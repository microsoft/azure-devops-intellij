// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.utils;

import org.junit.Assert;
import org.junit.Test;

public class VcsHelperTest {
    @Test
    public void testGetTeamProjectFromTfvcServerPath() {
        Assert.assertEquals("", VcsHelper.getTeamProjectFromTfvcServerPath(null));
        Assert.assertEquals("", VcsHelper.getTeamProjectFromTfvcServerPath(""));
        Assert.assertEquals("", VcsHelper.getTeamProjectFromTfvcServerPath("$/"));
        Assert.assertEquals("", VcsHelper.getTeamProjectFromTfvcServerPath("$"));
        Assert.assertEquals("", VcsHelper.getTeamProjectFromTfvcServerPath("/"));
        Assert.assertEquals("proj", VcsHelper.getTeamProjectFromTfvcServerPath("$/proj"));
        Assert.assertEquals("proj", VcsHelper.getTeamProjectFromTfvcServerPath("$/proj/"));
        Assert.assertEquals("proj", VcsHelper.getTeamProjectFromTfvcServerPath("$/proj/one"));
        Assert.assertEquals("proj", VcsHelper.getTeamProjectFromTfvcServerPath("$/proj/one/"));
    }
}
