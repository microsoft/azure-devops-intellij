// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.Nullable;

public class TFSProgressUtil {

    public static void checkCanceled(final @Nullable ProgressIndicator progressIndicator) throws ProcessCanceledException {
        if (progressIndicator != null && progressIndicator.isCanceled()) {
            throw new ProcessCanceledException();
        }
    }

    public static boolean isCanceled(final @Nullable ProgressIndicator progressIndicator) {
        return progressIndicator != null && progressIndicator.isCanceled();
    }

    public static void setProgressText(final @Nullable ProgressIndicator progressIndicator, @Nullable String text) {
        if (progressIndicator != null && text != null) {
            progressIndicator.setText(text);
        }
    }

    public static void setProgressText2(final @Nullable ProgressIndicator progressIndicator, @Nullable String text) {
        if (progressIndicator != null && text != null) {
            progressIndicator.setText2(text);
        }
    }

    public static void setIndeterminate(final @Nullable ProgressIndicator progressIndicator, final boolean indeterminate) {
        if (progressIndicator != null) {
            progressIndicator.setIndeterminate(indeterminate);
        }
    }
}
