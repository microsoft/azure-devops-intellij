// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.external.ToolRunner;
import org.junit.Assert;
import org.junit.Test;

public class CreateBranchCommandTest extends AbstractCommandTest {
    @Override
    protected void doAdditionalSetup() {
    }

    @Test
    public void testConstructor_nullContext() {
        final CreateBranchCommand cmd = new CreateBranchCommand(null, "/working/folder", true, "comment1", "author1", "$/item/to/branch", "$/new/branch");
    }

    @Test
    public void testConstructor_withContext() {
        final CreateBranchCommand cmd = new CreateBranchCommand(context, "/working/folder", true, "comment1", "author1", "$/item/to/branch", "$/new/branch");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullArgs() {
        final CreateBranchCommand cmd = new CreateBranchCommand(null, null, false, null, null, null, null);
    }

    @Test
    public void testGetArgumentBuilder_withContext() {
        final CreateBranchCommand cmd = new CreateBranchCommand(context, "/working/folder", true, "comment1", "author1", "$/item/to/branch", "$/new/branch");
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("branch -noprompt -collection:http://server:8080/tfs/defaultcollection ******** -checkin -recursive -comment:comment1 -author:author1 $/item/to/branch $/new/branch", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_nullContext() {
        final CreateBranchCommand cmd = new CreateBranchCommand(null, "/working/folder", true, "comment1", "author1", "$/item/to/branch", "$/new/branch");
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("branch -noprompt -checkin -recursive -comment:comment1 -author:author1 $/item/to/branch $/new/branch", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_nullContext_noRecursive() {
        final CreateBranchCommand cmd = new CreateBranchCommand(null, "/working/folder", false, "comment1", "author1", "$/item/to/branch", "$/new/branch");
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("branch -noprompt -checkin -comment:comment1 -author:author1 $/item/to/branch $/new/branch", builder.toString());
    }

    @Test
    public void testParseOutput_noOutput() {
        final CreateBranchCommand cmd = new CreateBranchCommand(null, "/working/folder", true, "comment1", "author1", "$/item/to/branch", "$/new/branch");
        final String message = cmd.parseOutput("", "");
        Assert.assertEquals("", message);
    }

    @Test
    public void testParseOutput_noErrors() {
        final CreateBranchCommand cmd = new CreateBranchCommand(null, "/working/folder", true, "comment1", "author1", "$/item/to/branch", "$/new/branch");
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
        final CreateBranchCommand cmd = new CreateBranchCommand(null, "/working/folder", true, "comment1", "author1", "$/item/to/branch", "$/new/branch");
        final String message = cmd.parseOutput("/path/path", "error");
    }
}
