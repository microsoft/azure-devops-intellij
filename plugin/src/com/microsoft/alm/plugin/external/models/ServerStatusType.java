// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.models;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server status types
 */
public enum ServerStatusType {
    ADD,
    RENAME,
    EDIT,
    DELETE,
    UNDELETE,
    RENAME_EDIT,
    UNKNOWN;

    public static final Logger logger = LoggerFactory.getLogger(ServerStatusType.class);

    /**
     * Figure out server status type from string
     *
     * @param statusString
     * @return
     */
    public static ServerStatusType getServerStatusType(final String statusString) {
        if (StringUtils.equalsIgnoreCase(statusString, ADD.name())) {
            return ADD;
        } else if (StringUtils.equalsIgnoreCase(statusString, DELETE.name())) {
            return DELETE;
        } else if (StringUtils.equalsIgnoreCase(statusString, EDIT.name())) {
            return EDIT;
        } else if (StringUtils.equalsIgnoreCase(statusString, RENAME.name())) {
            return RENAME;
        } else if (StringUtils.equalsIgnoreCase(statusString, UNDELETE.name())) {
            return UNDELETE;
        } else if (StringUtils.containsIgnoreCase(statusString, RENAME.name()) && StringUtils.containsIgnoreCase(statusString, EDIT.name())) {
            return RENAME_EDIT;
        } else {
            logger.error("Undocumented status from server: " + statusString);
            return UNKNOWN;
        }
    }
}