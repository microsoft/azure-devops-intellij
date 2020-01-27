// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.extensions;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.VcsKey;
import com.intellij.openapi.vcs.VcsRootChecker;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.external.tools.TfTool;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.tfvc.core.TFSVcs;
import com.microsoft.alm.plugin.idea.tfvc.ui.settings.EULADialog;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Paths;

public class TfvcRootChecker extends VcsRootChecker {
    @Override
    public boolean isRoot(@NotNull String path) {
        if (StringUtil.isEmpty(TfTool.getLocation()))
            return false;

        if (!new File(path, "$tf").isDirectory() && !new File(path, ".tf").isDirectory())
            return false;

        return EULADialog.executeWithGuard(null, () -> {
            Workspace workspace = CommandUtils.getPartialWorkspace(Paths.get(path));
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
