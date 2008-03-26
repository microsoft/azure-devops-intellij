package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.revision.TFSContentRevision;
import org.jetbrains.tfsIntegration.core.revision.TFSContentRevisionFactory;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;

import java.util.ArrayList;
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

  public void getChanges(final VcsDirtyScope dirtyScope, final ChangelistBuilder builder, final ProgressIndicator progress)
    throws VcsException {
    if (myProject.isDisposed()) {
      return;
    }
    if (builder == null) {
      return;
    }

    progress.setText("Processing changes");

    final List<FilePath> paths = new ArrayList<FilePath>();

    dirtyScope.iterate(new Processor<FilePath>() {
      public boolean process(final FilePath filePath) {
        VirtualFile file = filePath.getVirtualFile();
        if (file != null && file.isValid()) {
          if (!ChangeListManager.getInstance(myProject).isIgnoredFile(file)) {
            paths.add(filePath);
          }
        }
        return true;
      }
    });
    try {
      WorkstationHelper.processByWorkspaces(paths, new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          ChangeProcessor processor = new ChangelistBuilderProcessor(builder);
          StatusProvider.processPaths(workspace, paths, new DebugChangeProcessor(processor), null);
        }
      });
    }
    catch (TfsException e) {
      throw new VcsException("Failed to update file status", e);
    }
  }


  static class ChangelistBuilderProcessor implements ChangeProcessor {
    private ChangelistBuilder builder;

    public ChangelistBuilderProcessor(@NotNull ChangelistBuilder builder) {
      this.builder = builder;
    }

    public void processDeletedOnServer(final @NotNull ItemPath path, @NotNull final ExtendedItem item) {
      // TODO: special procesing for deleted on server files?
      builder.processUnversionedFile(path.getLocalPath().getVirtualFile());
    }

    public void processExistsButNotDownloaded(final @NotNull ItemPath path, @NotNull final ExtendedItem item) {
      builder.processUnversionedFile(path.getLocalPath().getVirtualFile());
    }

    public void processLocallyDeleted(final @NotNull ItemPath path, @NotNull final ExtendedItem item) {
      builder.processLocallyDeletedFile(path.getLocalPath());
    }

    public void processCheckedOutForEdit(final @NotNull ItemPath path, @NotNull final ExtendedItem item) {
      TFSContentRevision revision = TFSContentRevisionFactory.getRevision(path.getLocalPath(), item.getLatest());
      builder.processChange(new Change(revision, CurrentContentRevision.create(path.getLocalPath())));
    }

    public void processHijacked(final @NotNull ItemPath path, @NotNull final ExtendedItem item) {
      builder.processModifiedWithoutCheckout(path.getLocalPath().getVirtualFile());
    }

    public void processScheduledForAddition(final @NotNull ItemPath path, @NotNull final ExtendedItem item) {
      builder.processChange(new Change(null, new CurrentContentRevision(path.getLocalPath())));
    }

    public void processScheduledForDeletion(final @NotNull ItemPath path, @NotNull final ExtendedItem item) {
      builder.processChange(new Change(new CurrentContentRevision(path.getLocalPath()), null));
    }

    public void processOutOfDate(final ItemPath itemPath, final ExtendedItem item) {
      // do nothing
    }

    public void processUnversioned(final @NotNull ItemPath path) {
      builder.processUnversionedFile(path.getLocalPath().getVirtualFile());
    }

    public void processGhost(final ItemPath itemPath) {
      // do nothing
    }

    public void processUnexistingDeleted(final ItemPath itemPath, final ExtendedItem item) {
      //do nothing
    }

    public void processUpToDate(final ItemPath itemPath, final ExtendedItem item) {
      //do nothing
    }
  }

  public boolean isModifiedDocumentTrackingRequired() {
    return true;
  }


}
