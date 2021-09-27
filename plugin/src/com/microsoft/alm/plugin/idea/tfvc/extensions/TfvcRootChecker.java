// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.extensions;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.VcsKey;
import com.intellij.openapi.vcs.VcsRootChecker;
import com.microsoft.alm.plugin.external.exceptions.ToolAuthenticationException;
import com.microsoft.alm.plugin.external.tools.TfTool;
import com.microsoft.alm.plugin.idea.tfvc.core.TFSVcs;
import com.microsoft.alm.plugin.idea.tfvc.core.TfvcWorkspaceLocator;
import com.microsoft.alm.plugin.idea.tfvc.ui.settings.EULADialog;
import com.microsoft.tfs.model.connector.TfsDetailedWorkspaceInfo;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TfvcRootChecker extends VcsRootChecker {
    private static final Logger ourLogger = Logger.getInstance(TfvcRootChecker.class);

    private final TfvcRootCache myCache = new TfvcRootCache();

    /**
     * Checks if registered mapping can be used to perform VCS operations. According to the specification, returns
     * {@code true} if unsure.
     */
    @Override
    public boolean validateRoot(@NotNull String pathString) {
        Path path = Paths.get(pathString);
        Path fileName = path.getFileName();
        if (fileName != null && (isVcsDir(fileName.toString()) || fileName.toString().startsWith("$")))
            return false;

        for (Path component : path) {
            if (isVcsDir(component.toString()))
                return false;
        }

        TfvcRootCache.CachedStatus cachedStatus = myCache.get(path);
        return cachedStatus == TfvcRootCache.CachedStatus.UNKNOWN
                || cachedStatus == TfvcRootCache.CachedStatus.IS_MAPPING_ROOT; // known as not a root otherwise
    }

    @Override
    public boolean isRoot(@NotNull String path) {
        if (!validateRoot(path))
            return false;

        if (StringUtil.isEmpty(TfTool.getLocation()))
            return false;

        TfvcRootCache.CachedStatus cachedStatus = myCache.get(Paths.get(path));
        switch (cachedStatus) {
            case IS_MAPPING_ROOT:
                return true;
            case NO_ROOT:
            case UNDER_MAPPING_ROOT:
                return false;
        }

        // Will get here only if cachedStatus == UNKNOWN.
        return EULADialog.executeWithGuard(null, () -> {
            TfsDetailedWorkspaceInfo workspace = null;
            Path workspacePath = Paths.get(path);
            try {
                workspace = TfvcWorkspaceLocator.getPartialWorkspace(null, workspacePath, true);
            } catch (ToolAuthenticationException ex) {
                ourLogger.warn(ex);
            }

            if (workspace == null) {
                ourLogger.info("TFVC workspace could not be determined from path \"" + path + "\"");
                myCache.putNoMappingsFor(workspacePath);
                return false;
            }

            myCache.putMappings(workspace.getMappings());
            return workspace.getMappings().stream()
                    .anyMatch(mapping -> FileUtil.pathsEqual(path, mapping.getLocalPath().getPath()));
        });
    }

    @Override
    public boolean isVcsDir(@NotNull String path) {
        return path.equalsIgnoreCase("$tf") || path.equalsIgnoreCase(".tf");
    }

    @NotNull
    @Override
    public VcsKey getSupportedVcs() {
        return TFSVcs.getKey();
    }
}
