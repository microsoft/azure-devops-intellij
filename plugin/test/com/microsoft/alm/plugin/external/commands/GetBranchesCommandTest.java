// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.external.ToolRunner;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class GetBranchesCommandTest extends AbstractCommandTest {

    @Override
    protected void doAdditionalSetup() {
    }

    @Test
    public void testConstructor_nullContext() {
        final GetBranchesCommand cmd = new GetBranchesCommand(null, "/work/folder", "$/tp/path/sourcefolder");
    }

    @Test
    public void testConstructor_withContext() {
        final GetBranchesCommand cmd = new GetBranchesCommand(context, "/work/folder", "$/tp/path/sourcefolder");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullArgs() {
        final GetBranchesCommand cmd = new GetBranchesCommand(null, null, null);
    }

    @Test
    public void testGetArgumentBuilder_withContext() {
        final GetBranchesCommand cmd = new GetBranchesCommand(context, "/work/folder", "$/tp/path/sourcefolder");
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("branches -noprompt -collection:http://server:8080/tfs/defaultcollection ******** $/tp/path/sourcefolder", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_nullContext() {
        final GetBranchesCommand cmd = new GetBranchesCommand(null, "/work/folder", "$/tp/path/sourcefolder");
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("branches -noprompt $/tp/path/sourcefolder", builder.toString());
    }

    @Test
    public void testParseOutput_noOutput() {
        final GetBranchesCommand cmd = new GetBranchesCommand(null, "/work/folder", "$/tp/path/sourcefolder");
        final List<String> branches = cmd.parseOutput("", "");
        Assert.assertEquals(0, branches.size());
    }

    @Test
    public void testParseOutput_noErrors() {
        final GetBranchesCommand cmd = new GetBranchesCommand(null, "/work/folder", "$/tp/path/sourcefolder");
        final String output =
                "$/tfsTest_03/Folder1/model\n" +
                        ">>      $/tfsTest_03/Folder1/model_01     Branched from version 8 <<\n" +
                        "             $/tfsTest_03/Folder1/model_01_copy Branched from version 207\n" +
                        "        $/tfsTest_03/Folder1/model_02     Branched from version 8\n" +
                        "        $/tfsTest_03/Folder1/model_03     Branched from version 8\n" +
                        "        $/tfsTest_03/Folder1/model_04     Branched from version 8\n" +
                        "        $/tfsTest_03/Folder2/model_05   Branched from version 8\n" +
                        "        $/tfsTest_03/model_branch_44    Branched from version 8\n" +
                        "        $/tfsTest_03/model_branch_06    Branched from version 8\n" +
                        "        $/tfsTest_03/Folder1/model_11     Branched from version 8\n" +
                        "        $/tfsTest_03/Folder333  Branched from version 8\n" +
                        "";
        final List<String> branches = cmd.parseOutput(output, "");
        Assert.assertEquals(10, branches.size());
        Assert.assertFalse(branches.contains("$/tfsTest_03/Folder1/model_01"));
        Assert.assertTrue(branches.contains("$/tfsTest_03/Folder1/model"));
        Assert.assertTrue(branches.contains("$/tfsTest_03/Folder1/model_01_copy"));
        Assert.assertTrue(branches.contains("$/tfsTest_03/Folder1/model_02"));
        Assert.assertTrue(branches.contains("$/tfsTest_03/Folder1/model_03"));
        Assert.assertTrue(branches.contains("$/tfsTest_03/Folder1/model_04"));
        Assert.assertTrue(branches.contains("$/tfsTest_03/Folder2/model_05"));
        Assert.assertTrue(branches.contains("$/tfsTest_03/model_branch_44"));
        Assert.assertTrue(branches.contains("$/tfsTest_03/model_branch_06"));
        Assert.assertTrue(branches.contains("$/tfsTest_03/Folder1/model_11"));
    }

    @Test(expected = RuntimeException.class)
    public void testParseOutput_errors() {
        final GetBranchesCommand cmd = new GetBranchesCommand(null, "/work/folder", "$/tp/path/sourcefolder");
        final List<String> branches = cmd.parseOutput("/path/path", "error");
    }
}
