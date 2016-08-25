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

    /**
     * This class is used by the Workspace commands when creating or updating a workspace.
     */
    public enum FileTime {
        CURRENT,
        CHECKIN;

        @Override
        public String toString() {
            if (this == FileTime.CHECKIN) {
                return "checkin";
            } else {
                return "current";
            }
        }
    }

    /**
     * This class is used by the Workspace commands when creating or updating a workspace.
     */
    public enum Permission {
        PRIVATE,
        PUBLIC_LIMITED,
        PUBLIC;

        @Override
        public String toString() {
            if (this == Permission.PRIVATE) {
                return "Private";
            } else if (this == Permission.PUBLIC) {
                return "Public";
            } else {
                return "PublicLimited";
            }
        }
    }

    /**
     * This class represents a "working folder" in a workspace that maps a local path to a server path.
     */
    public static class Mapping {
        private final String serverPath;
        private final String localPath;
        private final boolean cloaked;

        public Mapping(final String serverPath, final String localPath, final boolean isCloaked) {
            this.serverPath = serverPath;
            this.localPath = localPath;
            this.cloaked = isCloaked;
        }

        public String getLocalPath() {
            return localPath;
        }

        public String getServerPath() {
            return serverPath;
        }

        public boolean isCloaked() {
            return cloaked;
        }
    }
}
