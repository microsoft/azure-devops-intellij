// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.models;

import com.microsoft.alm.common.utils.ArgumentHelper;
import org.apache.commons.lang.StringUtils;

/**
 * This VersionSpec represents all the variations of a version string that TFVC supports.
 * Examples:
 * C123 - changeset 123
 * D12-12-2016T12:12 - Date time of the form mm-dd-yyyyTHH:MM
 * LmyLabel - Label called myLabel
 * WmyWorkspace - version based on the local workspace called myWorkspace
 * T - Latest version (or Tip)
 */
public class VersionSpec {
    public enum Type {
        Latest, Changeset, DateTime, Label, Workspace;

        @Override
        public String toString() {
            if (this == Changeset) return "C";
            else if (this == DateTime) return "D";
            else if (this == Label) return "L";
            else if (this == Workspace) return "W";
            return "T";
        }

        public static Type fromString(final String typeString) {
            if (StringUtils.isNotEmpty(typeString)) {
                switch (typeString.toUpperCase().charAt(0)) {
                    case 'C':
                        return Changeset;
                    case 'D':
                        return DateTime;
                    case 'L':
                        return Label;
                    case 'W':
                        return Workspace;
                }
            }

            return Latest;
        }
    }

    /**
     * This class represents a VersionSpec range.
     * This is generally denoted by a string like "versionSpec1~versionSpec2"
     */
    public static class Range {
        private static final char RANGE_SEPARATOR = '~';

        private final VersionSpec start;
        private final VersionSpec end;

        public Range(final VersionSpec start, final VersionSpec end) {
            ArgumentHelper.checkNotNull(start, "start");
            ArgumentHelper.checkNotNull(end, "end");
            this.start = start;
            this.end = end;
        }

        public static Range create(final String rangeString) {
            final String[] parts = StringUtils.split(rangeString, RANGE_SEPARATOR);
            if (parts.length == 2) {
                return new Range(VersionSpec.create(parts[0]), VersionSpec.create(parts[1]));
            } else if (parts.length == 1) {
                return new Range(VersionSpec.create(parts[0]), VersionSpec.create(parts[0]));
            }
            throw new IllegalArgumentException("rangeString");
        }

        public VersionSpec getStart() {
            return start;
        }

        public VersionSpec getEnd() {
            return end;
        }

        @Override
        public String toString() {
            return start.toString() + RANGE_SEPARATOR + end.toString();
        }
    }

    public static final VersionSpec LATEST = new VersionSpec(Type.Latest, StringUtils.EMPTY);

    private final Type type;
    private final String value;

    protected VersionSpec(final Type type, final String value) {
        this.type = type;
        this.value = value;
    }

    public static VersionSpec create(final String versionSpecString) {
        if (StringUtils.isEmpty(versionSpecString)) {
            return LATEST;
        }

        final Type type = Type.fromString(versionSpecString);
        // If the type is latest return the constant so comparisons will work
        if (type == Type.Latest) {
            return LATEST;
        }
        return new VersionSpec(type, versionSpecString.substring(1));
    }

    public static VersionSpec create(final int changeset) {
        if (changeset <= 0) {
            throw new IndexOutOfBoundsException("changeset");
        }
        return new VersionSpec(Type.Changeset, Integer.toString(changeset));
    }

    public Type getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return type.toString() + value;
    }
}