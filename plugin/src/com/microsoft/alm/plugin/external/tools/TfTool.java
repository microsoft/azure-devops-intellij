// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.tools;

import com.microsoft.alm.client.utils.StringUtil;
import com.microsoft.alm.common.utils.SystemHelper;
import com.microsoft.alm.plugin.external.commands.TfVersionCommand;
import com.microsoft.alm.plugin.external.exceptions.ToolBadExitCodeException;
import com.microsoft.alm.plugin.external.exceptions.ToolException;
import com.microsoft.alm.plugin.external.exceptions.ToolVersionException;
import com.microsoft.alm.plugin.external.models.ToolVersion;
import com.microsoft.alm.plugin.services.PluginServiceProvider;
import com.microsoft.alm.plugin.services.PropertyService;
import com.sun.jna.Platform;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * This is a static class that provides helper methods for running the TF command line.
 */
public class TfTool {
    private static final Logger logger = LoggerFactory.getLogger(TfTool.class);

    private static final Object cacheLock = new Object();

    private static final String[] TF_WINDOWS_PROGRAMS = {"tf.exe", "tf.bat", "tf.cmd"};
    private static final String[] TF_OTHER_PROGRAMS = {"tf", "tf.sh"};
    private static ToolVersion cachedVersion = null;
    public static final ToolVersion TF_MIN_VERSION = new ToolVersion("14.0.3");
    public static final String TF_DIRECTORY_PREFIX = "TEE-CLC";
    private static final String PATH_ENV = "PATH";
    private static final String TF_HOME_ENV = "TF_HOME";
    private static final String SYSTEM_DRIVE = SystemHelper.getEnvironmentVariable("SystemDrive");

    // standard unix paths to check (Git uses similar ones for both Linux/Unix/Mac)
    private static final File[] UNIX_PATHS = {
            new File("/opt/local/opt"),
            new File("/opt"),
            new File("/sbin"),
            new File("/usr/share"),
            new File("/usr/sbin"),
            new File("/usr/local/opt"),
            new File("/usr/local/share"),
            new File("/usr/local")};

    private static File[] PROGRAM_FILE_PATHS = {
            new File(SYSTEM_DRIVE, "Program Files"),
            new File(SYSTEM_DRIVE, "Program Files (x86)"),
            new File(SYSTEM_DRIVE, "Program Files\\Java")
    };

    /**
     * This method returns the path to the TF command line program.
     * It relies on the vsts-settings file.
     * This method will throw if no valid path exists.
     */
    public static String getValidLocation() {
        // Get the tf command location from file
        final String location = getLocation();
        if (StringUtil.isNullOrEmpty(location)) {
            // tfHome property not set
            throw new ToolException(ToolException.KEY_TF_HOME_NOT_SET);
        }

        final String[] filenames = Platform.isWindows() ? TF_WINDOWS_PROGRAMS : TF_OTHER_PROGRAMS;

        // Verify the path leads to a tf command and exists
        for (final String filename : filenames) {
            if (StringUtils.endsWith(location, filename) && (new File(location)).exists()) {
                return location;
            }
        }

        // The saved location does not point to the tf command
        throw new ToolException(ToolException.KEY_TF_EXE_NOT_FOUND);
    }

    /**
     * This method returns the path to the TF command line program that was saved in the properties file
     */
    public static String getLocation() {
        // Get the location of the TF command from the properties files
        return PluginServiceProvider.getInstance().getPropertyService().getProperty(PropertyService.PROP_TF_HOME);
    }

    /**
     * Determines the version of the TF command being used and throws if the version is too small.
     */
    public static void checkVersion() {
        synchronized (cacheLock) {
            final TfVersionCommand command = new TfVersionCommand();
            cachedVersion = command.runSynchronously();
            if (cachedVersion.compare(TF_MIN_VERSION) < 0) {
                throw new ToolVersionException(cachedVersion, TF_MIN_VERSION);
            }
        }
    }

    @NotNull public static ToolVersion getToolVersion() {
        synchronized (cacheLock) {
            if (cachedVersion == null) {
                cachedVersion = new TfVersionCommand().runSynchronously();
            }
            return cachedVersion;
        }
    }

    /**
     * Use this method to check the TF exit code and throw an appropriate exception.
     */
    public static void throwBadExitCode(final int exitCode) {
        if (exitCode != 0) {
            // TODO distinguish between warnings and fatal errors
            throw new ToolBadExitCodeException(exitCode);
        }
    }

    /**
     * Try to find the tf command by checking common places based on the OS
     *
     * @return
     */
    public static String tryDetectTf() {
        final String[] exeNames = Platform.isWindows() ? TF_WINDOWS_PROGRAMS : TF_OTHER_PROGRAMS;
        final File[] filePaths = Platform.isWindows() ? PROGRAM_FILE_PATHS : UNIX_PATHS;
        return tryDetectTf(exeNames, filePaths);
    }

    protected static String tryDetectTf(final String[] exeNames, final File[] filePaths) {
        try {
            // try looking at env path
            String exe = checkTfPath(SystemHelper.getEnvironmentVariable(TF_HOME_ENV), exeNames);
            if (StringUtils.isNotEmpty(exe)) {
                logger.debug("Found exe by TF_HOME: " + exe);
                return exe;
            }

            // try looking at PATH
            exe = getExeFromPath(exeNames);
            if (StringUtils.isNotEmpty(exe)) {
                logger.debug("Found exe by PATH: " + exe);
                return exe;
            }

            exe = searchDirectories(filePaths, exeNames);
            if (StringUtils.isNotEmpty(exe)) {
                logger.debug("Found exe by Program Files: " + exe);
                return exe;
            }
        } catch (Exception e) {
            // swallow all errors since it's not essential we find the exe
            logger.warn("Error while detecting tf exe", e);
        }

        logger.warn("No tf command was detected");
        return null;
    }

    /**
     * Check if PATH contains the path to the tf command
     *
     * @return
     */
    private static String getExeFromPath(final String[] exeNames) {
        final String envPath = SystemHelper.getEnvironmentVariable(PATH_ENV);

        if (StringUtils.isNotEmpty(envPath)) {
            final String[] paths = envPath.split(";");
            for (final String path : paths) {
                final File filePath = new File(path);
                if (StringUtils.startsWith(filePath.getName(), TF_DIRECTORY_PREFIX) && filePath.isDirectory()) {
                    final String verifiedPath = checkTfPath(filePath.getPath(), exeNames);
                    if (verifiedPath != null) {
                        return verifiedPath;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Search thru given directories to find one of the given acceptable tf commands
     *
     * @param paths
     * @param exeNames
     * @return
     */
    private static String searchDirectories(final File[] paths, final String[] exeNames) {
        for (final File path : paths) {
            if (path.exists()) {
                for (final File subDirectory : path.listFiles()) {
                    if (StringUtils.startsWith(subDirectory.getName(), TF_DIRECTORY_PREFIX) && subDirectory.isDirectory()) {
                        final String verifiedPath = checkTfPath(subDirectory.getPath(), exeNames);
                        if (verifiedPath != null) {
                            return verifiedPath;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Checks if the given path contains one of the given acceptable tf commands
     *
     * @param exePath
     * @param exeNames
     * @return
     */
    private static String checkTfPath(final String exePath, final String[] exeNames) {
        if (StringUtils.isNotEmpty(exePath)) {
            for (final String filename : exeNames) {
                final File tfFile = new File(exePath, filename);
                if (tfFile.exists()) {
                    return tfFile.toString();
                }
            }
        }
        return null;
    }
}