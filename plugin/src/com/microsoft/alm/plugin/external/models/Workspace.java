// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.models;

/**
 * This class represents the properties of a local TFVC workspace.
 */
public class Workspace {
    private final String name;
    private final String computer;
    private final String owner;
    private final String comment;
    private final String server;

    public Workspace(final String server, final String name, final String computer, final String owner, final String comment) {
        this.server = server;
        this.name = name;
        this.computer = computer;
        this.owner = owner;
        this.comment = comment;
    }

    public String getServer() {
        return server;
    }

    public String getName() {
        return name;
    }

    public String getComputer() {
        return computer;
    }

    public String getOwner() {
        return owner;
    }

    public String getComment() {
        return comment;
    }
}
