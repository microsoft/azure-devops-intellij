// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.external.ToolRunner;
import org.junit.Assert;
import org.junit.Test;

public class GetLocalPathCommandTest extends AbstractCommandTest {
    @Test
    public void testConstructor_nullContext() {
        final GetLocalPathCommand cmd = new GetLocalPathCommand(null, "$/path/server/file.txt", "ws1");
    }

    @Test
    public void testConstructor_withContext() {
        final GetLocalPathCommand cmd = new GetLocalPathCommand(context, "$/path/server/file.txt", "ws1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullArgs() {
        final GetLocalPathCommand cmd = new GetLocalPathCommand(null, null, null);
    }

    @Test
    public void testGetArgumentBuilder_withContext() {
        final GetLocalPathCommand cmd = new GetLocalPathCommand(context, "$/path/server/file.txt", "ws1");
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("resolvePath -noprompt -collection:http://server:8080/tfs/defaultcollection ******** $/path/server/file.txt -workspace:ws1", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_nullContext() {
        final GetLocalPathCommand cmd = new GetLocalPathCommand(null, "$/path/server/file.txt", "ws1");
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("resolvePath -noprompt $/path/server/file.txt -workspace:ws1", builder.toString());
    }

    @Test
    public void testParseOutput_noOutput() {
        final GetLocalPathCommand cmd = new GetLocalPathCommand(null, "$/path/server/file.txt", "ws1");
        final String message = cmd.parseOutput("", "");
        Assert.assertEquals("", message);
    }

    @Test
    public void testParseOutput_noErrors() {
        final GetLocalPathCommand cmd = new GetLocalPathCommand(null, "$/path/server/file.txt", "ws1");
        final String output = "/path/local/file.txt";
        final String message = cmd.parseOutput(output, "");
        Assert.assertEquals(output, message);
    }

    @Test(expected = RuntimeException.class)
    public void testParseOutput_errors() {
        final GetLocalPathCommand cmd = new GetLocalPathCommand(null, "$/path/server/file.txt", "ws1");
        final String message = cmd.parseOutput("/path/path", "error");
    }
}
