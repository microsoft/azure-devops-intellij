// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.setup;

import com.microsoft.alm.plugin.idea.utils.IdeaHelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 */
public class LinuxStartup {
    private static final String UPDATE_LOCAL_DESKTOP_DB_CMD = "update-desktop-database " + System.getProperty("user.home")+ "/.local/share/applications/";
    protected static final String SCRIPT_NAME = "vsts.sh";
    protected static final String LINUX_DIR = "linux";

    /**
     * Setup Linux specific configurations upon plugin launch
     */
    public static void startup() {
        try {
            final String scriptPath = IdeaHelper.getResourcePath(LinuxStartup.class.getResource("/"), SCRIPT_NAME, LINUX_DIR);
            createDesktopFile(new File(scriptPath));

            // Run process to update local desktop database to include the new protocol handler
            final Process process = Runtime.getRuntime().exec(UPDATE_LOCAL_DESKTOP_DB_CMD);
            process.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create VB script to run KeyCreator application as elevated
     *
     * @param scriptFile
     * @return script file
     * @throws IOException
     */
    protected static File createDesktopFile(final File scriptFile) throws IOException {
        final File script = new File(System.getProperty("user.home") + "/.local/share/applications/vsoi.desktop");
        final FileWriter fileWriter = new FileWriter(script);
        System.out.println(IdeaHelper.getResourcePath(LinuxStartup.class.getResource("/"), "vs-logo.png", "icons"));
        final BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        try {
            bufferedWriter.write(
                    "[Desktop Entry]\n" +
                    "Name=VSTS Protocol Handler\n" +
                    "Comment=Custom protocol handler for the IntelliJ VSTS plugin\n" +
                    "Exec=" + scriptFile.getAbsolutePath() + " %U\n" +
                    "Icon=/home/lantkiewicz/Documents/repos/Java.IntelliJ/plugin.idea/resources/icons/vs-logo.png \n" +
                    "Terminal=0\n" +
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
        return script;
    }
}

