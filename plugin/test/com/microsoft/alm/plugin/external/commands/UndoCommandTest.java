// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.external.ToolRunner;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class UndoCommandTest extends AbstractCommandTest {
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
        final UndoCommand cmd = new UndoCommand(null, files);
    }

    @Test
    public void testConstructor_withContext() {
        final UndoCommand cmd = new UndoCommand(context, files);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullArgs() {
        final UndoCommand cmd = new UndoCommand(null, null);
    }

    @Test
    public void testGetArgumentBuilder_withContext() {
        final UndoCommand cmd = new UndoCommand(context, files);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("undo -noprompt -collection:http://server:8080/tfs/defaultcollection ******** file1 file2 file3", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_nullContext() {
        final UndoCommand cmd = new UndoCommand(null, files);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("undo -noprompt file1 file2 file3", builder.toString());
    }

    @Test
    public void testParseOutput_noOutput() {
        final UndoCommand cmd = new UndoCommand(null, files);
        final List<String> filesUndone = cmd.parseOutput("", "");
        Assert.assertNotNull(filesUndone);
        Assert.assertEquals(0, filesUndone.size());
    }

    @Test
    public void testParseOutput_noErrors() {
        final UndoCommand cmd = new UndoCommand(null, files);
        final String output = "/path/path:\n" +
                "Undoing edit: file1.txt\n" +
                "Undoing edit: file2.txt\n" +
                "/path2/path2:\n" +
                "Undoing edit: file3.txt\n" +
                "Undoing edit: file4.txt\n";
        final List<String> filesUndone = cmd.parseOutput(output, "");
        Assert.assertNotNull(filesUndone);
        Assert.assertEquals(4, filesUndone.size());
    }

    @Test(expected = RuntimeException.class)
    public void testParseOutput_errors() {
        final UndoCommand cmd = new UndoCommand(null, files);
        final List<String> filesUndone = cmd.parseOutput("/path/path", "error");
    }
}
