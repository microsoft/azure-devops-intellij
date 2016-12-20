// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.external.ToolRunner;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class CreateLabelCommandTest extends AbstractCommandTest {
    List<String> itemSpecs;

    @Override
    protected void doAdditionalSetup() {
        itemSpecs = new ArrayList<String>(3);
        itemSpecs.add("$/item/to/label");
        itemSpecs.add("$/item/to/label2");
        itemSpecs.add("$/item/to/label3");
    }

    @Test
    public void testConstructor_nullContext() {
        final CreateLabelCommand cmd = new CreateLabelCommand(null, "/working/folder", "label1", "comment1", false, itemSpecs);
    }

    @Test
    public void testConstructor_withContext() {
        final CreateLabelCommand cmd = new CreateLabelCommand(context, "/working/folder", "label1", "comment1", false, itemSpecs);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullArgs() {
        final CreateLabelCommand cmd = new CreateLabelCommand(null, null, null, null, true, null);
    }

    @Test
    public void testGetArgumentBuilder_withContext() {
        final CreateLabelCommand cmd = new CreateLabelCommand(context, "/working/folder", "label1", "comment1", false, itemSpecs);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("label -noprompt -collection:http://server:8080/tfs/defaultcollection ******** label1 -comment:comment1 $/item/to/label $/item/to/label2 $/item/to/label3", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_nullContext() {
        final CreateLabelCommand cmd = new CreateLabelCommand(null, "/working/folder", "label1", "comment1", false, itemSpecs);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("label -noprompt label1 -comment:comment1 $/item/to/label $/item/to/label2 $/item/to/label3", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_nullContext_recursive() {
        final CreateLabelCommand cmd = new CreateLabelCommand(null, "/working/folder", "label1", "comment1", true, itemSpecs);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("label -noprompt label1 -recursive -comment:comment1 $/item/to/label $/item/to/label2 $/item/to/label3", builder.toString());
    }

    @Test(expected = RuntimeException.class)
    public void testParseOutput_noOutput() {
        final CreateLabelCommand cmd = new CreateLabelCommand(null, "/working/folder", "label1", "comment1", false, itemSpecs);
        final String message = cmd.parseOutput("", "");
    }

    @Test
    public void testParseOutput_created() {
        final CreateLabelCommand cmd = new CreateLabelCommand(null, "/working/folder", "label1", "comment1", false, itemSpecs);
        final String output = "Created label label1@$/\n";
        final String message = cmd.parseOutput(output, "");
        Assert.assertEquals(CreateLabelCommand.LABEL_CREATED, message);
    }

    @Test
    public void testParseOutput_updated() {
        final CreateLabelCommand cmd = new CreateLabelCommand(null, "/working/folder", "label1", "comment1", false, itemSpecs);
        final String output = "Updated label label1@$/\n";
        final String message = cmd.parseOutput(output, "");
        Assert.assertEquals(CreateLabelCommand.LABEL_UPDATED, message);
    }

    @Test(expected = RuntimeException.class)
    public void testParseOutput_errors() {
        final CreateLabelCommand cmd = new CreateLabelCommand(null, "/working/folder", "label1", "comment1", false, itemSpecs);
        final String message = cmd.parseOutput("/path/path", "error");
    }
}
