// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core.revision;

import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import com.microsoft.alm.plugin.idea.tfvc.exceptions.TfsException;

import java.io.File;
import java.io.IOException;

public interface TFSContentStore {

    void saveContent(TfsFileUtil.ContentWriter contentWriter) throws TfsException, IOException;

    byte[] loadContent() throws TfsException, IOException;

    File getTmpFile();
}
