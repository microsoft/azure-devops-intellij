// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.helpers.Path;
import com.microsoft.alm.plugin.external.ToolRunner;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class AddCommandTest extends AbstractCommandTest {
    private List<String> files;

    @Override
    protected void doAdditionalSetup() {
        files = new ArrayList<String>();
        files.add("file1");
        files.add("file2");
        files.add("file3");
    }

    @Test
    public void testConstructor_nullContext() {
        final AddCommand cmd = new AddCommand(null, files);
    }

    @Test
    public void testConstructor_withContext() {
        final AddCommand cmd = new AddCommand(context, files);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullArgs() {
        final AddCommand cmd = new AddCommand(null, null);
    }

    @Test
    public void testGetArgumentBuilder_withContext() {
        final AddCommand cmd = new AddCommand(context, files);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("add -noprompt -collection:http://server:8080/tfs/defaultcollection ******** file1 file2 file3", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_nullContext() {
        final AddCommand cmd = new AddCommand(null, files);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("add -noprompt file1 file2 file3", builder.toString());
    }

    @Test
    public void testParseOutput_noOutput() {
        final AddCommand cmd = new AddCommand(null, files);
        final List<String> filesAdded = cmd.parseOutput("", "");
        Assert.assertEquals(0, filesAdded.size());
    }

    @Test
    public void testParseOutput_noErrors() {
        final AddCommand cmd = new AddCommand(null, files);
        final String output = "/path/path:\n" +
                "file1.txt\n" +
                "file2.txt\n" +
                "/path2/path2:\n" +
                "file3.txt\n" +
                "file4.txt\n";
        final List<String> filesAdded = cmd.parseOutput(output, "");
        Assert.assertEquals(4, filesAdded.size());
        Assert.assertEquals(Path.combine("/path/path", "file1.txt"), filesAdded.get(0));
        Assert.assertEquals(Path.combine("/path/path", "file2.txt"), filesAdded.get(1));
        Assert.assertEquals(Path.combine("/path2/path2", "file3.txt"), filesAdded.get(2));
        Assert.assertEquals(Path.combine("/path2/path2", "file4.txt"), filesAdded.get(3));
    }

    @Test(expected = RuntimeException.class)
    public void testParseOutput_errors() {
        final AddCommand cmd = new AddCommand(null, files);
        final List<String> filesAdded = cmd.parseOutput("/path/path", "error");
    }
}
