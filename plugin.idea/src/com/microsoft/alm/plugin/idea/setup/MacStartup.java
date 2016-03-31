// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.setup;

import com.microsoft.alm.plugin.idea.utils.IdeaHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * This class runs a Mac application bundle that will install the protocol handler on a Mac
 */
public class MacStartup {
    private static final Logger logger = LoggerFactory.getLogger(MacStartup.class);
    protected static final String APP_NAME = "vsoi.app/";
    protected static final String OS_X_DIR = "osx";
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
            final String appPath = IdeaHelper.getResourcePath(MacStartup.class.getResource("/"), APP_NAME, OS_X_DIR);
            IdeaHelper.setExecutablePermissions(new File(appPath + APPLET_PATH));
            final Process process = Runtime.getRuntime().exec(new String[]{OPEN_CMD, appPath});
            process.waitFor();
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
}