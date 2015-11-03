// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.setup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

/**
 * This class runs a Mac application bundle that will install the protocol handler on a Mac
 */
public class MacStartup {
    private static final Logger logger = LoggerFactory.getLogger(MacStartup.class);
    protected static final String APP_NAME = "vsoi.app";
    protected static final String OS_X_DIR = "/osx/";
    private static final String ENCODING_SCHEME = "utf-8";
    private static final String APPLET_PATH = "Contents/MacOS/applet";
    private static final String OPEN_CMD = "open";

    /**
     * Setup Mac specific configurations upon plugin launch
     */
    public static void startup() {
        setupProtocolHandlerUri();
    }

    /**
     * Setup the protocol handler URI by calling an OSX app that runs an applescript which registers the URI
     */
    protected static void setupProtocolHandlerUri() {
        try {
            // setup protocol handler URI by running vsoi.app
            final String appPath = getAppPath(MacStartup.class.getResource("/"));
            setAppletPermissions(new File(appPath + APPLET_PATH));
            final Process process = Runtime.getRuntime().exec(new String[]{OPEN_CMD, appPath});
            logger.debug("The return code for executing {} was {}", APP_NAME, process.exitValue());
        } catch (UnsupportedEncodingException e) {
            logger.warn("An UnsupportedEncodingException was caught while trying to execute {}: {}", APP_NAME, e.getMessage());
        } catch (FileNotFoundException e) {
            logger.warn("A FileNotFoundException was caught while trying to execute {}: {}", APP_NAME, e.getMessage());
        } catch (IOException e) {
            logger.warn("An IOException was caught while trying to execute {}: {}", APP_NAME, e.getMessage());
        } catch (Exception e) {
            logger.warn("An Exception was caught while trying to find and execute {}: {}", APP_NAME, e.getMessage());
        }
    }

    /**
     * Find the path to the app whether it's installed for the idea or being run inside the idea
     *
     * @param appUrl the URL for vsoi.app
     * @return the path to the app
     * @throws UnsupportedEncodingException
     */
    protected static String getAppPath(final URL appUrl) throws UnsupportedEncodingException {
        // find location of the app
        String appPath = appUrl.getPath();
        appPath = URLDecoder.decode(appPath, ENCODING_SCHEME);

        // when running the plugin inside of the idea, the path to the app needs to be
        // manipulated to look in a different location than where the zip would find it
        if (appPath != null && !appPath.endsWith(APP_NAME + "/")) {
            appPath = appPath + OS_X_DIR + APP_NAME;
        }
        return appPath;
    }

    /**
     * Check if the applet file is executable and if not sets it to be executable. When the plugin is unzipped,
     * the permissions of the file is not persisted so that is why a check is needed.
     *
     * @param applet applet file for application
     * @throws FileNotFoundException
     */
    protected static void setAppletPermissions(final File applet) throws FileNotFoundException {
        if (!applet.exists()) {
            throw new FileNotFoundException(applet.getPath() + " not found while trying to set permissions.");
        }

        // set the applet to execute for all users
        if (!applet.canExecute()) {
            applet.setExecutable(true, false);
        }
    }
}