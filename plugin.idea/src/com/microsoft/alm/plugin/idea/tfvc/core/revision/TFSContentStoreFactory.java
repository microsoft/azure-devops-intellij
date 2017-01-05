// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.microsoft.alm.plugin.idea.tfvc.core.revision;

import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.commands.Command;
import com.microsoft.alm.plugin.external.commands.DownloadCommand;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class TFSContentStoreFactory {
    private static final Logger logger = LoggerFactory.getLogger(TFSContentStoreFactory.class);

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
            try {
                store = TFSContentStoreFactory.create(localPath, revision);
                // By setting the IgnoreFileNotFound flag to true in DownloadCommand, we will get back an empty file if the file was deleted on the server or
                // for some other reason doesn't exist.
                final Command<String> command = new DownloadCommand(serverContext, actualPath, revision, store.getTmpFile().getPath(), true);
                command.runSynchronously();
            } catch(final Throwable t) {
                // Can't let exceptions bubble out here to the caller. This method is called by the VCS provider code in various places.
                logger.warn("Unable to download content for a TFVC file.", t);
            }
        }
        return store;
    }
}

