// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.utils;

import com.microsoft.alm.plugin.AbstractTest;
import com.microsoft.alm.plugin.external.models.Workspace;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class WorkspaceHelperTest extends AbstractTest {
    @Test
    public void testAreMappingsDifferent_null() {
        final List<Workspace.Mapping> mappings1 = null;
        final List<Workspace.Mapping> mappings2 = new ArrayList<Workspace.Mapping>();;
        Assert.assertEquals(false, WorkspaceHelper.areMappingsDifferent(mappings1, mappings1));
        Assert.assertEquals(true, WorkspaceHelper.areMappingsDifferent(mappings1, mappings2));
        Assert.assertEquals(true, WorkspaceHelper.areMappingsDifferent(mappings2, mappings1));
    }

    @Test
    public void testAreMappingsDifferent_empty() {
        final List<Workspace.Mapping> mappings1 = new ArrayList<Workspace.Mapping>();
        final List<Workspace.Mapping> mappings2 = new ArrayList<Workspace.Mapping>();
        Assert.assertEquals(false, WorkspaceHelper.areMappingsDifferent(mappings1, mappings2));
    }

    @Test
    public void testAreMappingsDifferent_identical() {
        final List<Workspace.Mapping> mappings = new ArrayList<Workspace.Mapping>();
        mappings.add(new Workspace.Mapping("", "", false));
        Assert.assertEquals(false, WorkspaceHelper.areMappingsDifferent(mappings, mappings));
    }

    @Test
    public void testAreMappingsDifferent_sameContent() {
        final List<Workspace.Mapping> mappings1 = new ArrayList<Workspace.Mapping>();
        mappings1.add(new Workspace.Mapping("one", "localOne", false));
        mappings1.add(new Workspace.Mapping("two", "localTwo", false));
        mappings1.add(new Workspace.Mapping("three", "localThree", false));
        final List<Workspace.Mapping> mappings2 = new ArrayList<Workspace.Mapping>();
        mappings2.add(new Workspace.Mapping("one", "localOne", false));
        mappings2.add(new Workspace.Mapping("two", "localTwo", false));
        mappings2.add(new Workspace.Mapping("three", "localThree", false));
        Assert.assertEquals(false, WorkspaceHelper.areMappingsDifferent(mappings1, mappings2));
        Assert.assertEquals(false, WorkspaceHelper.areMappingsDifferent(mappings2, mappings1));
    }

    @Test
    public void testAreMappingsDifferent_differentCount() {
        final List<Workspace.Mapping> mappings1 = new ArrayList<Workspace.Mapping>();
        mappings1.add(new Workspace.Mapping("one", "two", false));
        final List<Workspace.Mapping> mappings2 = new ArrayList<Workspace.Mapping>();
        Assert.assertEquals(true, WorkspaceHelper.areMappingsDifferent(mappings1, mappings2));
        Assert.assertEquals(true, WorkspaceHelper.areMappingsDifferent(mappings2, mappings1));
    }

    @Test
    public void testAreMappingsDifferent_differentServerPaths() {
        final List<Workspace.Mapping> mappings1 = new ArrayList<Workspace.Mapping>();
        mappings1.add(new Workspace.Mapping("one", "localOne", false));
        mappings1.add(new Workspace.Mapping("two", "localTwo", false));
        mappings1.add(new Workspace.Mapping("three", "localThree", false));
        final List<Workspace.Mapping> mappings2 = new ArrayList<Workspace.Mapping>();
        mappings2.add(new Workspace.Mapping("one", "localOne", false));
        mappings2.add(new Workspace.Mapping("222", "localTwo", false));
        mappings2.add(new Workspace.Mapping("three", "localThree", false));
        Assert.assertEquals(true, WorkspaceHelper.areMappingsDifferent(mappings1, mappings2));
        Assert.assertEquals(true, WorkspaceHelper.areMappingsDifferent(mappings2, mappings1));
    }

    @Test
    public void testAreMappingsDifferent_differentLocalPaths() {
        final List<Workspace.Mapping> mappings1 = new ArrayList<Workspace.Mapping>();
        mappings1.add(new Workspace.Mapping("one", "localOne", false));
        mappings1.add(new Workspace.Mapping("two", "localTwo", false));
        mappings1.add(new Workspace.Mapping("three", "localThree", false));
        final List<Workspace.Mapping> mappings2 = new ArrayList<Workspace.Mapping>();
        mappings2.add(new Workspace.Mapping("one", "localOne", false));
        mappings2.add(new Workspace.Mapping("two", "local222", false));
        mappings2.add(new Workspace.Mapping("three", "localThree", false));
        Assert.assertEquals(true, WorkspaceHelper.areMappingsDifferent(mappings1, mappings2));
        Assert.assertEquals(true, WorkspaceHelper.areMappingsDifferent(mappings2, mappings1));
    }

    @Test
    public void testAreMappingsDifferent_differentCloaks() {
        final List<Workspace.Mapping> mappings1 = new ArrayList<Workspace.Mapping>();
        mappings1.add(new Workspace.Mapping("one", "localOne", false));
        mappings1.add(new Workspace.Mapping("two", "localTwo", false));
        mappings1.add(new Workspace.Mapping("three", "localThree", false));
        final List<Workspace.Mapping> mappings2 = new ArrayList<Workspace.Mapping>();
        mappings2.add(new Workspace.Mapping("one", "localOne", false));
        mappings2.add(new Workspace.Mapping("two", "localTwo", true));
        mappings2.add(new Workspace.Mapping("three", "localThree", false));
        Assert.assertEquals(true, WorkspaceHelper.areMappingsDifferent(mappings1, mappings2));
        Assert.assertEquals(true, WorkspaceHelper.areMappingsDifferent(mappings2, mappings1));
    }

    @Test
    public void testAreMappingsDifferent_withWorkspaces_basic() {
        final List<Workspace.Mapping> mappings = new ArrayList<Workspace.Mapping>();
        mappings.add(new Workspace.Mapping("one", "two", false));
        final List<Workspace.Mapping> mappings2 = new ArrayList<Workspace.Mapping>();
        mappings2.add(new Workspace.Mapping("one", "two", false));
        final Workspace workspace1 = new Workspace("server1", "ws1", "computer", "owner", "comment1", mappings);
        final Workspace workspace2 = new Workspace("server1", "ws1", "computer", "owner", "comment1", mappings2);
        Assert.assertEquals(false, WorkspaceHelper.areMappingsDifferent(workspace1, workspace2));
    }

    @Test
    public void testIsOneLevelMapping() {
        Assert.assertEquals(false, WorkspaceHelper.isOneLevelMapping(null));
        Assert.assertEquals(false, WorkspaceHelper.isOneLevelMapping(""));
        Assert.assertEquals(true, WorkspaceHelper.isOneLevelMapping("/*"));
        Assert.assertEquals(true, WorkspaceHelper.isOneLevelMapping("$/project/*"));
        Assert.assertEquals(false, WorkspaceHelper.isOneLevelMapping("$/project*"));
        Assert.assertEquals(false, WorkspaceHelper.isOneLevelMapping("$/project*/"));
        Assert.assertEquals(false, WorkspaceHelper.isOneLevelMapping("$/project/"));
    }

    @Test
    public void testGetOneLevelServerPath() {
        Assert.assertEquals("/*", WorkspaceHelper.getOneLevelServerPath(null));
        Assert.assertEquals("/*", WorkspaceHelper.getOneLevelServerPath(""));
        Assert.assertEquals("$/*", WorkspaceHelper.getOneLevelServerPath("$/*"));
        Assert.assertEquals("$/project/*", WorkspaceHelper.getOneLevelServerPath("$/project"));
        Assert.assertEquals("$/project/*", WorkspaceHelper.getOneLevelServerPath("$/project/"));
        Assert.assertEquals("$/project/*", WorkspaceHelper.getOneLevelServerPath("$/project/*"));

        // If it is already a one level mapping the exact same string should be returned
        final String test = "$/project/*";
        Assert.assertSame(test, WorkspaceHelper.getOneLevelServerPath(test));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNormalizedServerPath_null() {
        WorkspaceHelper.getNormalizedServerPath(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNormalizedServerPath_empty() {
        WorkspaceHelper.getNormalizedServerPath("");
    }

    @Test
    public void testGetNormalizedServerPath_basic() {
        Assert.assertEquals("$/", WorkspaceHelper.getNormalizedServerPath("$/*"));
        Assert.assertEquals("$/project", WorkspaceHelper.getNormalizedServerPath("$/project"));
        Assert.assertEquals("$/project/", WorkspaceHelper.getNormalizedServerPath("$/project/"));
        Assert.assertEquals("$/project/", WorkspaceHelper.getNormalizedServerPath("$/project/*"));
    }

    @Test
    public void testGetMappingsToRemove_none() {
        final List<Workspace.Mapping> mappings1 = new ArrayList<Workspace.Mapping>();
        mappings1.add(new Workspace.Mapping("one", "localOne", false));
        mappings1.add(new Workspace.Mapping("two", "localTwo", false));
        mappings1.add(new Workspace.Mapping("three", "localThree", false));
        final List<Workspace.Mapping> mappings2 = new ArrayList<Workspace.Mapping>();
        mappings2.add(new Workspace.Mapping("one", "local1", false));
        mappings2.add(new Workspace.Mapping("two", "local2", true));
        mappings2.add(new Workspace.Mapping("three", "local3", false));
        final Workspace workspace1 = new Workspace("server1", "ws1", "computer", "owner", "comment1", mappings1);
        final Workspace workspace2 = new Workspace("server1", "ws2", "computer", "owner", "comment2", mappings2);
        Assert.assertEquals(0, WorkspaceHelper.getMappingsToRemove(workspace1, workspace2).size());
        Assert.assertEquals(0, WorkspaceHelper.getMappingsToRemove(workspace2, workspace1).size());
    }

    @Test
    public void testGetMappingsToRemove_one() {
        final List<Workspace.Mapping> mappings1 = new ArrayList<Workspace.Mapping>();
        mappings1.add(new Workspace.Mapping("one", "localOne", false));
        mappings1.add(new Workspace.Mapping("two", "localTwo", false));
        mappings1.add(new Workspace.Mapping("three", "localThree", false));
        final List<Workspace.Mapping> mappings2 = new ArrayList<Workspace.Mapping>();
        mappings2.add(new Workspace.Mapping("one", "local1", false));
        mappings2.add(new Workspace.Mapping("2", "local2", true));
        mappings2.add(new Workspace.Mapping("three", "local3", false));
        final Workspace workspace1 = new Workspace("server1", "ws1", "computer", "owner", "comment1", mappings1);
        final Workspace workspace2 = new Workspace("server1", "ws2", "computer", "owner", "comment2", mappings2);
        Assert.assertEquals(1, WorkspaceHelper.getMappingsToRemove(workspace1, workspace2).size());
        Assert.assertEquals(1, WorkspaceHelper.getMappingsToRemove(workspace2, workspace1).size());
        Assert.assertEquals("two", WorkspaceHelper.getMappingsToRemove(workspace1, workspace2).get(0).getServerPath());
        Assert.assertEquals("2", WorkspaceHelper.getMappingsToRemove(workspace2, workspace1).get(0).getServerPath());
    }

    @Test
    public void testGetMappingsToRemove_all() {
        final List<Workspace.Mapping> mappings1 = new ArrayList<Workspace.Mapping>();
        mappings1.add(new Workspace.Mapping("one", "localOne", false));
        mappings1.add(new Workspace.Mapping("two", "localTwo", false));
        mappings1.add(new Workspace.Mapping("three", "localThree", false));
        final List<Workspace.Mapping> mappings2 = new ArrayList<Workspace.Mapping>();
        mappings2.add(new Workspace.Mapping("1", "local1", false));
        mappings2.add(new Workspace.Mapping("2", "local2", true));
        mappings2.add(new Workspace.Mapping("3", "local3", false));
        final Workspace workspace1 = new Workspace("server1", "ws1", "computer", "owner", "comment1", mappings1);
        final Workspace workspace2 = new Workspace("server1", "ws2", "computer", "owner", "comment2", mappings2);
        Assert.assertEquals(3, WorkspaceHelper.getMappingsToRemove(workspace1, workspace2).size());
        Assert.assertEquals(3, WorkspaceHelper.getMappingsToRemove(workspace2, workspace1).size());
    }

    @Test
    public void testGetMappingsToChange() {
        final List<Workspace.Mapping> mappings1 = new ArrayList<Workspace.Mapping>();
        mappings1.add(new Workspace.Mapping("one", "localOne", false));
        mappings1.add(new Workspace.Mapping("two", "localTwo", false));
        mappings1.add(new Workspace.Mapping("three", "localThree", false));
        final List<Workspace.Mapping> mappings2 = new ArrayList<Workspace.Mapping>();
        mappings2.add(new Workspace.Mapping("1", "local1", false));
        mappings2.add(new Workspace.Mapping("2", "local2", true));
        mappings2.add(new Workspace.Mapping("3", "local3", false));
        final Workspace workspace1 = new Workspace("server1", "ws1", "computer", "owner", "comment1", mappings1);
        final Workspace workspace2 = new Workspace("server1", "ws2", "computer", "owner", "comment2", mappings2);
        Assert.assertEquals(3, WorkspaceHelper.getMappingsToChange(workspace1, workspace2).size());
        Assert.assertEquals(3, WorkspaceHelper.getMappingsToChange(workspace2, workspace1).size());

    }

}
