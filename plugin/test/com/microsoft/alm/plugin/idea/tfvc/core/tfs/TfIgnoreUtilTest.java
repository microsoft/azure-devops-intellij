package com.microsoft.alm.plugin.idea.tfvc.core.tfs;

import com.microsoft.alm.plugin.external.models.Workspace;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class TfIgnoreUtilTest {

    @Test
    public void findRootTfIgnore() throws IOException {
        Path root = createTempFileSystem("root/project/node_modules/test.txt");
        Path projectRoot = root.resolve("root/project");
        Path textFile = projectRoot.resolve("node_modules/test.txt");
        Workspace.Mapping mapping = new Workspace.Mapping("$/", projectRoot.toString(), false);

        File tfIgnore = TfIgnoreUtil.findNearestOrRootTfIgnore(Collections.singleton(mapping), textFile.toFile());

        File expectedTfIgnore = projectRoot.resolve(".tfignore").toFile();
        assertEquals(expectedTfIgnore, tfIgnore);
    }

    @Test
    public void findTfIgnoreInTheSameDirectory() throws IOException {
        Path root = createTempFileSystem("root/project/node_modules/test.txt", "root/project/node_modules/.tfignore");
        Path projectRoot = root.resolve("root/project");
        Path textFile = projectRoot.resolve("node_modules/test.txt");
        Workspace.Mapping mapping = new Workspace.Mapping("$/", projectRoot.toString(), false);

        File tfIgnore = TfIgnoreUtil.findNearestOrRootTfIgnore(Collections.singleton(mapping), textFile.toFile());

        File expectedTfIgnore = projectRoot.resolve("node_modules/.tfignore").toFile();
        assertEquals(expectedTfIgnore, tfIgnore);
    }

    @Test
    public void findTfIgnoreInNearestDirectory() throws IOException {
        Path root = createTempFileSystem("root/project/src/node_modules/test.txt", "root/project/src/.tfignore");
        Path projectRoot = root.resolve("root/project");
        Path textFile = projectRoot.resolve("src/node_modules/src/node_modules/test.txt");
        Workspace.Mapping mapping = new Workspace.Mapping("$/", projectRoot.toString(), false);

        File tfIgnore = TfIgnoreUtil.findNearestOrRootTfIgnore(Collections.singleton(mapping), textFile.toFile());

        File expectedTfIgnore = projectRoot.resolve("src/.tfignore").toFile();
        assertEquals(expectedTfIgnore, tfIgnore);
    }

    private static Path createTempFileSystem(String... filesToCreate) throws IOException {
        Path rootDirectory = Files.createTempDirectory("azuredevopstest");
        for (String relativePath : filesToCreate) {
            Path filePath = rootDirectory.resolve(relativePath);
            Path directoryPath = filePath.getParent();

            Files.createDirectories(directoryPath);
            //noinspection ResultOfMethodCallIgnored
            filePath.toFile().createNewFile();
        }

        return rootDirectory;
    }
}