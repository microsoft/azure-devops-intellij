// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.Date;

public class SystemHelper {
    private static Logger logger = LoggerFactory.getLogger(SystemHelper.class);

    /**
     * Gets the computer name
     * @return local host name if found, null otherwise
     */
    public static String getComputerName() {
        try {
            java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();
            return localMachine.getHostName();
        } catch (UnknownHostException e) {
            logger.warn("getComputerName failed", e);
        }
        return null;
    }

    /**
     * Get date time string
     * @return Friendly string representation of current date time
     */
    public static String getCurrentDateTime() {
        final Date date = new Date();
        final String friendlyDate = String.format("%tc", date);
        return friendlyDate;
    }
}
