// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.intellij.openapi.vcs.EditFileProvider;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.tfs.model.connector.TfsLocalPath;
import com.microsoft.tfs.model.connector.TfvcCheckoutResult;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TfvcEditFileProvider implements EditFileProvider {

    private final TFSVcs myVcs;

    public TfvcEditFileProvider(TFSVcs vcs) {
        myVcs = vcs;
    }

    @Override
    public void editFiles(VirtualFile[] files) throws VcsException {
        List<Path> paths = Stream.of(files).map(file -> Paths.get(file.getPath())).collect(Collectors.toList());

        ServerContext serverContext = myVcs.getServerContext(true);
        TfvcCheckoutResult result = TfvcClient.getInstance(myVcs.getProject()).checkoutForEdit(
                serverContext,
                paths,
                false);
        if (!result.getErrorMessages().isEmpty()) {
            String message = String.join("\n", result.getErrorMessages());
            throw new VcsException(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CHECKOUT_FAILED, message));
        }

        if (!result.getNotFoundFiles().isEmpty()) {
            List<String> failedPaths = result.getNotFoundFiles().stream()
                    .map(TfsLocalPath::getPath)
                    .collect(Collectors.toList());
            String failedPathList = String.join(", ", failedPaths);
            throw new VcsException(
                    TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CHECKOUT_FILES_FAILED, failedPathList));
        }
    }

    @Override
    public String getRequestText() {
        return TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CHECKOUT_FILES);
    }
}
