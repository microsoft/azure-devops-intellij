// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.models;

import org.apache.commons.lang.StringUtils;

/**
 * This class represents the version of a command line tool.
 */
public class ToolVersion {
    public static final ToolVersion UNKNOWN = new ToolVersion(StringUtils.EMPTY);

    private final int major;
    private final int minor;
    private final int revision;
    private final String build;

    /**
     * constructor that takes a version string and creates a ToolVersion object by parsing the string.
     * Ex. 14.0.3.201603291047
     */
    public ToolVersion(final String versionString) {
        final String[] parts = StringUtils.split(versionString, '.');
        major = getIntegerPart(parts, 0, 0);
        minor = getIntegerPart(parts, 1, 0);
        revision = getIntegerPart(parts, 2, 0);
        if (parts.length > 3) {
            build = parts[3];
        } else {
            build = StringUtils.EMPTY;
        }
    }

    public int compare(final ToolVersion other) {
        if (this.getMajor() == other.getMajor()) {
            if (this.getMinor() == other.getMinor()) {
                if (this.getRevision() == other.getRevision()) {
                    return 0;
                } else {
                    return this.getRevision() - other.getRevision();
                }
            } else {
                return this.getMinor() - other.getMinor();
            }
        } else {
            return this.getMajor() - other.getMajor();
        }
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getRevision() {
        return revision;
    }

    public String getBuild() {
        return build;
    }

    @Override
    public String toString() {
        final String version = String.format("%d.%d.%d", getMajor(), getMinor(), getRevision());
        if (StringUtils.isNotEmpty(getBuild())) {
            return version + "." + getBuild();
        } else {
            return version;
        }

    }

    private int getIntegerPart(final String[] parts, final int index, final int defaultValue) {
        if (parts == null || index >= parts.length) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(parts[index]);
        } catch (final NumberFormatException ex) {
            return defaultValue;
        }
    }
}
