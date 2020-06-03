package com.microsoft.alm.plugin.external.utils;

import com.intellij.openapi.vcs.VcsException;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.tfs.model.connector.TfsLocalPath;
import com.microsoft.tfs.model.connector.TfvcCheckoutResult;

import java.util.List;
import java.util.stream.Collectors;

public final class TfvcCheckoutResultUtils {

    /**
     * Verifies that the checkout has been completed successfully.
     *
     * @throws VcsException in case if it isn't.
     */
    public static void verify(TfvcCheckoutResult result) throws VcsException {
        if (!result.getErrorMessages().isEmpty()) {
            String message = String.join("\n", result.getErrorMessages());
            throw new VcsException(
                    TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CHECKOUT_FAILED, message));
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
}
