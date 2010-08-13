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

package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.annotate.AnnotationProvider;
import com.intellij.openapi.vcs.annotate.FileAnnotation;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.ExtendedItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.revision.TFSContentRevision;
import org.jetbrains.tfsIntegration.core.tfs.AnnotationBuilder;
import org.jetbrains.tfsIntegration.core.tfs.TfsFileUtil;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.Workstation;
import org.jetbrains.tfsIntegration.core.tfs.version.ChangesetVersionSpec;
import org.jetbrains.tfsIntegration.core.tfs.version.VersionSpecBase;
import org.jetbrains.tfsIntegration.core.tfs.version.WorkspaceVersionSpec;
import org.jetbrains.tfsIntegration.exceptions.TfsException;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TFSAnnotationProvider implements AnnotationProvider {

  private static final int CURRENT_CHANGESET = 0;

  private final @NotNull TFSVcs myVcs;

  public TFSAnnotationProvider(final @NotNull TFSVcs vcs) {
    myVcs = vcs;
  }

  @Nullable
  public FileAnnotation annotate(final VirtualFile file) throws VcsException {
    return annotate(file, CURRENT_CHANGESET);
  }

  @Nullable
  public FileAnnotation annotate(final VirtualFile file, final VcsFileRevision revision) throws VcsException {
    return annotate(file, ((VcsRevisionNumber.Int)revision.getRevisionNumber()).getValue());
  }

  @Nullable
  private FileAnnotation annotate(final VirtualFile file, final int changeset) throws VcsException {
    final Ref<VcsException> exception = new Ref<VcsException>();
    final Ref<FileAnnotation> result = new Ref<FileAnnotation>();

    Runnable runnable = new Runnable() {
      public void run() {
        try {
          final ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
          TFSProgressUtil.setIndeterminate(progressIndicator, true);
          final FilePath localPath = TfsFileUtil.getFilePath(file);

          final Collection<WorkspaceInfo> workspaces = Workstation.getInstance().findWorkspaces(localPath, false);
          TFSProgressUtil.checkCanceled(progressIndicator);
          if (workspaces.isEmpty()) {
            exception.set(new VcsException(MessageFormat.format("Mappings not found for file ''{0}''", localPath.getPresentableUrl())));
            return;
          }

          final WorkspaceInfo workspace = workspaces.iterator().next();
          final Map<FilePath, ExtendedItem> path2item =
            workspace.getExtendedItems(Collections.singletonList(localPath), myVcs.getProject(), TFSBundle.message("loading.item"));
          if (path2item.isEmpty()) {
            exception.set(new VcsException(MessageFormat.format("''{0}'' is unversioned", localPath.getPresentableUrl())));
            return;
          }
          TFSProgressUtil.checkCanceled(progressIndicator);

          final VersionSpecBase versionSpec = changeset == CURRENT_CHANGESET
                                              ? new WorkspaceVersionSpec(workspace.getName(), workspace.getOwnerName())
                                              : new ChangesetVersionSpec(changeset);
          final List<VcsFileRevision> revisionList =
            TFSHistoryProvider.getRevisions(myVcs.getProject(), path2item.get(localPath).getSitem(), false, workspace, versionSpec);
          TFSProgressUtil.checkCanceled(progressIndicator);
          if (revisionList.isEmpty()) {
            return;
          }

          result.set(annotate(workspace, localPath, path2item.get(localPath).getItemid(), revisionList));
        }
        catch (TfsException e) {
          exception.set(new VcsException(e));
        }
        catch (VcsException e) {
          exception.set(e);
        }
      }
    };

    if (ApplicationManager.getApplication().isDispatchThread()) {
      ProgressManager.getInstance().runProcessWithProgressSynchronously(runnable, "Computing Annotations", true, myVcs.getProject());
    }
    else {
      runnable.run();
    }

    if (!exception.isNull()) {
      throw exception.get();
    }
    return result.get();
  }

  @Nullable
  private FileAnnotation annotate(final WorkspaceInfo workspace,
                                  final FilePath localPath,
                                  final int itemId,
                                  final List<VcsFileRevision> revisions) throws VcsException {

    final ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();

    final AnnotationBuilder annotationBuilder = new AnnotationBuilder(revisions, new AnnotationBuilder.ContentProvider() {
      public String getContent(final VcsFileRevision revision) throws VcsException {
        TFSProgressUtil.checkCanceled(progressIndicator);
        int changeset = ((VcsRevisionNumber.Int)revision.getRevisionNumber()).getValue();
        //noinspection ConstantConditions
        final String content = TFSContentRevision.create(myVcs.getProject(), workspace, localPath, changeset, itemId).getContent();
        if (content == null) {
          final String errorMessage =
            MessageFormat.format("Cannot load content for file ''{0}'', rev. {1}", localPath.getPresentableUrl(), changeset);
          throw new VcsException(errorMessage);
        }
        return content;
      }
    });

    return new TFSFileAnnotation(myVcs, workspace, annotationBuilder.getAnnotatedContent(), annotationBuilder.getLineRevisions());
  }

  public boolean isAnnotationValid(final VcsFileRevision revision) {
    return true;
  }
}
