// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core.tfs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.LocalFilePath;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.tfvc.core.TFSVcs;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
     * Will return only invalid paths for TFVC (i.e. paths containing dollar in any component, or $tf / .tf service
     * directory).
     * <p>
     * Performs a quick check (without checking every VCS mapping) because we often need this in performance-sensitive
     * contexts.
     */
    public static Stream<FilePath> collectInvalidTFVCPaths(@NotNull TFSVcs vcs, @NotNull Stream<FilePath> paths) {
        ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(vcs.getProject());
        List<FilePath> mappings = vcsManager.getDirectoryMappings(vcs).stream()
                .map(mapping -> new LocalFilePath(mapping.getDirectory(), true))
                .collect(Collectors.toList());
        return paths.filter(path -> isInServiceDirectory(path)
            || mappings.stream() .anyMatch(mapping -> hasIllegalDollarInAnyComponent(mapping, path)));
    }

    /**
     * Will check invalid paths for TFVC (i.e. paths containing dollar in any component, or $tf / .tf service
     * directory).
     * <p>
     * Performs a quick check (without checking every VCS mapping) because we often need this in performance-sensitive
     * contexts.
     */
    public static boolean isInvalidTFVCPath(@NotNull TFSVcs vcs, @NotNull FilePath path) {
        if (isInServiceDirectory(path)) return true;

        ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(vcs.getProject());

        return vcsManager.getDirectoryMappings(vcs).stream()
                .map(mapping -> new LocalFilePath(mapping.getDirectory(), true))
                .anyMatch(mapping -> hasIllegalDollarInAnyComponent(mapping, path));
    }

    /**
     * Some commands (e.g. `tf status` or `tf get`) won't work if any of the paths passed don't belong to a workspace,
     * so we need to filter only the items belonging to a local workspace before passing arguments to these commands.
     */
    @NotNull
    public static List<String> filterValidTFVCPaths(@NotNull Project project, @NotNull Collection<FilePath> paths) {
        List<FilePath> mappingPaths = getMappingsFromWorkspace(project);
        List<String> filteredPaths = new ArrayList<>();
        for (FilePath path : paths) {
            // if we get a change notification in the $tf folder, we need to just ignore it
            if (isInServiceDirectory(path)) {
                continue;
            }

            // Ignore any files outside of a TFVC mapping:
            for (FilePath mappingPath : mappingPaths) {
                if (path.isUnder(mappingPath, false)) {
                    // Ignore any paths that has '$' in any component under the mapping root.
                    if (!hasIllegalDollarInAnyComponent(mappingPath, path)) {
                        filteredPaths.add(path.getPath());
                        break;
                    }
                }
            }
        }

        return filteredPaths;
    }

    private static boolean isInServiceDirectory(FilePath filePath) {
        String path = filePath.getPath();
        return StringUtils.containsIgnoreCase(path, "$tf")
                || StringUtils.containsIgnoreCase(path, ".tf");
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
