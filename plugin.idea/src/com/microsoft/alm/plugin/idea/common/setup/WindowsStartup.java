// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.setup;

import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinReg;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This class contains the startup steps for when the plugin is launched from a Windows OS. Registry keys must be checked and created if missing.
 */
public final class WindowsStartup {
    private static final Logger logger = LoggerFactory.getLogger(WindowsStartup.class);
    public static final String VSOI_KEY = "SOFTWARE\\Classes\\vsoi\\Shell\\Open\\Command";
    protected static final String CMD_NAME = "vsts.cmd";
    protected static final String WIN_DIR = "win";

    /**
     * Setup windows specific configurations upon plugin launch
     */
    public static void startup() {
        try {
            final String cmdPath = IdeaHelper.getResourcePath(WindowsStartup.class.getResource("/"), CMD_NAME, WIN_DIR);
            final File cmdFile = new File(cmdPath);

            if (cmdFile.exists() && doesKeyNeedUpdated(cmdFile)) {
                final File regeditFile = createRegeditFile(cmdFile);
                launchCreation(regeditFile.getPath());
                regeditFile.delete();
            }
        } catch (IOException e) {
            logger.warn("An IOException was encountered while creating/writing to the Regedit file: {}", e.getMessage());
        } catch (Win32Exception e) {
            logger.warn("A Win32Exception was encountered while trying to get IntelliJ's registry key: {}", e.getMessage());
        } catch (Exception e) {
            logger.warn("An exception was encountered while trying to create vsoi registry key: {}", e.getMessage());
        }
    }

    /**
     * Check if registry keys exist and if the cmd file the key contains matches the latest cmd file
     *
     * @param newCmd
     * @return if keys exists or not
     */
    protected static boolean doesKeyNeedUpdated(final File newCmd) throws IOException {
        try {
            final String existingKey = Advapi32Util.registryGetStringValue(WinReg.HKEY_CURRENT_USER, VSOI_KEY, StringUtils.EMPTY);
            final File existingCmd = new File(existingKey.replace("\"%1\"", "").trim());
            if (!existingCmd.exists()) {
                logger.debug("The registry key needs updated because the old key cmd file doesn't exist.");
                return true;
            }

            if (existingCmd.getPath().equalsIgnoreCase(newCmd.getPath()) || FileUtils.contentEquals(existingCmd, newCmd)) {
                logger.debug("The registry key does not need updated because {}", existingCmd.getPath().equalsIgnoreCase(newCmd.getPath()) ?
                        "the file paths are the same" : "the contents of the files are the same.");
                return false;
            }

            // use the newest cmd file so update if existing cmd file is older
            // TODO: version cmd file if we continually iterate on it so we chose the correct file more reliably
            logger.debug("The existing cmd file is {} old and the the cmd file is {} old", existingCmd.lastModified(), newCmd.lastModified());
            return existingCmd.lastModified() < newCmd.lastModified() ? true : false;
        } catch (Win32Exception e) {
            // Error occurred reading the registry (possible key doesn't exist or is empty) so update just to be safe
            logger.debug("There was an issue reading the registry so updating the key to be safe.");
            return true;
        }
    }

    /**
     * Run script to create registry keys
     *
     * @param regeditFilePath
     */
    private static void launchCreation(final String regeditFilePath) {
        try {
            final String[] cmd = {"cmd", "/C", "reg.exe", "import", regeditFilePath};
            final ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            final Process process = processBuilder.start();
            process.waitFor();
        } catch (IOException e) {
            logger.warn("Running regedit encountered an IOException: {}", e.getMessage());
        } catch (Exception e) {
            logger.warn("Waiting for the process to execute resulted in an error: " + e.getMessage());
        }
    }

    /**
     * Create VB script to run KeyCreator application as elevated
     *
     * @param newCmd
     * @return script file
     * @throws IOException
     */
    protected static File createRegeditFile(final File newCmd) throws IOException {
        final File script = File.createTempFile("CreateKeys", ".reg");
        final FileWriter fileWriter = new FileWriter(script);
        final BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        try {
            bufferedWriter.write(
                    "Windows Registry Editor Version 5.00\r\n\r\n" +
                            "[-HKEY_CURRENT_USER\\SOFTWARE\\Classes\\vsoi]\r\n\r\n" +
                            "[HKEY_CURRENT_USER\\SOFTWARE\\Classes\\vsoi]\r\n" +
                            "\"URL Protocol\"=\"\"\r\n\r\n" +
                            "[HKEY_CURRENT_USER\\SOFTWARE\\Classes\\vsoi\\Shell\\Open\\Command]\r\n" +
                            "\"\"=\"" + newCmd.getPath().replace("\\", "\\\\") + " \\\"%1\\\" \"");
        } finally {
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
        }
        return script;
    }
}
