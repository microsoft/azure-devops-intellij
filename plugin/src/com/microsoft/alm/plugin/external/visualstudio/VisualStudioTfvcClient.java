// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.visualstudio;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.microsoft.alm.plugin.external.exceptions.VisualStudioClientVersionException;
import com.microsoft.alm.plugin.external.models.ToolVersion;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

public class VisualStudioTfvcClient {

    private static final Logger ourLogger = Logger.getInstance(VisualStudioTfvcClient.class);

    private static final String[] SUPPORTED_VS_VERSIONS = new String[] { "2019", "2017" };
    private static final String[] SUPPORTED_VS_EDITIONS = new String[] { "Enterprise", "Professional", "Community" };
    private static final String EDITION_RELATIVE_TF_EXE_PATH =
            "Common7\\IDE\\CommonExtensions\\Microsoft\\TeamFoundation\\Team Explorer\\TF.exe";

    private static final ToolVersion MINIMAL_SUPPORTED_VERSION = new ToolVersion(15, 0, 0, "");

    /**
     * Tries to detect path to the latest available TFVC client from a set of hardcoded locations.
     *
     * @return detected path or null.
     */
    @Nullable
    public static Path detectClientPath() {
        if (!SystemInfo.isWindows) return null;
        String programFilesPath = SystemInfo.is32Bit ? System.getenv("ProgramFiles") : System.getenv("ProgramFiles(x86)");
        if (StringUtils.isEmpty(programFilesPath))
            programFilesPath = "C:\\Program Files (x86)";
        Path visualStudioPath = Paths.get(programFilesPath, "Microsoft Visual Studio");
        for (String vsVersion : SUPPORTED_VS_VERSIONS) {
            Path versionPath = visualStudioPath.resolve(vsVersion);
            if (!versionPath.toFile().isDirectory()) continue;
            for (String vsEdition : SUPPORTED_VS_EDITIONS) {
                Path editionPath = versionPath.resolve(vsEdition);
                Path tfExePath = editionPath.resolve(EDITION_RELATIVE_TF_EXE_PATH);
                if (tfExePath.toFile().isFile()) return tfExePath;
            }
        }

        return null;
    }

    @NotNull
    public static CompletionStage<Void> checkVersionAsync(Path visualStudioClientPath) {
        return VisualStudioTfvcCommands.getVersionAsync(visualStudioClientPath).thenAccept(version -> {
            if (version == null)
                throw new VisualStudioClientVersionException(ToolVersion.UNKNOWN, MINIMAL_SUPPORTED_VERSION);

            if (MINIMAL_SUPPORTED_VERSION.compare(version) > 0)
                throw new VisualStudioClientVersionException(version, MINIMAL_SUPPORTED_VERSION);
        });
    }

    static List<String> executeClientAndGetOutput(
            @NotNull Path clientPath,
            @Nullable Path workingDirectory,
            @NotNull List<String> arguments) throws IOException, InterruptedException {
        ourLogger.info("Executing VS client: " + clientPath + ", args: " + StringUtils.join(arguments, ','));
        List<String> command = new ArrayList<>(arguments.size() + 1);
        command.add(clientPath.toString());
        command.addAll(arguments);

        Process client = new ProcessBuilder()
                .command(command)
                .directory(workingDirectory == null ? null : workingDirectory.toFile())
                .start();

        List<String> output = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                ourLogger.info("VS client stdout: " + line);
                output.add(line);
            }
        }

        int exitCode = client.waitFor();
        ourLogger.info("VS client exit code: " + exitCode);

        return output;
    }
}
