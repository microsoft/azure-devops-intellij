// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.models.ToolVersion;
import org.junit.Assert;
import org.junit.Test;

public class TfVersionCommandTest extends AbstractCommandTest {
    @Test
    public void testConstructor() {
        final TfVersionCommand cmd = new TfVersionCommand();
    }

    @Test
    public void testGetArgumentBuilder() {
        final TfVersionCommand cmd = new TfVersionCommand();
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("add -noprompt -?", builder.toString());
    }

    @Test
    public void testParseOutput_noOutput() {
        final TfVersionCommand cmd = new TfVersionCommand();
        final ToolVersion toolVersion = cmd.parseOutput("", "");
        Assert.assertNotNull(toolVersion);
        Assert.assertEquals("0.0.0", toolVersion.toString());
    }

    @Test
    public void testParseOutput_noErrors() {
        final TfVersionCommand cmd = new TfVersionCommand();
        final String output = "Team Explorer Everywhere Command Line Client (version 1.2.3.123123123)";
        final ToolVersion toolVersion = cmd.parseOutput(output, "");
        Assert.assertNotNull(toolVersion);
        Assert.assertEquals("1.2.3.123123123", toolVersion.toString());
    }

    @Test(expected = RuntimeException.class)
    public void testParseOutput_errors() {
        final TfVersionCommand cmd = new TfVersionCommand();
        final ToolVersion toolVersion = cmd.parseOutput("/path/path", "error");
    }
}
