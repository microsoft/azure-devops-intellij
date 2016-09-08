// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.models.SyncResults;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class SyncCommandTest extends AbstractCommandTest {
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
        final SyncCommand cmd = new SyncCommand(null, files, true);
    }

    @Test
    public void testConstructor_withContext() {
        final SyncCommand cmd = new SyncCommand(context, files, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullArgs() {
        final SyncCommand cmd = new SyncCommand(null, null, false);
    }

    @Test
    public void testGetArgumentBuilder_withContext() {
        final SyncCommand cmd = new SyncCommand(context, files, true);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("get -noprompt -collection:http://server:8080/tfs/defaultcollection ******** file1 file2 file3 -recursive", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_nullContext() {
        final SyncCommand cmd = new SyncCommand(null, files, true);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("get -noprompt file1 file2 file3 -recursive", builder.toString());
    }

    @Test
    public void testParseOutput_noOutput() {
        final SyncCommand cmd = new SyncCommand(null, files, true);
        final SyncResults results = cmd.parseOutput("", "");
        Assert.assertEquals(false, results.doConflictsExists());
        Assert.assertEquals(0, results.getDeletedFiles().size());
        Assert.assertEquals(0, results.getExceptions().size());
        Assert.assertEquals(0, results.getNewFiles().size());
        Assert.assertEquals(0, results.getUpdatedFiles().size());
    }

    @Test
    public void testParseOutput_updateToDate() {
        final SyncCommand cmd = new SyncCommand(null, files, true);
        final SyncResults results = cmd.parseOutput("All files up to date.", "");
        Assert.assertEquals(false, results.doConflictsExists());
        Assert.assertEquals(0, results.getDeletedFiles().size());
        Assert.assertEquals(0, results.getExceptions().size());
        Assert.assertEquals(0, results.getNewFiles().size());
        Assert.assertEquals(0, results.getUpdatedFiles().size());
    }

    @Test
    public void testParseOutput_noErrors() {
        final SyncCommand cmd = new SyncCommand(null, files, true);
        final String output = "D:\\tmp\\test:\n" +
                "Getting addFold\n" +
                "Getting addFold-branch\n" +
                "\n" +
                "D:\\tmp\\test\\addFold-branch:\n" +
                "Getting testHereRename.txt\n" +
                "\n" +
                "D:\\tmp\\test\\addFold:\n" +
                "Getting testHere3\n" +
                "Getting testHereRename7.txt\n" +
                "\n" +
                "D:\\tmp\\test:\n" +
                "Getting Rename2.txt\n" +
                "Replacing test3.txt\n" +
                "Getting TestAdd.txt\n" +
                "Deleting TestDelete.txt\n" +
                "Deleting TestDelete2.txt\n" +
                "\n" +
                "---- Summary: 1 conflicts, 0 warnings, 0 errors ----\n" +
                "Conflict D:\\tmp\\test\\test_renamed.txt - Unable to perform the get operation because you have a conflicting rename, edit";
        final String stderr = "Conflict test_renamed.txt - Unable to perform the get operation because you have a conflicting rename, edit\n";
        final SyncResults results = cmd.parseOutput(output, stderr);
        Assert.assertEquals(true, results.doConflictsExists());
        Assert.assertEquals(2, results.getDeletedFiles().size());
        Assert.assertEquals(0, results.getExceptions().size());
        Assert.assertEquals(7, results.getNewFiles().size());
        Assert.assertEquals(1, results.getUpdatedFiles().size());
    }

    @Test
    public void testParseOutput_errors() {
        final SyncCommand cmd = new SyncCommand(null, files, true);
        final String output = "D:\\tmp\\test:\n" +
                "Getting addFold\n" +
                "Deleting TestDelete2.txt\n" +
                "\n" +
                "---- Summary: 0 conflicts, 0 warnings, 1 errors ----\n" +
                "Unable to get test4.txt because it is locked by another user.\n";
        final String stderr = "Unable to get test4.txt because it is locked by another user.";
        final SyncResults results = cmd.parseOutput(output, stderr);
        Assert.assertEquals(false, results.doConflictsExists());
        Assert.assertEquals(1, results.getDeletedFiles().size());
        Assert.assertEquals(1, results.getExceptions().size());
        Assert.assertEquals(1, results.getNewFiles().size());
        Assert.assertEquals(0, results.getUpdatedFiles().size());
        Assert.assertEquals(stderr, results.getExceptions().get(0).getMessage());
    }
}
