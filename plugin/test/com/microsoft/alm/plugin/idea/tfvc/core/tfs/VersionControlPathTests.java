package com.microsoft.alm.plugin.idea.tfvc.core.tfs;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class VersionControlPathTests {
    @Test
    public void localPathFromTfsRepresentationShouldConvertPathCase() throws IOException {
        File tempDirectory = FileUtil.createTempDirectory("azure-devops", ".tmp");
        try {
            File tempFile = tempDirectory.toPath().resolve("CASE_SENSITIVE.tmp").toFile();
            Assert.assertTrue(tempFile.createNewFile());

            String tfsRepresentation = tempFile.getAbsolutePath();
            // On non-Windows systems, TFS uses a "fake drive" prefix:
            if (!SystemInfo.isWindows)
                tfsRepresentation = "U:" + tfsRepresentation;

            String localPath = VersionControlPath.localPathFromTfsRepresentation(tfsRepresentation);
            Assert.assertEquals(tempFile.getAbsolutePath(), localPath);

            tfsRepresentation = tfsRepresentation.toLowerCase();
            localPath = VersionControlPath.localPathFromTfsRepresentation(tfsRepresentation);
            Assert.assertEquals(tempFile.getAbsolutePath(), localPath);
        } finally {
            FileUtil.delete(tempDirectory);
        }
    }
}
