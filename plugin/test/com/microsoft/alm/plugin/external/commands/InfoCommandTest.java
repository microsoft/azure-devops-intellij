// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.models.ExtendedItemInfo;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class InfoCommandTest extends AbstractCommandTest {
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
        final InfoCommand cmd = new InfoCommand(null, files);
    }

    @Test
    public void testConstructor_withContext() {
        final InfoCommand cmd = new InfoCommand(context, files);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullArgs() {
        final InfoCommand cmd = new InfoCommand(null, null);
    }

    @Test
    public void testGetArgumentBuilder_withContext() {
        final InfoCommand cmd = new InfoCommand(context, files);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("info -noprompt -collection:http://server:8080/tfs/defaultcollection ******** file1 file2 file3", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_nullContext() {
        final InfoCommand cmd = new InfoCommand(null, files);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("info -noprompt file1 file2 file3", builder.toString());
    }

    @Test
    public void testParseOutput_noOutput() {
        final InfoCommand cmd = new InfoCommand(null, files);
        List<ExtendedItemInfo> infos = cmd.parseOutput("", "");
        Assert.assertEquals(0, infos.size());
    }

    @Test
    public void testParseOutput_noErrors() {
        final InfoCommand cmd = new InfoCommand(null, files);
        final String output = "" +
                "Local information:\n" +
                "Local path:  /path/to/build.xml\n" +
                "Server path: $/TFVC_1/build.xml\n" +
                "Changeset:   18\n" +
                "Change:      none\n" +
                "Type:        file\n" +
                "Server information:\n" +
                "Server path:   $/TFVC_1/build.xml\n" +
                "Changeset:     19\n" +
                "Deletion ID:   0\n" +
                "Lock:          none\n" +
                "Lock owner:\n" +
                "Last modified: Nov 18, 2016 11:10:20 AM\n" +
                "Type:          file\n" +
                "File type:     windows-1252\n" +
                "Size:          1385\n" +
                "\n" +
                "Local information:\n" +
                "Local path:  /path/to/HelloWorld.java\n" +
                "Server path: $/TFVC_1/src/com/microsoft/demo/HelloWorld.java\n" +
                "Changeset:   13\n" +
                "Change:      edit\n" +
                "Type:        file\n" +
                "Server information:\n" +
                "Server path:   $/TFVC_1/src/com/microsoft/demo/HelloWorld.java\n" +
                "Changeset:     13\n" +
                "Deletion ID:   0\n" +
                "Lock:          none\n" +
                "Lock owner:\n" +
                "Last modified: Sep 8, 2016 4:34:33 PM\n" +
                "Type:          file\n" +
                "File type:     windows-1252\n" +
                "Size:          164";
        List<ExtendedItemInfo> infos = cmd.parseOutput(output, "");
        Assert.assertEquals(2, infos.size());
        Assert.assertEquals("/path/to/build.xml", infos.get(0).getLocalItem());
        Assert.assertEquals("$/TFVC_1/build.xml", infos.get(0).getServerItem());
        Assert.assertEquals("18", infos.get(0).getLocalVersion());
        Assert.assertEquals("19", infos.get(0).getServerVersion());
        Assert.assertEquals("/path/to/HelloWorld.java", infos.get(1).getLocalItem());
        Assert.assertEquals("$/TFVC_1/src/com/microsoft/demo/HelloWorld.java", infos.get(1).getServerItem());
        Assert.assertEquals("13", infos.get(1).getLocalVersion());
        Assert.assertEquals("13", infos.get(1).getServerVersion());
    }

    @Test(expected = RuntimeException.class)
    public void testParseOutput_errors() {
        final InfoCommand cmd = new InfoCommand(null, files);
        List<ExtendedItemInfo> infos = cmd.parseOutput("", "error");
    }
}
