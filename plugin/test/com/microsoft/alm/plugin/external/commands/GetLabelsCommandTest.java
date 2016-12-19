// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.models.TfvcLabel;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class GetLabelsCommandTest extends AbstractCommandTest {
    @Test
    public void testConstructor_nullContext() {
        final GetLabelsCommand cmd = new GetLabelsCommand(null, "/localpath", null);
    }

    @Test
    public void testConstructor_withContext() {
        final GetLabelsCommand cmd = new GetLabelsCommand(context, "/localpath", null);
    }

    @Test
    public void testConstructor_nullArgs() {
        final GetLabelsCommand cmd = new GetLabelsCommand(null, null, null);
    }

    @Test
    public void testGetArgumentBuilder_withContext() {
        final GetLabelsCommand cmd = new GetLabelsCommand(context, "/localpath", null);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("labels -noprompt -collection:http://server:8080/tfs/defaultcollection ******** -format:xml", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_nullContext() {
        final GetLabelsCommand cmd = new GetLabelsCommand(null, "/localpath", null);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("labels -noprompt -format:xml", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_withFilter() {
        final GetLabelsCommand cmd = new GetLabelsCommand(null, "/localpath", "filter");
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("labels -noprompt -format:xml filter", builder.toString());
    }

    @Test
    public void testParseOutput_noOutput() {
        final GetLabelsCommand cmd = new GetLabelsCommand(null, "/localpath", null);
        final List<TfvcLabel> labels = cmd.parseOutput("", "");
        Assert.assertEquals(0, labels.size());
    }

    @Test
    public void testParseOutput_noErrors() {
        final GetLabelsCommand cmd = new GetLabelsCommand(null, "/localpath", null);
        final String output = "<?xml version=\"1.0\" encoding=\"utf-8\"?><labels>\n" +
                "     <label name=\"MyLabel\" scope=\"$/tfsTest_01\" user=\"domain\\user1\" date=\"2016-12-15T14:53:38.247-0500\">\n" +
                "         <comment>My comment.</comment>\n" +
                "         <item changeset=\"266\" server-item=\"$/tfsTest_03/path/file1.txt\"/>\n" +
                "         <item changeset=\"267\" server-item=\"$/tfsTest_03/path/file2.txt\"/>\n" +
                "     </label>\n" +
                "     <label name=\"Label2\" scope=\"$/tfsTest_02\" user=\"domain\\user2\" date=\"2016-12-15T14:53:38.247-0501\">\n" +
                "         <comment>My second comment.</comment>\n" +
                "         <item changeset=\"286\" server-item=\"$/tfsTest_03/path/file1.txt\"/>\n" +
                "         <item changeset=\"287\" server-item=\"$/tfsTest_03/path/file2.txt\"/>\n" +
                "         <item changeset=\"288\" server-item=\"$/tfsTest_03/path/file3.txt\"/>\n" +
                "     </label>\n" +
                "</labels>";
        final List<TfvcLabel> labels = cmd.parseOutput(output, "");
        Assert.assertEquals(2, labels.size());
        Assert.assertEquals("MyLabel", labels.get(0).getName());
        Assert.assertEquals("$/tfsTest_01", labels.get(0).getScope());
        Assert.assertEquals("domain\\user1", labels.get(0).getUser());
        Assert.assertEquals("My comment.", labels.get(0).getComment());
        Assert.assertEquals("2016-12-15T14:53:38.247-0500", labels.get(0).getDate());
        Assert.assertEquals(2, labels.get(0).getItems().size());
        Assert.assertEquals("Label2", labels.get(1).getName());
        Assert.assertEquals("$/tfsTest_02", labels.get(1).getScope());
        Assert.assertEquals("domain\\user2", labels.get(1).getUser());
        Assert.assertEquals("My second comment.", labels.get(1).getComment());
        Assert.assertEquals("2016-12-15T14:53:38.247-0501", labels.get(1).getDate());
        Assert.assertEquals(3, labels.get(1).getItems().size());
    }

    @Test(expected = RuntimeException.class)
    public void testParseOutput_errors() {
        final GetLabelsCommand cmd = new GetLabelsCommand(null, "/localpath", null);
        final List<TfvcLabel> labels = cmd.parseOutput("/path/path", "error");
    }
}
