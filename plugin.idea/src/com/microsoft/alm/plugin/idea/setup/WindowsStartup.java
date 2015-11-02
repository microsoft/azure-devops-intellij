// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.setup;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.platform.win32.Win32Exception;

/**
 * This class contains the startup steps for when the plugin is launched from a Windows OS. Registry keys must be checked and created if missing.
 */
public final class WindowsStartup {
    private static final Logger logger = LoggerFactory.getLogger(WindowsStartup.class);
    public static final String VSOI_KEY = "vsoi\\Shell\\Open\\Command";
    private static final String INTELLIJ_KEY = "IntelliJIdeaProjectFile\\shell\\open\\command";

    /**
     * Setup windows specific configurations upon plugin launch
     */
    public static void startup() {
        try {
            // get most up-to-date IntelliJ exe
            final String intellijExe = Advapi32Util.registryGetStringValue(WinReg.HKEY_CLASSES_ROOT, INTELLIJ_KEY, "");

            if (!StringUtils.isEmpty(getValidExe(intellijExe)) && !checkIfKeysExistAndMatch(intellijExe)) {
                final File regeditFile = createRegeditFile(intellijExe);
                launchElevatedCreation(regeditFile.getPath());
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
     * Check if vsoi registry keys exist already and matches current IntelliJ exe
     *
     * @param intellijExe
     * @return if keys exists or not
     */
    protected static boolean checkIfKeysExistAndMatch(final String intellijExe) {
        if (Advapi32Util.registryKeyExists(WinReg.HKEY_CLASSES_ROOT, VSOI_KEY) &&
                Advapi32Util.registryValueExists(WinReg.HKEY_CLASSES_ROOT,  VSOI_KEY, intellijExe)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Run script to launch elevated process to create registry keys
     *
     * @param regeditFilePath
     */
    private static void launchElevatedCreation(final String regeditFilePath) {
        try {
            final String[] cmd = {"cmd", "/C", "regedit", "/s", regeditFilePath};
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
     * @param intellijExe
     * @return script file
     * @throws IOException
     */
    protected static File createRegeditFile(final String intellijExe) throws IOException {
        final String exePath = getValidExe(intellijExe); //TODO remove line once trimming arguments isn't needed
        final File script = File.createTempFile("CreateKeys" , ".reg");
        final FileWriter fileWriter = new FileWriter(script);
        final BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        try {
            bufferedWriter.write(
                    "Windows Registry Editor Version 5.00\r\n\r\n" +
                            "[-HKEY_CLASSES_ROOT\\vsoi]\r\n\r\n" +
                            "[HKEY_CLASSES_ROOT\\vsoi]\r\n" +
                            "\"URL Protocol\"=\"\"\r\n\r\n" +
                            "[HKEY_CLASSES_ROOT\\vsoi\\Shell\\Open\\Command]\r\n" +
                            "\"\"=\"" + exePath.replace("\\", "\\\\") + "\"");
        } finally {
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
        }
        return script;
    }

    /**
     * Finds a valid exe path and removes the argument parameters after the exe path
     * TODO change this to boolean return once arguments can be passed and not trimmed
     *
     * @param path
     * @return exe path only
     */
    protected static String getValidExe(final String path) {
        final Pattern exeExtension = Pattern.compile(".*.exe");
        final Matcher exePath = exeExtension.matcher(path);
        if (exePath.find() && new File(exePath.group(0)).isFile()) {
            return exePath.group(0);
        } else {
            return "";
        }
    }
}
