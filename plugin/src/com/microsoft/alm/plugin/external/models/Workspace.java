// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class represents the properties of a local TFVC workspace.
 */
public class Workspace {
    private final String name;
    private final String computer;
    private final String owner;
    private final String comment;
    private final String server;
    private final List<Mapping> mappings;

    public Workspace(final String server, final String name, final String computer, final String owner,
                     final String comment, final List<Mapping> mappings) {
        this.server = server;
        this.name = name;
        this.computer = computer;
        this.owner = owner;
        this.comment = comment;
        this.mappings = new ArrayList<Mapping>(mappings);
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

    public List<Mapping> getMappings() {
        return Collections.unmodifiableList(mappings);
    }

    public static class Mapping {
        private final String serverPath;
        private final String localPath;
        private final boolean isCloaked;

        public Mapping(final String serverPath, final String localPath, final boolean isCloaked) {
            this.serverPath = serverPath;
            this.localPath = localPath;
            this.isCloaked = isCloaked;
        }

        public String getLocalPath() {
            return localPath;
        }

        public String getServerPath() {
            return serverPath;
        }

        public boolean isCloaked() {
            return isCloaked;
        }
    }
}
