// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.models.VersionSpec;
import org.junit.Assert;
import org.junit.Test;

public class GetBaseVersionCommandTest extends AbstractCommandTest {
    @Test
    public void testConstructor_nullContext() {
        final GetBaseVersionCommand cmd = new GetBaseVersionCommand(null, "/work/folder", "$/path/server/file.txt", "$/path/server/originalFile.txt");
    }

    @Test
    public void testConstructor_withContext() {
        final GetBaseVersionCommand cmd = new GetBaseVersionCommand(context, "/work/folder", "$/path/server/file.txt", "$/path/server/originalFile.txt");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullArgs() {
        final GetBaseVersionCommand cmd = new GetBaseVersionCommand(null, null, null, null);
    }

    @Test
    public void testGetArgumentBuilder_withContext() {
        final GetBaseVersionCommand cmd = new GetBaseVersionCommand(context, "/work/folder", "$/path/server/file.txt", "$/path/server/originalFile.txt");
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("merges -noprompt -collection:http://server:8080/tfs/defaultcollection ******** -format:brief $/path/server/file.txt $/path/server/originalFile.txt", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_nullContext() {
        final GetBaseVersionCommand cmd = new GetBaseVersionCommand(null, "/work/folder", "$/path/server/file.txt", "$/path/server/originalFile.txt");
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("merges -noprompt -format:brief $/path/server/file.txt $/path/server/originalFile.txt", builder.toString());
    }

    @Test
    public void testParseOutput_noOutput() {
        final GetBaseVersionCommand cmd = new GetBaseVersionCommand(null, "/work/folder", "$/path/server/file.txt", "$/path/server/originalFile.txt");
        final VersionSpec versionSpec = cmd.parseOutput("", "");
        Assert.assertNull(versionSpec);
    }

    @Test
    public void testParseOutput_noErrors() {
        final GetBaseVersionCommand cmd = new GetBaseVersionCommand(null, "/work/folder", "$/path/server/file.txt", "$/path/server/originalFile.txt");
        final String output = "Changeset Merged In Changeset Author         Date\n" +
                "--------- ------------------- -------------- ------------------------\n" +
                "8         208                 Jason Prickett Nov 30, 2016 11:53:58 AM\n" +
                "222       232                 Jason Prickett Dec 7, 2016 12:00:28 PM\n" +
                "231       232                 Jason Prickett Dec 7, 2016 12:00:28 PM\n" +
                "233       234                 Jason Prickett Dec 7, 2016 12:01:24 PM\n";
        final VersionSpec versionSpec = cmd.parseOutput(output, "");
        Assert.assertEquals("C233", versionSpec.toString());
    }

    @Test(expected = RuntimeException.class)
    public void testParseOutput_errors() {
        final GetBaseVersionCommand cmd = new GetBaseVersionCommand(null, "/work/folder", "$/path/server/file.txt", "$/path/server/originalFile.txt");
        final VersionSpec versionSpec = cmd.parseOutput("/path/path", "error");
    }
}
