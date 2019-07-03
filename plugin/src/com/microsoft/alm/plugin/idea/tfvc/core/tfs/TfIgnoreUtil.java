package com.microsoft.alm.plugin.idea.tfvc.core.tfs;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.LocalFilePath;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.util.ObjectUtils;
import com.microsoft.alm.plugin.external.models.Workspace;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class TfIgnoreUtil {
    public static final String TFIGNORE_FILE_NAME = ".tfignore";

    /**
     * Will find existing .tfignore file that is near the target file. If .tfignore doesn't exists, then the location
     * will be proposed.
     *
     * @param mappings workspace mappings (to properly determine root location)
     * @param file target file
     * @return path to .tfignore (not necessary an existing file); will return null if place to create .tfignore was not
     * found
     */
    @Nullable
    public static File findNearestOrRootTfIgnore(@NotNull List<Workspace.Mapping> mappings, @NotNull File file) {
        List<LocalFilePath> localRoots = mappings.stream()
                .map(m -> new LocalFilePath(m.getLocalPath(), Files.isDirectory(Paths.get(m.getLocalPath()))))
                .collect(Collectors.toList());
        File potentialTfIgnore = null;
        while (file != null) {
            if (file.isDirectory()) {
                LocalFilePath filePath = new LocalFilePath(file.getAbsolutePath(), true);
                if (localRoots.stream().noneMatch(root -> filePath.isUnder(root, false))) {
                    // Path is not under any of the root mappings; return last potential tfignore location that was under
                    // mapping.
                    return potentialTfIgnore;
                }

                potentialTfIgnore = new File(FileUtil.join(file.getAbsolutePath(), TFIGNORE_FILE_NAME));
                if (potentialTfIgnore.isFile()) {
                    return potentialTfIgnore;
                }
            }

            file = file.getParentFile();
        }

        // We've ended at the file system root; finish here.
        return null;
    }

    /**
     * Adds an item into the .tfignore file.
     * @param requestor an object that requested the change; see {@link VirtualFileEvent#getRequestor}
     * @param tfIgnore a {@link File} object representing the .tfignore file
     * @param fileToIgnore a file to ignore
     */
    public static void addToTfIgnore(@NotNull Object requestor, @NotNull File tfIgnore, @NotNull File fileToIgnore) throws IOException {
        String relativePath = FileUtil.getRelativePath(tfIgnore.getParentFile(), fileToIgnore);
        VirtualFile virtualTfIgnoreFile = LocalFileSystem.getInstance().findFileByIoFile(tfIgnore);
        if (virtualTfIgnoreFile == null) {
            VirtualFile parentDir = ObjectUtils.assertNotNull(  // should never be null because of the way we work with .tfignore
                    LocalFileSystem.getInstance().findFileByIoFile(tfIgnore.getParentFile()));
            virtualTfIgnoreFile = parentDir.createChildData(requestor, TFIGNORE_FILE_NAME);
        }

        addLineToFile(virtualTfIgnoreFile, relativePath);
    }

    private static void addLineToFile(VirtualFile file, String line) {
        FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
        Document document = ObjectUtils.assertNotNull(fileDocumentManager.getDocument(file));
        CharSequence contents = document.getCharsSequence();
        if (!StringUtils.isEmpty(contents) && !StringUtils.endsWith(contents, "\n")) {
            document.insertString(contents.length(), "\n");
        }
        document.insertString(document.getTextLength(), line);
        fileDocumentManager.saveDocument(document);
    }
}
