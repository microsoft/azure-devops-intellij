// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.models.MergeResults;
import com.microsoft.alm.plugin.external.models.ServerStatusType;
import com.microsoft.alm.plugin.external.models.VersionSpec;
import org.junit.Assert;
import org.junit.Test;

public class MergeCommandTest extends AbstractCommandTest {
    private final String workingFolder = "/work/folder";
    private final String source = "$/project/branch1/source";
    private final String destination = "$/project/branch2/destination";

    @Override
    protected void doAdditionalSetup() {
    }

    @Test
    public void testConstructor_nullContext() {
        final MergeCommand cmd = new MergeCommand(null, workingFolder, source, destination, null, true);
    }

    @Test
    public void testConstructor_withContext() {
        final MergeCommand cmd = new MergeCommand(context, workingFolder, source, destination, null, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullArgs() {
        final MergeCommand cmd = new MergeCommand(null, null, null, null, null, false);
    }

    @Test
    public void testGetArgumentBuilder_withContext() {
        final MergeCommand cmd = new MergeCommand(context, workingFolder, source, destination, null, true);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("merge -noprompt -collection:http://server:8080/tfs/defaultcollection ******** -format:detailed -recursive $/project/branch1/source $/project/branch2/destination", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_nullContext() {
        final MergeCommand cmd = new MergeCommand(null, workingFolder, source, destination, null, true);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("merge -noprompt -format:detailed -recursive $/project/branch1/source $/project/branch2/destination", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_withVersionSpec() {
        final MergeCommand cmd = new MergeCommand(context, workingFolder, source, destination, VersionSpec.create(25), true);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("merge -noprompt -collection:http://server:8080/tfs/defaultcollection ******** -format:detailed -version:C25 -recursive $/project/branch1/source $/project/branch2/destination", builder.toString());
    }

    @Test
    public void testParseOutput_noOutput() {
        final MergeCommand cmd = new MergeCommand(null, workingFolder, source, destination, null, true);
        final MergeResults result = cmd.parseOutput("", "");
        Assert.assertEquals(false, result.doConflictsExists());
        Assert.assertEquals(true, result.noChangesToMerge());
        Assert.assertEquals(0, result.getMappings().size());
    }

    @Test
    public void testParseOutput_noChangesToMerge() {
        final MergeCommand cmd = new MergeCommand(null, workingFolder, source, destination, null, true);
        final MergeResults result = cmd.parseOutput("There are no changes to merge.", "");
        Assert.assertEquals(false, result.doConflictsExists());
        Assert.assertEquals(true, result.noChangesToMerge());
        Assert.assertEquals(0, result.getMappings().size());
    }

    @Test
    public void testParseOutput_noConflicts() {
        final MergeCommand cmd = new MergeCommand(null, workingFolder, source, destination, null, true);
        final String output = "merge, edit: $/project/branch1/source/DemandEquals.java;C222~C222 -> $/project/branch2/destination/DemandEquals.java;C222\n" +
                "merge, edit: $/project/branch1/source/Demand.java;C222~C222 -> $/project/branch2/destination/Demand.java;C213\n" +
                "merge, branch: $/project/branch1/source/Demand2.java;C215 -> $/project/branch2/destination/Demand2.java";
        final MergeResults result = cmd.parseOutput(output, "");
        Assert.assertEquals(false, result.doConflictsExists());
        Assert.assertEquals(false, result.noChangesToMerge());
        Assert.assertEquals(3, result.getMappings().size());
        assertMergeMapping(result, 0, "DemandEquals.java", "C222~C222", "C222", false, ServerStatusType.MERGE, ServerStatusType.EDIT);
        assertMergeMapping(result, 1, "Demand.java", "C222~C222", "C213", false, ServerStatusType.MERGE, ServerStatusType.EDIT);
        assertMergeMapping(result, 2, "Demand2.java", "C215~C215", "T", false, ServerStatusType.MERGE, ServerStatusType.BRANCH);
    }

    @Test
    public void testParseOutput_conflicts() {
        final MergeCommand cmd = new MergeCommand(null, workingFolder, source, destination, null, true);
        final String output = "Conflict (merge, edit): $/project/branch1/source/DemandEquals.java;C222~C222 -> $/project/branch2/destination/DemandEquals.java;C222\n" +
                "merge, edit: $/project/branch1/source/Demand.java;C222~C222 -> $/project/branch2/destination/Demand.java;C213\n" +
                "merge, branch: $/project/branch1/source/Demand2.java;C215 -> $/project/branch2/destination/Demand2.java";
        final MergeResults result = cmd.parseOutput(output, "");
        Assert.assertEquals(true, result.doConflictsExists());
        Assert.assertEquals(false, result.noChangesToMerge());
        Assert.assertEquals(3, result.getMappings().size());
        assertMergeMapping(result, 0, "DemandEquals.java", "C222~C222", "C222", true, ServerStatusType.MERGE, ServerStatusType.EDIT);
        assertMergeMapping(result, 1, "Demand.java", "C222~C222", "C213", false, ServerStatusType.MERGE, ServerStatusType.EDIT);
        assertMergeMapping(result, 2, "Demand2.java", "C215~C215", "T", false, ServerStatusType.MERGE, ServerStatusType.BRANCH);
    }

    private void assertMergeMapping(final MergeResults results, final int index, final String filename,
                                    final String fromRange, final String toVersion, final boolean isConflict,
                                    final ServerStatusType... changeTypes) {
        Assert.assertTrue(results.getMappings().size() > index);
        Assert.assertEquals(isConflict, results.getMappings().get(index).isConflict());
        Assert.assertEquals(source + "/" + filename, results.getMappings().get(index).getFromServerItem());
        Assert.assertEquals(fromRange, results.getMappings().get(index).getFromServerItemVersion().toString());
        Assert.assertEquals(destination + "/" + filename, results.getMappings().get(index).getToServerItem());
        Assert.assertEquals(toVersion, results.getMappings().get(index).getToServerItemVersion().toString());
        Assert.assertEquals(changeTypes.length, results.getMappings().get(0).getChangeTypes().size());
        int changeTypeIndex = 0;
        for (ServerStatusType type : changeTypes) {
            Assert.assertEquals(type, results.getMappings().get(index).getChangeTypes().get(changeTypeIndex));
            changeTypeIndex++;
        }
    }

    @Test
    public void testParseOutput_errors() {
        final MergeCommand cmd = new MergeCommand(null, workingFolder, source, destination, null, true);
        final MergeResults result = cmd.parseOutput("", "\n\n\nerror\n\n\n");
        Assert.assertTrue(result.getMappings().size() == 0);
        Assert.assertTrue(result.getWarnings().size() == 0);
        Assert.assertTrue(result.getErrors().size() == 1);
    }
}
