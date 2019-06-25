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

package com.microsoft.alm.plugin.idea.tfvc.core.tfs.conflicts;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.tfvc.ui.resolve.MergeNameDialog;
import org.jetbrains.annotations.Nullable;

public class DialogNameMerger implements NameMerger {

    @Nullable
    public String mergeName(final String nameChoice1, final String nameChoice2, final Project project) {
        final MergeNameDialog dialog = new MergeNameDialog(nameChoice1, nameChoice2, project);
        if (dialog.showAndGet()) {
            return dialog.getSelectedPath();
        }
        return null;
    }
}