// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.models;

import com.google.common.base.Objects;
import com.microsoft.alm.common.utils.UrlHelper;
import org.apache.commons.lang.StringUtils;

import java.net.URI;
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
    private final Location location;

    public Workspace(final String server, final String name, final String computer, final String owner,
                     final String comment, final List<Mapping> mappings) {
        this(server, name, computer, owner, comment, mappings, Location.LOCAL);
    }

    public Workspace(final String server, final String name, final String computer, final String owner,
                     final String comment, final List<Mapping> mappings, final Location location) {
        this.server = server;
        this.name = name;
        this.computer = computer;
        this.owner = owner;
        this.comment = comment;
        this.mappings = new ArrayList<Mapping>(mappings);
        this.location = location;
    }

    /**
     * @return the presentable string for server name (the URL without URL escaping applied). For example, URL
     * "http://tfs/collection%20name" will be presented here as "http://tfs/collection name".
     */
    public String getServerDisplayName() {
        return server;
    }

    public URI getServerUri() {
        return UrlHelper.createUri(server);
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

    public Location getLocation() {
        return location;
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
        if (!StringUtils.equals(this.server, workspace.server)) {
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

    /**
     * This enum represents the location of the workspace or if it's unknown
     */
    public enum Location {
        LOCAL,
        SERVER,
        UNKNOWN;

        public static Location fromString(final String location) {
            if (StringUtils.equalsIgnoreCase(location, LOCAL.name())) {
                return LOCAL;
            } else if (StringUtils.equalsIgnoreCase(location, SERVER.name())) {
                return SERVER;
            } else {
                return UNKNOWN;
            }
        }

        /**
         * Converts this enum member to a string suitable for use in a command-line TFVC client.
         */
        public String toParameterString() {
            switch (this) {
                case LOCAL: return "local";
                case SERVER: return "server";
                default: return "unknown";
            }
        }
    }
}