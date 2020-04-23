// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.google.common.collect.Lists;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import com.microsoft.tfs.model.connector.TfsPath;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A result of TFVC delete operation.
 */
public class TfvcDeleteResult {
    private final List<Path> deletedPaths;
    private final List<TfsPath> notFoundPaths;
    private final List<String> errorMessages;

    public TfvcDeleteResult() {
        this(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    public TfvcDeleteResult(List<Path> deletedPaths, List<TfsPath> notFoundPaths, List<String> errorMessages) {
        this.deletedPaths = deletedPaths;
        this.notFoundPaths = notFoundPaths;
        this.errorMessages = errorMessages;
    }

    public TfvcDeleteResult mergeWith(TfvcDeleteResult other) {
        List<Path> newDeletedPaths = Lists.newArrayList(deletedPaths);
        List<TfsPath> newNotFoundPaths = Lists.newArrayList(notFoundPaths);
        List<String> newErrorMessages = Lists.newArrayList(errorMessages);

        newDeletedPaths.addAll(other.deletedPaths);
        newNotFoundPaths.addAll(other.notFoundPaths);
        newErrorMessages.addAll(other.errorMessages);

        return new TfvcDeleteResult(newDeletedPaths, newNotFoundPaths, newErrorMessages);
    }

    public List<Path> getDeletedPaths() {
        return deletedPaths;
    }

    /**
     * List of paths that weren't found in the repository and thus couldn't be deleted.
     */
    public List<TfsPath> getNotFoundPaths() {
        return notFoundPaths;
    }

    /**
     * Other error messages that happened during the operation, except "file not found" message.
     */
    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public void throwIfErrorMessagesAreNotEmpty() throws IOException {
        if (!errorMessages.isEmpty()) {
            throw new IOException(
                    "Error occurred when trying to delete files:\n"
                            + String.join("\n", errorMessages));
        }
    }

    public void throwIfNotFoundPathsAreNotEmpty() throws IOException {
        if (!notFoundPaths.isEmpty()) {
            List<String> fileNames = notFoundPaths.stream()
                    .map(TfsFileUtil::getPathItem)
                    .collect(Collectors.toList());
            throw new IOException(
                    "TFVC client wasn't able to not delete the following files:\n"
                            + String.join("\n", fileNames));
        }
    }
}
