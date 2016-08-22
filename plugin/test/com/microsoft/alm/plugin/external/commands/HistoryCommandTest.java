// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.models.ChangeSet;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class HistoryCommandTest extends AbstractCommandTest {
    @Test
    public void testConstructor_nullContext() {
        final HistoryCommand cmd = new HistoryCommand(null, "/localpath", null, 1, true, null);
    }

    @Test
    public void testConstructor_withContext() {
        final HistoryCommand cmd = new HistoryCommand(context, "/localpath", null, 1, true, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullArgs() {
        final HistoryCommand cmd = new HistoryCommand(null, null, null, 0, false, null);
    }

    @Test
    public void testGetArgumentBuilder_withContext() {
        final HistoryCommand cmd = new HistoryCommand(context, "/localpath", null, 1, true, null);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("history -noprompt -collection:http://server:8080/tfs/defaultcollection ******** -format:xml -recursive -stopafter:1 /localpath", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_nullContext() {
        final HistoryCommand cmd = new HistoryCommand(null, "/localpath", null, 1, true, null);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("history -noprompt -format:xml -recursive -stopafter:1 /localpath", builder.toString());
    }

    @Test
    public void testParseOutput_noOutput() {
        final HistoryCommand cmd = new HistoryCommand(null, "/localpath", null, 1, true, null);
        final List<ChangeSet> changesets = cmd.parseOutput("", "");
        Assert.assertEquals(0, changesets.size());
    }

    @Test
    public void testParseOutput_noErrors() {
        final HistoryCommand cmd = new HistoryCommand(null, "/localpath", null, 1, true, null);
        final String output = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<history>\n" +
                "<changeset id=\"4\" owner=\"john\" committer=\"john\" date=\"2016-06-07T11:18:18.790-0400\">\n" +
                "<comment>add readme</comment>\n" +
                "<item change-type=\"add\" server-item=\"$/tfs01/readme.txt\"/>\n" +
                "</changeset>\n" +
                "<changeset id=\"3\" owner=\"jeff\" committer=\"jeff\" date=\"2016-06-07T11:13:51.747-0400\">\n" +
                "<comment>initial checkin</comment>\n" +
                "<item change-type=\"add\" server-item=\"$/tfs01/com.microsoft.core\"/>\n" +
                "<item change-type=\"add\" server-item=\"$/tfs01/com.microsoft.core/.classpath\"/>\n" +
                "</changeset>\n" +
                "</history>";
        final List<ChangeSet> changesets = cmd.parseOutput(output, "");
        Assert.assertEquals(2, changesets.size());
        Assert.assertEquals("4", changesets.get(0).getId());
        Assert.assertEquals("john", changesets.get(0).getOwner());
        Assert.assertEquals("add readme", changesets.get(0).getComment());
        Assert.assertEquals("john", changesets.get(0).getCommitter());
        Assert.assertEquals("2016-06-07T11:18:18.790-0400", changesets.get(0).getDate());
        Assert.assertEquals(1, changesets.get(0).getChanges().size());
        Assert.assertEquals("3", changesets.get(1).getId());
        Assert.assertEquals("jeff", changesets.get(1).getOwner());
        Assert.assertEquals("initial checkin", changesets.get(1).getComment());
        Assert.assertEquals("jeff", changesets.get(1).getCommitter());
        Assert.assertEquals("2016-06-07T11:13:51.747-0400", changesets.get(1).getDate());
        Assert.assertEquals(2, changesets.get(1).getChanges().size());
    }

    @Test(expected = RuntimeException.class)
    public void testParseOutput_errors() {
        final HistoryCommand cmd = new HistoryCommand(null, "/localpath", null, 1, true, null);
        final List<ChangeSet> changesets = cmd.parseOutput("/path/path", "error");
    }
}
