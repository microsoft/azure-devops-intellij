/*
 * Copyright 2000-2008 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.tfsIntegration.core.tfs.conflicts;

import org.jetbrains.tfsIntegration.ui.ConflictData;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Conflict;
import com.intellij.openapi.diff.MergeRequest;
import com.intellij.openapi.diff.ActionButtonPresentation;
import com.intellij.openapi.diff.DiffManager;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.peer.PeerFactory;

public class DialogContentMerger implements ContentMerger {

    public void mergeContent(Conflict conflict, ConflictData conflictData, Project project, VirtualFile localFile, String localPath) {
        MergeRequest request = PeerFactory.getInstance().getDiffRequestFactory().createMergeRequest(
                StreamUtil.convertSeparators(conflictData.serverContent), StreamUtil.convertSeparators(conflictData.localContent),
                StreamUtil.convertSeparators(conflictData.baseContent), localFile, project, ActionButtonPresentation.createApplyButton());

        request.setWindowTitle("Merge " + localPath);
        request.setVersionTitles(new String[]{"Server content (rev. " + conflict.getTver() + ")", "Merge result", "Local content"});
        // TODO call canShow() first
        DiffManager.getInstance().getDiffTool().show(request);
    }
}