// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.visualstudio;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.microsoft.alm.plugin.external.exceptions.VisualStudioClientVersionException;
import com.microsoft.alm.plugin.external.models.ToolVersion;
import com.microsoft.alm.plugin.services.PropertyService;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
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
    private static final String TF_EXE_PATH_RELATIVE_TO_EDITION_ROOT =
            "Common7\\IDE\\CommonExtensions\\Microsoft\\TeamFoundation\\Team Explorer\\TF.exe";

    private static final ToolVersion MINIMAL_SUPPORTED_VERSION = new ToolVersion(15, 0, 0, "");
    private static final String MINIMAL_SUPPORTED_VERSION_NICKNAME = "Visual Studio 2017";

    @Nullable
    public static Path getOrDetectPath(PropertyService propertyService) {
        String path = propertyService.getProperty(PropertyService.PROP_VISUAL_STUDIO_TF_CLIENT_PATH);
        if (!StringUtil.isEmpty(path)) {
            return Paths.get(path);
        }

        return detectClientPath();
    }

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
                Path tfExePath = editionPath.resolve(TF_EXE_PATH_RELATIVE_TO_EDITION_ROOT);
                if (tfExePath.toFile().isFile()) return tfExePath;
            }
        }

        return null;
    }

    @NotNull
    public static CompletionStage<Void> checkVersionAsync(
            @NotNull Project project,
            @NotNull Path visualStudioClientPath) {
        return VisualStudioTfvcCommands.getVersionAsync(project, visualStudioClientPath).thenAccept(version -> {
            if (version == null)
                throw new VisualStudioClientVersionException(ToolVersion.UNKNOWN, MINIMAL_SUPPORTED_VERSION, MINIMAL_SUPPORTED_VERSION_NICKNAME);

            if (MINIMAL_SUPPORTED_VERSION.compare(version) > 0)
                throw new VisualStudioClientVersionException(version, MINIMAL_SUPPORTED_VERSION, MINIMAL_SUPPORTED_VERSION_NICKNAME);
        });
    }

    private static List<String> readAsLines(InputStream stream, String label) throws IOException {
        List<String> output = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                ourLogger.info("VS client " + label + ": " + line);
                output.add(line);
            }
        }

        return output;
    }

    /**
     * @return a pair of stdout and stderr.
     */
    static Couple<List<String>> executeClientAndGetOutput(
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

        List<String> output = readAsLines(client.getInputStream(), "stdout");
        List<String> errors = readAsLines(client.getErrorStream(), "stderr");

        int exitCode = client.waitFor();
        ourLogger.info("VS client exit code: " + exitCode);

        return Couple.of(output, errors);
    }
}
