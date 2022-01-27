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

import com.intellij.diff.DiffManager;
import com.intellij.diff.DiffRequestFactory;
import com.intellij.diff.InvalidDiffRequestException;
import com.intellij.diff.merge.MergeRequest;
import com.intellij.diff.merge.MergeResult;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vcs.merge.MergeDialogCustomizer;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.idea.tfvc.ui.resolve.ContentTriplet;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class DialogContentMerger implements ContentMerger {

    public boolean mergeContent(final ContentTriplet contentTriplet, final Project project, final VirtualFile localFile,
                                final VcsRevisionNumber serverVersion) {
        ArgumentHelper.checkIfFileWriteable(new File(localFile.getPath()));

        Document document = Objects.requireNonNull(FileDocumentManager.getInstance().getDocument(localFile));
        final MergeDialogCustomizer c = new MergeDialogCustomizer();
        final MergeRequest request;
        AtomicReference<MergeResult> result = new AtomicReference<>();
        try {
            request = DiffRequestFactory.getInstance().createMergeRequest(
                    project,
                    localFile.getFileType(),
                    document,
                    Arrays.asList(
                        StreamUtil.convertSeparators(contentTriplet.localContent),
                        StreamUtil.convertSeparators(contentTriplet.baseContent),
                        StreamUtil.convertSeparators(contentTriplet.serverContent)
                    ),
                    c.getMergeWindowTitle(localFile),
                    Arrays.asList(
                            c.getLeftPanelTitle(localFile),
                            c.getCenterPanelTitle(localFile),
                            c.getRightPanelTitle(localFile, serverVersion)
                    ), result::set);
        } catch (InvalidDiffRequestException e) {
            throw new RuntimeException(e);
        }

        DiffManager.getInstance().showMerge(project, request);

        return result.get() != MergeResult.CANCEL;
    }
}
