// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.external.ToolRunner;
import org.junit.Assert;
import org.junit.Test;

public class RenameCommandTest extends AbstractCommandTest {
    public static final String OLD_NAME = "/path/to/file/nameOld";
    public static final String NEW_NAME = "/path/to/file/nameNew";

    @Test
    public void testConstructor_nullContext() {
        final RenameCommand cmd = new RenameCommand(null, OLD_NAME, NEW_NAME);
    }

    @Test
    public void testConstructor_withContext() {
        final RenameCommand cmd = new RenameCommand(context, OLD_NAME, NEW_NAME);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullOldFile() {
        // file names can't be null
        final RenameCommand cmd = new RenameCommand(null, null, NEW_NAME);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullNewFile() {
        // file names can't be null
        final RenameCommand cmd = new RenameCommand(null, OLD_NAME, null);
    }

    @Test
    public void testGetArgumentBuilder_withContext() {
        final RenameCommand cmd = new RenameCommand(context, OLD_NAME, NEW_NAME);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals(String.format("rename -noprompt -collection:http://server:8080/tfs/defaultcollection ******** %s %s", OLD_NAME, NEW_NAME), builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_nullContext() {
        final RenameCommand cmd = new RenameCommand(null, OLD_NAME, NEW_NAME);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals(String.format("rename -noprompt %s %s", OLD_NAME, NEW_NAME), builder.toString());
    }

    @Test(expected = RuntimeException.class)
    public void testParseOutput_errors() {
        final RenameCommand cmd = new RenameCommand(null, OLD_NAME, NEW_NAME);
        final String results = cmd.parseOutput("/path/path", "error");
    }
}
