// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.setup;

import com.ice.jni.registry.NoSuchKeyException;
import com.ice.jni.registry.NoSuchValueException;
import com.ice.jni.registry.Registry;
import com.ice.jni.registry.RegistryException;
import com.ice.jni.registry.RegistryKey;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class contains the startup steps for when the plugin is launched from a Windows OS. Registry keys must be checked and created if missing.
 */
public final class WindowsStartup {
    private static final Logger logger = LoggerFactory.getLogger(WindowsStartup.class);
    private static final String JNI_REGISTRY_PACKAGE = RegistryKey.class.getPackage().getName();
    private static final String URL_FILE_PREFIX = "file:/";
    private static final String JAVA_LIBRARY_PATH = "java.library.path";
    private static final String SYS_PATH = "sys_paths";
    public static final String VSOI_KEY = "vsoi\\Shell\\Open\\Command";
    private static final String INTELLIJ_KEY = "IntelliJIdeaProjectFile\\shell\\open\\command";

    /**
     * Setup windows specific configurations upon plugin launch
     */
    public static void startup() {
        try {
            addDllPath(getDllDirectory());

            // get most up-to-date IntelliJ exe
            final RegistryKey rootKey = Registry.getTopLevelKey("HKEY_CLASSES_ROOT");
            final RegistryKey intellijKey = rootKey.openSubKey(INTELLIJ_KEY);
            final String intellijExe = intellijKey.getDefaultValue();

            if (!StringUtils.isEmpty(getValidExe(intellijExe)) && !checkIfKeysExistAndMatch(intellijExe, rootKey)) {
                final File regeditFile = createRegeditFile(intellijExe);
                launchElevatedCreation(regeditFile.getPath());
                regeditFile.delete();
            }
        } catch (RegistryException e) {
            logger.debug("The IntelliJ exe path could not be extracted from the registry: {}", e.getMessage());
        } catch (IOException e) {
            logger.debug("An IOException was encountered while creating/writing to the Regedit file: {}", e.getMessage());
        } catch (IllegalAccessException e) {
            logger.debug("Access denied while setting java.library.path: {}", e.getMessage());
        } catch (NoSuchFieldException e) {
            logger.debug("Exception while getting sys_path: {}", e.getMessage());
        }
    }

    /**
     * Add ICE_JNIRegistry.dll to java.library path for native C operations
     *
     * @param dllDirectory
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    protected static void addDllPath(String dllDirectory) throws NoSuchFieldException, IllegalAccessException {
        final String existingPath = System.getProperty(JAVA_LIBRARY_PATH);
        final String newPath = StringUtils.isEmpty(existingPath) ? dllDirectory : existingPath + ";" + dllDirectory;
        System.setProperty(JAVA_LIBRARY_PATH, newPath);
        final Field sysPath = ClassLoader.class.getDeclaredField(SYS_PATH);
        sysPath.setAccessible(true);
        // when the sys_paths field is set to null, the next time loadLibrary is called it will trigger
        // the library paths to be set again which pulls in the new addition of the dll to java.library.path
        sysPath.set(null, null);
    }

    /**
     * Check if vsoi registry keys exist already and match IntelliJ
     *
     * @param intellijExe
     * @param rootKey
     * @return if keys exists or not
     */
    protected static boolean checkIfKeysExistAndMatch(final String intellijExe, final RegistryKey rootKey) {
        // default that keys don't exist because it's ok to recreate them
        boolean isKeyCreated = false;
        try {
            // exception thrown if key is not found
            final RegistryKey vsoiKey = rootKey.openSubKey(VSOI_KEY);
            // check idea.exe is the same in both places
            if (intellijExe.equals(vsoiKey.getDefaultValue())) {
                isKeyCreated = true;
            }
        } catch (NoSuchKeyException e) {
            // suppressing exception since if key cannot be opened it doesn't exist and needs to be created
            logger.debug("The vsoi registry key does not exist: {}", VSOI_KEY);
        } catch (NoSuchValueException e) {
            // suppressing exception since if exe value doesn't exist then it needs to be created
            logger.debug("The value for the exe path does not exist: {}");
        } finally {
            return isKeyCreated;
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
            logger.debug("Running regedit encountered an IOException: {}", e.getMessage());
        } catch (Exception e) {
            logger.debug("Waiting for the process to execute resulted in an error: " + e.getMessage());
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
     * Finds the DLL directory of the plugin by finding the location of an existing JAR dependency
     *
     * @return plugin's base directory
     */
    protected static String getDllDirectory() {
        final String jarUrlPath = WindowsStartup.class.getClassLoader().getResource(JNI_REGISTRY_PACKAGE.replace(".", "/")).getPath();
        final int endOfBaseDir = jarUrlPath.lastIndexOf("lib");
        final String baseDir = jarUrlPath.substring(0, endOfBaseDir).replace(URL_FILE_PREFIX, "").replace("/", "\\");
        return baseDir.concat("classes\\");
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
