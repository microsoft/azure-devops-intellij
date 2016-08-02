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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diff.ActionButtonPresentation;
import com.intellij.openapi.diff.DiffManager;
import com.intellij.openapi.diff.DiffRequestFactory;
import com.intellij.openapi.diff.MergeRequest;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vcs.merge.MergeDialogCustomizer;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.Conflict;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.ui.ContentTriplet;

public class DialogContentMerger implements ContentMerger {

  public boolean mergeContent(Conflict conflict, ContentTriplet contentTriplet, Project project, final VirtualFile localFile, String localPath,
                              VcsRevisionNumber serverVersion) {
    TFSVcs.assertTrue(localFile.isWritable(), localFile.getPresentableUrl() + " must be writable");

    MergeDialogCustomizer c = new MergeDialogCustomizer();
    MergeRequest request = DiffRequestFactory.getInstance().createMergeRequest(StreamUtil.convertSeparators(contentTriplet.localContent),
                                                                               StreamUtil.convertSeparators(contentTriplet.serverContent),
                                                                               StreamUtil.convertSeparators(contentTriplet.baseContent),
                                                                               localFile, project,
                                                                               ActionButtonPresentation.APPLY,
                                                                               ActionButtonPresentation.CANCEL_WITH_PROMPT);

    request.setWindowTitle(c.getMergeWindowTitle(localFile));
    request.setVersionTitles(new String[] {
      c.getLeftPanelTitle(localFile),
      c.getCenterPanelTitle(localFile),
      c.getRightPanelTitle(localFile, serverVersion)
    });

    // TODO call canShow() first
    DiffManager.getInstance().getDiffTool().show(request);
    if (request.getResult() == DialogWrapper.OK_EXIT_CODE) {
      return true;
    }
    else {
      request.restoreOriginalContent();
      // TODO maybe MergeVersion.MergeDocumentVersion.restoreOriginalContent() should save document itself?
      ApplicationManager.getApplication().runWriteAction(new Runnable() {
        public void run() {
          final Document document = FileDocumentManager.getInstance().getDocument(localFile);
          if (document != null) {
              FileDocumentManager.getInstance().saveDocument(document);
          }
        }
      });
      return false;
    }
  }
}
