// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.google.common.collect.ImmutableList;
import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.external.models.ServerStatusType;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class StatusCommandTest extends AbstractCommandTest {

    @Test
    public void testConstructor_nullContext() {
        final StatusCommand cmd = new StatusCommand(null, "/localpath");
    }

    @Test
    public void testConstructor_withContext() {
        final StatusCommand cmd = new StatusCommand(context, "/localpath");
    }

    @Test
    public void testConstructor_nullArgsString() {
        final StatusCommand cmd = new StatusCommand(null, (String) null);
    }

    @Test
    public void testConstructor_nullArgsList() {
        final StatusCommand cmd = new StatusCommand(null, (List) null);
    }

    @Test
    public void testGetArgumentBuilder_withContext() {
        final StatusCommand cmd = new StatusCommand(context, "/localpath");
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("status -noprompt -collection:http://server:8080/tfs/defaultcollection ******** -format:xml -recursive /localpath", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_nullContext() {
        final StatusCommand cmd = new StatusCommand(null, "/localpath");
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("status -noprompt -format:xml -recursive /localpath", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_nullPath() {
        final StatusCommand cmd = new StatusCommand(null, (String) null);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("status -noprompt -format:xml -recursive", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_multipleFiles() {
        final StatusCommand cmd = new StatusCommand(null, ImmutableList.of("/localpath1/file1.txt", "/localpath2/file2.txt"));
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("status -noprompt -format:xml -recursive /localpath1/file1.txt /localpath2/file2.txt", builder.toString());
    }

    @Test
    public void testParseOutput_noOutput() {
        final StatusCommand cmd = new StatusCommand(null, "/localpath");
        final List<PendingChange> pendingChanges = cmd.parseOutput("", "");
        Assert.assertEquals(0, pendingChanges.size());
    }

    @Test
    public void testParseOutput_noErrors() {
        final StatusCommand cmd = new StatusCommand(null, "/localpath");
        final String output = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<status>\n" +
                "<pending-changes/>\n" +
                "<candidate-pending-changes>\n" +
                "<pending-change server-item=\"$/tfsTest_01/test.txt\" version=\"0\" owner=\"jason\" date=\"2016-07-13T12:36:51.060-0400\" lock=\"none\" change-type=\"add\" workspace=\"MyNewWorkspace2\" computer=\"machine\" local-item=\"/path/path/text.txt\"/>\n" +
                "</candidate-pending-changes>\n" +
                "</status>";
        final List<PendingChange> pendingChanges = cmd.parseOutput(output, "");
        Assert.assertEquals(1, pendingChanges.size());
        Assert.assertEquals(ServerStatusType.ADD, pendingChanges.get(0).getChangeTypes().get(0));
        Assert.assertEquals("2016-07-13T12:36:51.060-0400", pendingChanges.get(0).getDate());
        Assert.assertEquals("jason", pendingChanges.get(0).getOwner());
        Assert.assertEquals("machine", pendingChanges.get(0).getComputer());
        Assert.assertEquals("/path/path/text.txt", pendingChanges.get(0).getLocalItem());
        Assert.assertEquals("none", pendingChanges.get(0).getLock());
        Assert.assertEquals("$/tfsTest_01/test.txt", pendingChanges.get(0).getServerItem());
        Assert.assertEquals("0", pendingChanges.get(0).getVersion());
        Assert.assertEquals("MyNewWorkspace2", pendingChanges.get(0).getWorkspace());
    }

    @Test(expected = RuntimeException.class)
    public void testParseOutput_errors() {
        final StatusCommand cmd = new StatusCommand(null, "/localpath");
        final List<PendingChange> pendingChanges = cmd.parseOutput("/path/path", "error");
    }
}
