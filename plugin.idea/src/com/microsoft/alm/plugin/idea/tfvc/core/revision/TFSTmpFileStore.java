// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core.revision;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.StreamUtil;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import com.microsoft.alm.plugin.idea.tfvc.exceptions.TfsException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class TFSTmpFileStore implements TFSContentStore {
    @NonNls
    private static final String TMP_FILE_NAME = "idea_tfs";
    private static String myTfsTmpDir;

    @Nullable
    public static TFSContentStore find(final String localPath, final int revision) throws IOException {
        File tmpFile = new File(createTmpFileName(localPath, revision));
        if (tmpFile.exists()) {
            return new TFSTmpFileStore(tmpFile);
        }
        return null;
    }

    private final File myTmpFile;

    TFSTmpFileStore(final String localPath, final int revision) throws IOException {
        myTmpFile = new File(createTmpFileName(localPath, revision));
        myTmpFile.deleteOnExit();
    }

    private static String createTmpFileName(final String localPath, final int revision) throws IOException {
        return getTfsTmpDir() + File.separator + localPath.hashCode() + "." + revision;
    }

    TFSTmpFileStore(final File tmpFile) {
        myTmpFile = tmpFile;
    }

    private static String getTfsTmpDir() throws IOException {
        if (myTfsTmpDir == null) {
            File tmpDir = FileUtil.createTempFile(TMP_FILE_NAME, "");
            tmpDir.delete();
            tmpDir.mkdir();
            tmpDir.deleteOnExit();
            myTfsTmpDir = tmpDir.getAbsolutePath();
        }
        return myTfsTmpDir;
    }

    public void saveContent(TfsFileUtil.ContentWriter contentWriter) throws TfsException, IOException {
        TfsFileUtil.setFileContent(myTmpFile, contentWriter);
    }

    public byte[] loadContent() throws IOException {
        InputStream fileStream = null;
        try {
            fileStream = new FileInputStream(myTmpFile);
            return StreamUtil.loadFromStream(fileStream);
        } finally {
            if (fileStream != null) {
                fileStream.close();
            }
        }
    }

    public File getTmpFile() {
        return myTmpFile;
    }
}
