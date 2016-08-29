// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core.revision;

import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.commands.Command;
import com.microsoft.alm.plugin.external.commands.DownloadCommand;
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

    /**
     * Find the store for the given file path and if it doesn't already exist create it and download the file
     *
     * @param localPath:  local path of the file which is used as the key in the store along with the revision number
     * @param revision:   revision number of the file
     * @param actualPath: file path acknowledged by the server (could differ local path in case of renames)
     * @return
     * @throws IOException
     */
    public static TFSContentStore findOrCreate(final String localPath, final int revision, final String actualPath, final ServerContext serverContext) throws IOException {
        TFSContentStore store = TFSContentStoreFactory.find(localPath, revision);
        if (store == null) {
            store = TFSContentStoreFactory.create(localPath, revision);
            final Command<String> command = new DownloadCommand(serverContext, actualPath, revision, store.getTmpFile().getPath());
            command.runSynchronously();
        }
        return store;
    }
}

