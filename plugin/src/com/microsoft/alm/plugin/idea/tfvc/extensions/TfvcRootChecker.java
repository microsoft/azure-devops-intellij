// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.extensions;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.VcsKey;
import com.intellij.openapi.vcs.VcsRootChecker;
import com.microsoft.alm.plugin.external.exceptions.WorkspaceCouldNotBeDeterminedException;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.external.tools.TfTool;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.tfvc.core.TFSVcs;
import com.microsoft.alm.plugin.idea.tfvc.ui.settings.EULADialog;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Paths;

public class TfvcRootChecker extends VcsRootChecker {
    private static final Logger ourLogger = Logger.getInstance(TfvcRootChecker.class);

    public static boolean isPossibleTfvcWorkspaceRoot(@NotNull String path) {
        if (StringUtil.isEmpty(TfTool.getLocation()))
            return false;

        return new File(path, "$tf").isDirectory() || new File(path, ".tf").isDirectory();
    }

    @Override
    public boolean isRoot(@NotNull String path) {
        if (!isPossibleTfvcWorkspaceRoot(path))
            return false;

        return EULADialog.executeWithGuard(null, () -> {
            Workspace workspace = null;
            try {
                workspace = CommandUtils.getPartialWorkspace(Paths.get(path));
            } catch (WorkspaceCouldNotBeDeterminedException ex) {
                ourLogger.info("TFVC workspace could not be determined from path \"" + path + "\"");
            }

            if (workspace == null) return false;
            return workspace.getMappings().stream()
                    .anyMatch(mapping -> FileUtil.pathsEqual(path, mapping.getLocalPath()));
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
