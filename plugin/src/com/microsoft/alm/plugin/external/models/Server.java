// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.models;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents the properties of a Server as given by the CLC
 */
public class Server {
    private final String name;
    private final List<Workspace> workspaces;

    public Server(final String name, final List<Workspace> workspaces) {
        this.name = name;
        this.workspaces = new ArrayList<Workspace>(workspaces);
    }

    public String getName() {
        return name;
    }

    public List<Workspace> getWorkspaces() {
        return workspaces;
    }
}