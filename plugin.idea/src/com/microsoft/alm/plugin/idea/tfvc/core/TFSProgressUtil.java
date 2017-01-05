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
