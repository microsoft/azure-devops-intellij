// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.external.ToolRunner;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class CheckinCommandTest extends AbstractCommandTest {
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
        final CheckinCommand cmd = new CheckinCommand(null, files, "comment");
    }

    @Test
    public void testConstructor_withContext() {
        final CheckinCommand cmd = new CheckinCommand(context, files, "comment");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullArgs() {
        final CheckinCommand cmd = new CheckinCommand(null, null, null);
    }

    @Test
    public void testGetArgumentBuilder_withContext() {
        final CheckinCommand cmd = new CheckinCommand(context, files, "comment");
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("checkin -noprompt -collection:http://server:8080/tfs/defaultcollection ******** file1 file2 file3 -comment:comment", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_nullContext() {
        final CheckinCommand cmd = new CheckinCommand(null, files, "comment");
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("checkin -noprompt file1 file2 file3 -comment:comment", builder.toString());
    }

    @Test
    public void testParseOutput_noOutput() {
        final CheckinCommand cmd = new CheckinCommand(null, files, "comment");
        final String message = cmd.parseOutput("", "");
        Assert.assertEquals("", message);
    }

    @Test
    public void testParseOutput_noErrors() {
        final CheckinCommand cmd = new CheckinCommand(null, files, "comment");
        final String output = "/path/path:\n" +
                "Checking in edit: file1.txt\n" +
                "Checking in edit: file2.txt\n" +
                "/path2/path2:\n" +
                "Checking in add: file3.txt\n" +
                "Checking in add: file4.txt\n" +
                "\n" +
                "Changeset #20 checked in.\n";
        final String message = cmd.parseOutput(output, "");
        Assert.assertEquals("20", message);
    }

    @Test(expected = RuntimeException.class)
    public void testParseOutput_errors() {
        final CheckinCommand cmd = new CheckinCommand(null, files, "comment");
        final String message = cmd.parseOutput("/path/path", "error");
    }
}
