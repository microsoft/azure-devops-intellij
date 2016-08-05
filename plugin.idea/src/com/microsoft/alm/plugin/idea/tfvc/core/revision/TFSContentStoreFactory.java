// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core.revision;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class TFSContentStoreFactory {

    public static TFSContentStore create(final String localPath, final int revision) throws IOException {
        return new TFSTmpFileStore(localPath, revision);
    }

    @Nullable
    public static TFSContentStore find(final String localPath, final int revision) throws IOException {
        return TFSTmpFileStore.find(localPath, revision);
    }
}

