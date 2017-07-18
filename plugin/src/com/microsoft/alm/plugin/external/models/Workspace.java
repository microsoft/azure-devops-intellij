// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.models;

import jersey.repackaged.com.google.common.base.Objects;
import org.apache.commons.lang.StringUtils;

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
     * Compare only name, computer, owner, and server to see if it's the same workspace
     * The comment and mappings don't make the workspace unique
     *
     * @param object
     * @return
     */
    @Override
    public boolean equals(final Object object) {
        if (object == null || !(object instanceof Workspace)) {
            return false;
        }

        final Workspace workspace = (Workspace) object;
        if (!StringUtils.equals(this.name, workspace.getName())) {
            return false;
        }
        if (!StringUtils.equals(this.computer, workspace.getComputer())) {
            return false;
        }
        if (!StringUtils.equals(this.owner, workspace.getOwner())) {
            return false;
        }
        if (!StringUtils.equals(this.server, workspace.getServer())) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, computer, owner, server);
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
