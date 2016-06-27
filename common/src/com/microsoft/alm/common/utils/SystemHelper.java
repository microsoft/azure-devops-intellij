// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.common.utils;

import com.sun.jna.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;

public class SystemHelper {
    private static Logger logger = LoggerFactory.getLogger(SystemHelper.class);
    private final static String COMPUTER_NAME = "computername";

    /**
     * Gets the computer name
     *
     * @return local host name if found, falls back to computername env variable otherwise
     */
    public static String getComputerName() {
        try {
            java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();
            return localMachine.getHostName();
        } catch (UnknownHostException e) {
            logger.warn("getComputerName failed", e);
            return System.getenv(COMPUTER_NAME);
        }
    }

    /**
     * Takes in a path and converts it to Unix standard if Windows OS is detected
     *
     * @param path
     * @return path with forward slashes
     */
    public static String getUnixPath(final String path) {
        if (Platform.isWindows()) {
            return path.replace("\\", "/");
        } else {
            return path;
        }
    }
}
