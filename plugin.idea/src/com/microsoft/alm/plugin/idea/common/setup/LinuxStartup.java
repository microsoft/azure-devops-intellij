// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.setup;

import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

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
            createDesktopFileAndUpdateDatabase(script, new File(USER_HOME, VSOI_DESKTOP_FILE_PATH));
        } catch (IOException e) {
            logger.warn("An IOException was caught while trying to run the Linux startup steps: {}", e.getMessage());
        } catch (InterruptedException e) {
            logger.warn("An InterruptedException was caught while waiting for the database desktop to update: {}", e.getMessage());
        } catch (Exception e) {
            logger.warn("An Exception was caught while trying to run the Linux startup steps", e);
        }
    }

    /**
     * Create .desktop file to register the protocol handler and update desktop database
     *
     * @param scriptFile
     * @throws IOException
     */
    protected static void createDesktopFileAndUpdateDatabase(final File scriptFile, final File desktopFile) throws IOException, InterruptedException {
        final String desktopFileContent =
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
                "Categories=Network;Application;";
        if (isFileUpToDate(desktopFile, desktopFileContent)) {
            return;
        }

        writeFile(desktopFile, desktopFileContent);

        // Run process to update local desktop database to include the new protocol handler
        final Process process = Runtime.getRuntime().exec(UPDATE_LOCAL_DESKTOP_DB_CMD);
        process.waitFor();
        logger.debug("The return code for executing update-desktop-database was {}", process.exitValue());
    }


    protected static void writeFile(final File file, final String content) throws IOException {
        file.getParentFile().mkdirs();

        final OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
        try {
            writer.write(content);
        } finally {
            writer.close();
        }
    }

    protected static boolean isFileUpToDate(final File file, final String expectedContent) throws IOException {
        if (file.exists() && readFileAsString(file).equals(expectedContent)) {
            logger.info("file content is up-to-date: " + file);
            return true;
        }

        logger.info("file content needs updating: " + file);
        return false;
    }

    protected static String readFileAsString(File filePath) throws IOException {
        DataInputStream dis = new DataInputStream(new FileInputStream(filePath));
        try {
            long len = filePath.length();
            byte[] bytes = new byte[(int) len];
            dis.readFully(bytes);
            return new String(bytes, "UTF-8");
        } finally {
            dis.close();
        }
    }
}

