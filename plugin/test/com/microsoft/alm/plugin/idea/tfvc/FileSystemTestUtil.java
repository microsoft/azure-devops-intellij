// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSystemTestUtil {
    /**
     * Creates a file system tree in a temporary directory.
     * @param entriesToCreate a list of paths to create. If path ends with '/', it will become a directory; otherwise it
     *                        will become a file
     * @return a path to the root of the file system tree.
     */
    public static Path createTempFileSystem(String... entriesToCreate) throws IOException {
        Path rootDirectory = Files.createTempDirectory("azuredevopstest");
        for (String relativePath : entriesToCreate) {
            Path resultingPath = rootDirectory.resolve(relativePath);
            Path directoryPath = resultingPath.getParent();

            Files.createDirectories(directoryPath);
            if (relativePath.endsWith("/")) {
                Files.createDirectories(resultingPath);
            } else {
                //noinspection ResultOfMethodCallIgnored
                resultingPath.toFile().createNewFile();
            }
        }

        return rootDirectory;
    }
}
