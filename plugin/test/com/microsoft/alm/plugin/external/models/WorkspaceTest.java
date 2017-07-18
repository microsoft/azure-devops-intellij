// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.models;

import com.microsoft.alm.plugin.AbstractTest;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by leantk on 7/14/17.
 */
public class WorkspaceTest extends AbstractTest {
    final String name = "name1";
    final String server = "http://server1";
    final String comment = "this is a comment";
    final String computer = "computer1";
    final List<Workspace.Mapping> mappings = Collections.singletonList(new Workspace.Mapping("$/", "/path/", false));
    final String owner = "owner1";

    @Test
    public void testConstructor() {
        final Workspace workspace = new Workspace(server, name, computer, owner, comment, mappings);

        assertEquals(server, workspace.getServer());
        assertEquals(name, workspace.getName());
        assertEquals(computer, workspace.getComputer());
        assertEquals(owner, workspace.getOwner());
        assertEquals(comment, workspace.getComment());
        assertEquals(mappings, workspace.getMappings());
    }

    @Test
    public void testEquals_True_All() {
        final Workspace workspace1 = new Workspace(server, name, computer, owner, comment, mappings);
        final Workspace workspace2 = new Workspace(server, name, computer, owner, comment, mappings);

        assertTrue(workspace1.equals(workspace2));
    }

    @Test
    public void testEquals_True_Comment() {
        final Workspace workspace1 = new Workspace(server, name, computer, owner, comment, mappings);
        final Workspace workspace2 = new Workspace(server, name, computer, owner, "comment is different", mappings);

        assertTrue(workspace1.equals(workspace2));
    }

    @Test
    public void testEquals_True_Mappings() {
        final Workspace workspace1 = new Workspace(server, name, computer, owner, comment, mappings);
        final Workspace workspace2 = new Workspace(server, name, computer, owner, comment, Collections.EMPTY_LIST);

        assertTrue(workspace1.equals(workspace2));
    }

    @Test
    public void testEquals_False_Null() {
        final Workspace workspace1 = new Workspace(server, name, computer, owner, comment, mappings);

        assertFalse(workspace1.equals(null));
    }

    @Test
    public void testEquals_False_NotWorkspace() {
        final Workspace workspace1 = new Workspace(server, name, computer, owner, comment, mappings);

        assertFalse(workspace1.equals(new Server("server", Collections.EMPTY_LIST)));
    }

    @Test
    public void testEquals_False_Server() {
        final Workspace workspace1 = new Workspace(server, name, computer, owner, comment, mappings);
        final Workspace workspace2 = new Workspace("http://server2", name, computer, owner, comment, mappings);

        assertFalse(workspace1.equals(workspace2));
    }

    @Test
    public void testEquals_False_Name() {
        final Workspace workspace1 = new Workspace(server, name, computer, owner, comment, mappings);
        final Workspace workspace2 = new Workspace(server, "name2", computer, owner, comment, mappings);

        assertFalse(workspace1.equals(workspace2));
    }

    @Test
    public void testEquals_False_Computer() {
        final Workspace workspace1 = new Workspace(server, name, computer, owner, comment, mappings);
        final Workspace workspace2 = new Workspace(server, name, "computer2", owner, comment, mappings);

        assertFalse(workspace1.equals(workspace2));
    }

    @Test
    public void testEquals_False_Owner() {
        final Workspace workspace1 = new Workspace(server, name, computer, owner, comment, mappings);
        final Workspace workspace2 = new Workspace(server, name, computer, "owner2", comment, mappings);

        assertFalse(workspace1.equals(workspace2));
    }
}