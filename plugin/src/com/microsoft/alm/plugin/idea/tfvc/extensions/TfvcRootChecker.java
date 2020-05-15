// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.extensions;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.VcsKey;
import com.intellij.openapi.vcs.VcsRootChecker;
import com.microsoft.alm.plugin.external.exceptions.ToolAuthenticationException;
import com.microsoft.alm.plugin.external.exceptions.WorkspaceCouldNotBeDeterminedException;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.external.tools.TfTool;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.tfvc.core.TFSVcs;
import com.microsoft.alm.plugin.idea.tfvc.ui.settings.EULADialog;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TfvcRootChecker extends VcsRootChecker {
    private static final Logger ourLogger = Logger.getInstance(TfvcRootChecker.class);

    /**
     * Checks if registered mapping can be used to perform VCS operations. According to the specification, returns
     * {@code true} if unsure.
     * <br/>
     * It is used as optimization in IDEA 2019.2+.
     */
    // @Override // only available in IDEA 2019.2
    public boolean validateRoot(@NotNull String path) {
        return !StringUtil.isEmpty(TfTool.getLocation());
    }

    @Override
    public boolean isRoot(@NotNull String path) {
        if (!validateRoot(path))
            return false;

        return EULADialog.executeWithGuard(null, () -> {
            Workspace workspace = null;
            Path workspacePath = Paths.get(path);
            try {
                workspace = CommandUtils.getPartialWorkspace(workspacePath, true);
            } catch (WorkspaceCouldNotBeDeterminedException | ToolAuthenticationException ex) {
                if (!(ex instanceof WorkspaceCouldNotBeDeterminedException))
                    ourLogger.info(ex);

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
