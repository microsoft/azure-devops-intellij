// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.setup;

import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This class creates a Linux desktop file that installs the protocol handler on a Linux machine
 */
public class LinuxStartup {
    private static final Logger logger = LoggerFactory.getLogger(LinuxStartup.class);
    private static final String UPDATE_LOCAL_DESKTOP_DB_CMD = "update-desktop-database " + System.getProperty("user.home") + "/.local/share/applications/";
    private static final File USER_HOME = new File(System.getProperty("user.home"));
    private static final String VSOI_DESKTOP_FILE_PATH = ".local/share/applications/vsoi.desktop";
    private static final String SCRIPT_NAME = "vsts.sh";
    private static final String LINUX_DIR = "linux";

    /**
     * Setup Linux specific configurations upon plugin launch
     */
    public static void startup() {
        try {
            final File script = new File(IdeaHelper.getResourcePath(LinuxStartup.class.getResource("/"), SCRIPT_NAME, LINUX_DIR));
            IdeaHelper.setExecutablePermissions(script);
            createDesktopFile(script, new File(USER_HOME, VSOI_DESKTOP_FILE_PATH));

            // Run process to update local desktop database to include the new protocol handler
            final Process process = Runtime.getRuntime().exec(UPDATE_LOCAL_DESKTOP_DB_CMD);
            process.waitFor();
            logger.debug("The return code for executing update-desktop-database was {}", process.exitValue());
        } catch (IOException e) {
            logger.warn("An IOException was caught while trying to run the Linux startup steps: {}", e.getMessage());
        } catch (InterruptedException e) {
            logger.warn("An InterruptedException was caught while waiting for the database desktop to update: {}", e.getMessage());
        } catch (Exception e) {
            logger.warn("An Exception was caught while trying to run the Linux startup steps", e);
        }
    }

    /**
     * Create .desktop file to register the protocol handler
     *
     * @param scriptFile
     * @return script file
     * @throws IOException
     */
    protected static File createDesktopFile(final File scriptFile, final File desktopFile) throws IOException {
        final FileWriter fileWriter = new FileWriter(desktopFile);
        final BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        try {
            bufferedWriter.write(
                    "[Desktop Entry]\n" +
                            "Name=VSTS Protocol Handler\n" +
                            "Comment=Custom protocol handler for the IntelliJ VSTS plugin\n" +
                            "Exec=" + scriptFile.getAbsolutePath() + " %u\n" +
                            "Icon=\n" + //TODO: add icon
                            "Terminal=False\n" +
                            "Type=Application\n" +
                            "X-MultipleArgs=True\n" +
                            "MimeType=x-scheme-handler/vsoi\n" +
                            "Encoding=UTF-8\n" +
                            "Categories=Network;Application;");
        } finally {
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
        }
        return desktopFile;
    }
}

