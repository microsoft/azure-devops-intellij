// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.visualstudio;

import com.intellij.openapi.application.ApplicationManager;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VisualStudioTfvcClient {

    private static final Logger ourLogger = Logger.getInstance(VisualStudioTfvcClient.class);

    private static final String[] SUPPORTED_VS_VERSIONS = new String[] { "2019", "2017" };
    private static final String[] SUPPORTED_VS_EDITIONS = new String[] { "Enterprise", "Professional", "Community" };
    private static final String EDITION_RELATIVE_TF_EXE_PATH =
            "Common7\\IDE\\CommonExtensions\\Microsoft\\TeamFoundation\\Team Explorer\\TF.exe";

    private static final Pattern VERSION_PATTERN = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.\\d+");
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
        CompletableFuture<Void> result = new CompletableFuture<>();
        try {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    ToolVersion clientVersion = getClientVersion(visualStudioClientPath);
                    if (clientVersion == null)
                        throw new VisualStudioClientVersionException(ToolVersion.UNKNOWN, MINIMAL_SUPPORTED_VERSION);

                    if (MINIMAL_SUPPORTED_VERSION.compare(clientVersion) > 0)
                        throw new VisualStudioClientVersionException(clientVersion, MINIMAL_SUPPORTED_VERSION);

                    result.complete(null);
                } catch (Throwable t) {
                    result.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            result.completeExceptionally(t);
        }

        return result;
    }

    @Nullable
    private static ToolVersion getClientVersion(Path visualStudioClientPath) throws IOException {
        ourLogger.info("Checking VS Client: " + visualStudioClientPath);
        Process process = new ProcessBuilder().command(visualStudioClientPath.toString()).start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                ourLogger.info("Client stdout: " + line);

                Matcher matcher = VERSION_PATTERN.matcher(line);
                if (matcher.find()) {
                    ourLogger.info("Client version: " + matcher.group());
                    return new ToolVersion(matcher.group());
                }
            }
        }

        return null;
    }
}
