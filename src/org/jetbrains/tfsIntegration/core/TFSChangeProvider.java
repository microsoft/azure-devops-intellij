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

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.ChangeProvider;
import com.intellij.openapi.vcs.changes.ChangelistBuilder;
import com.intellij.openapi.vcs.changes.VcsDirtyScope;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.exceptions.TfsException;

import java.util.List;

/**
 * TODO important cases
 * 1. when folder1 is unversioned and folder1/file1 is scheduled for addition, team explorer effectively shows folder1 as scheduled for addition
 */

public class TFSChangeProvider implements ChangeProvider {

  private Project myProject;

  public TFSChangeProvider(final Project project) {
    myProject = project;
  }

  public boolean isModifiedDocumentTrackingRequired() {
    return true;
  }

  public void getChanges(final VcsDirtyScope dirtyScope, final ChangelistBuilder builder, final ProgressIndicator progress)
    throws VcsException {
    if (myProject.isDisposed()) {
      return;
    }
    if (builder == null) {
      return;
    }

    progress.setText("Processing changes");

    // process only roots, filter out child items since requests are recursive anyway
    RootsCollection.FilePathRootsCollection roots = new RootsCollection.FilePathRootsCollection();
    roots.addAll(dirtyScope.getRecursivelyDirtyDirectories());
    roots.addAll(dirtyScope.getDirtyFiles());

    try {
      final Ref<Boolean> mappingFound = Ref.create(false);
      // ingore orphan roots here
      WorkstationHelper.processByWorkspaces(roots, true, new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          StatusProvider.visitByStatus(workspace, paths, true, progress, new ChangelistBuilderStatusVisitor(builder, workspace));
          mappingFound.set(true);
        }
      });
      if (!mappingFound.get()) {
        throw new VcsException("No Team Foundation Server mapping found");
      }
    }
    catch (TfsException e) {
      throw new VcsException(e.getMessage(), e);
    }
  }

}
