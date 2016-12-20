// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.external.ToolRunner;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class LockCommandTest extends AbstractCommandTest {
    List<String> itemSpecs;

    @Override
    protected void doAdditionalSetup() {
        itemSpecs = new ArrayList<String>(3);
        itemSpecs.add("$/item/to/lock");
        itemSpecs.add("$/item/to/lock2");
        itemSpecs.add("$/item/to/lock3");
    }

    @Test
    public void testConstructor_nullContext() {
        final LockCommand cmd = new LockCommand(null, "/working/folder", LockCommand.LockLevel.CHECKIN, false, itemSpecs);
    }

    @Test
    public void testConstructor_withContext() {
        final LockCommand cmd = new LockCommand(context, "/working/folder", LockCommand.LockLevel.CHECKIN, false, itemSpecs);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullArgs() {
        final LockCommand cmd = new LockCommand(null, null, null, true, null);
    }

    @Test
    public void testGetArgumentBuilder_withContext() {
        final LockCommand cmd = new LockCommand(context, "/working/folder", LockCommand.LockLevel.CHECKIN, false, itemSpecs);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("lock -noprompt -collection:http://server:8080/tfs/defaultcollection ******** -lock:checkin $/item/to/lock $/item/to/lock2 $/item/to/lock3", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_nullContext() {
        final LockCommand cmd = new LockCommand(null, "/working/folder", LockCommand.LockLevel.CHECKIN, false, itemSpecs);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("lock -noprompt -lock:checkin $/item/to/lock $/item/to/lock2 $/item/to/lock3", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_checkout() {
        final LockCommand cmd = new LockCommand(null, "/working/folder", LockCommand.LockLevel.CHECKOUT, false, itemSpecs);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("lock -noprompt -lock:checkout $/item/to/lock $/item/to/lock2 $/item/to/lock3", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_none() {
        final LockCommand cmd = new LockCommand(null, "/working/folder", LockCommand.LockLevel.NONE, false, itemSpecs);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("lock -noprompt -lock:none $/item/to/lock $/item/to/lock2 $/item/to/lock3", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_nullContext_recursive() {
        final LockCommand cmd = new LockCommand(null, "/working/folder", LockCommand.LockLevel.CHECKIN, true, itemSpecs);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("lock -noprompt -lock:checkin -recursive $/item/to/lock $/item/to/lock2 $/item/to/lock3", builder.toString());
    }

    @Test
    public void testParseOutput_noOutput() {
        final LockCommand cmd = new LockCommand(null, "/working/folder", LockCommand.LockLevel.CHECKIN, false, itemSpecs);
        final String message = cmd.parseOutput("", "");
    }

    @Test(expected = RuntimeException.class)
    public void testParseOutput_allErrors() {
        final LockCommand cmd = new LockCommand(null, "/working/folder", LockCommand.LockLevel.CHECKIN, false, itemSpecs);
        final String message = cmd.parseOutput("", "TF14090: Cannot unlock $/tfsTest_03/Folder333. It is not currently locked in your workspace.");
    }

    @Test
    public void testParseOutput_someErrors() {
        final LockCommand cmd = new LockCommand(null, "/working/folder", LockCommand.LockLevel.CHECKIN, false, itemSpecs);
        final String message = cmd.parseOutput("/path/path", "TF14090: Cannot unlock $/tfsTest_03/Folder333. It is not currently locked in your workspace.");
        Assert.assertEquals(message, "");
    }

    @Test(expected = RuntimeException.class)
    public void testParseOutput_unexpectedErrors() {
        final LockCommand cmd = new LockCommand(null, "/working/folder", LockCommand.LockLevel.CHECKIN, false, itemSpecs);
        final String message = cmd.parseOutput("/path/path", "TF1: error text.");
    }

    @Test
    public void testLockLevel_fromString() {
        Assert.assertEquals(LockCommand.LockLevel.NONE, LockCommand.LockLevel.fromString("none"));
        Assert.assertEquals(LockCommand.LockLevel.NONE, LockCommand.LockLevel.fromString("None"));
        Assert.assertEquals(LockCommand.LockLevel.NONE, LockCommand.LockLevel.fromString("noNe"));
        Assert.assertEquals(LockCommand.LockLevel.NONE, LockCommand.LockLevel.fromString("NONE"));
        Assert.assertEquals(LockCommand.LockLevel.CHECKIN, LockCommand.LockLevel.fromString("checkin"));
        Assert.assertEquals(LockCommand.LockLevel.CHECKIN, LockCommand.LockLevel.fromString("check-in"));
        Assert.assertEquals(LockCommand.LockLevel.CHECKIN, LockCommand.LockLevel.fromString("Checkin"));
        Assert.assertEquals(LockCommand.LockLevel.CHECKIN, LockCommand.LockLevel.fromString("checkIN"));
        Assert.assertEquals(LockCommand.LockLevel.CHECKIN, LockCommand.LockLevel.fromString("CHECK-IN"));
        Assert.assertEquals(LockCommand.LockLevel.CHECKIN, LockCommand.LockLevel.fromString("CHECKIN"));
        Assert.assertEquals(LockCommand.LockLevel.CHECKOUT, LockCommand.LockLevel.fromString("checkout"));
        Assert.assertEquals(LockCommand.LockLevel.CHECKOUT, LockCommand.LockLevel.fromString("check-out"));
        Assert.assertEquals(LockCommand.LockLevel.CHECKOUT, LockCommand.LockLevel.fromString("checkOut"));
        Assert.assertEquals(LockCommand.LockLevel.CHECKOUT, LockCommand.LockLevel.fromString("checkOUT"));
        Assert.assertEquals(LockCommand.LockLevel.CHECKOUT, LockCommand.LockLevel.fromString("CHECKOUT"));
        Assert.assertEquals(LockCommand.LockLevel.CHECKOUT, LockCommand.LockLevel.fromString("CHECK-OUT"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLockLevel_fromString_fails() {
        LockCommand.LockLevel.fromString("failure");
    }
}
