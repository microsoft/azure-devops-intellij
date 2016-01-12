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
            final String intellijExe = Advapi32Util.registryGetStringValue(WinReg.HKEY_CLASSES_ROOT, INTELLIJ_KEY, StringUtils.EMPTY);
            final String command = createCommand(intellijExe);

            if (isValidExe(command) && !checkIfKeysExistAndMatch(command)) {
                final File regeditFile = createRegeditFile(command);
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
                Advapi32Util.registryGetStringValue(WinReg.HKEY_CLASSES_ROOT, VSOI_KEY, StringUtils.EMPTY).equals(intellijExe)) {
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
        final File script = File.createTempFile("CreateKeys", ".reg");
        final FileWriter fileWriter = new FileWriter(script);
        final BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        try {
            bufferedWriter.write(
                    "Windows Registry Editor Version 5.00\r\n\r\n" +
                            "[-HKEY_CLASSES_ROOT\\vsoi]\r\n\r\n" +
                            "[HKEY_CLASSES_ROOT\\vsoi]\r\n" +
                            "\"URL Protocol\"=\"\"\r\n\r\n" +
                            "[HKEY_CLASSES_ROOT\\vsoi\\Shell\\Open\\Command]\r\n" +
                            "\"\"=\"" + intellijExe.replace("\\", "\\\\").replace("\"", "\\\"") + "\"");
        } finally {
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
        }
        return script;
    }

    /**
     * Finds if the exe path is valid
     *
     * @param path
     * @return if exe exists
     */
    protected static boolean isValidExe(final String path) {
        final Pattern exeExtension = Pattern.compile(".*.exe");
        final Matcher exePath = exeExtension.matcher(path);
        if (exePath.find() && new File(exePath.group(0)).isFile()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Take the IntelliJ exe command and add the vsts command to it. Example:
     * In: C:\Program Files\JetBRains\IntelliJ IDEA 14.1.4\bin\idea.exe "%1"
     * Out: C:\Program Files\JetBRains\IntelliJ IDEA 14.1.4\bin\idea.exe vsts "%1"
     *
     * @param exePath
     * @return IntelliJ command with vsts command added
     */
    protected static String createCommand(final String exePath) {
        int index = exePath.indexOf(".exe ");
        if (index != -1) {
            // add 4 to index to account for .exe
            index = index + 4;
            return exePath.substring(0, index) + " vsts " + exePath.substring(index + 1);
        } else {
            // command could not be created because .exe file could not be found
            return StringUtils.EMPTY;
        }
    }
}
