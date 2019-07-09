package com.microsoft.alm.plugin.idea.tfvc.core.tfs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.LocalFilePath;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class TFVCUtil {

    public static boolean isFileUnderTFVCMapping(@NotNull Project project, FilePath filePath) {
        List<FilePath> workspaceMappings = getMappingsFromWorkspace(project);
        for (FilePath mappingPath : workspaceMappings) {
            if (filePath.isUnder(mappingPath, false)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Some commands (e.g. `tf status` or `tf get`) won't work if any of the paths passed don't belong to a workspace,
     * so we need to filter only the items belonging to a local workspace before passing arguments to these commands.
     */
    @NotNull
    public static List<String> filterValidTFVCPaths(@NotNull Project project, @NotNull Collection<FilePath> paths) {
        List<FilePath> mappingPaths = getMappingsFromWorkspace(project);
        List<String> pathsToProcess = new ArrayList<String>();
        for (FilePath path : paths) {
            // if we get a change notification in the $tf folder, we need to just ignore it
            if (StringUtils.containsIgnoreCase(path.getPath(), "$tf") ||
                    StringUtils.containsIgnoreCase(path.getPath(), ".tf")) {
                continue;
            }

            // Ignore any files outside of a TFVC mapping:
            for (FilePath mappingPath : mappingPaths) {
                if (path.isUnder(mappingPath, false)) {
                    // Ignore any paths that has '$' in any component under the mapping root.
                    if (!hasIllegalDollarInAnyComponent(mappingPath, path)) {
                        pathsToProcess.add(path.getPath());
                        break;
                    }
                }
            }
        }

        return pathsToProcess;
    }

    private static List<FilePath> getMappingsFromWorkspace(@NotNull Project project) {
        Workspace workspace = CommandUtils.getPartialWorkspace(project);
        if (workspace == null) {
            return Collections.emptyList();
        }

        List<FilePath> mappingPaths = new ArrayList<FilePath>();
        for (Workspace.Mapping mapping : workspace.getMappings()) {
            mappingPaths.add(new LocalFilePath(mapping.getLocalPath(), true));
        }

        return mappingPaths;
    }

    private static boolean hasIllegalDollarInAnyComponent(FilePath mapping, FilePath localPath) {
        String relativePath = FileUtil.getRelativePath(mapping.getIOFile(), localPath.getIOFile());
        if (relativePath == null) {
            return localPath.getName().startsWith("$");
        }

        File file = new File(relativePath);
        while (file != null) {
            if (file.getName().startsWith("$")) return true;
            file = file.getParentFile();
        }

        return false;
    }
}
