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
          StatusProvider.visitByStatus(workspace, paths, null, new ChangelistBuilderStatusVisitor(builder));
        }
      });
    }
    catch (TfsException e) {
      throw new VcsException("Failed to update file status", e);
    }
  }


  private static class ChangelistBuilderStatusVisitor implements StatusVisitor {
    private ChangelistBuilder builder;

    public ChangelistBuilderStatusVisitor(@NotNull ChangelistBuilder builder) {
      this.builder = builder;
    }

    public void unversioned(final ItemPath path, final boolean localItemExists) {
      if (localItemExists) {
        builder.processUnversionedFile(path.getLocalPath().getVirtualFile());
      }
    }

    public void notDownloaded(final ItemPath path, final ExtendedItem extendedItem, final boolean localItemExists) {
      if (localItemExists) {
        builder.processUnversionedFile(path.getLocalPath().getVirtualFile());
      }
    }

    public void checkedOutForEdit(final ItemPath path, final ExtendedItem extendedItem, final boolean localItemExists) {
      if (localItemExists) {
        TFSContentRevision latestRevision = TFSContentRevisionFactory.getRevision(path.getLocalPath(), extendedItem.getLatest());
        builder.processChange(new Change(latestRevision, CurrentContentRevision.create(path.getLocalPath())));
      }
      else {
        builder.processLocallyDeletedFile(path.getLocalPath());
      }
    }

    public void scheduledForAddition(final ItemPath path, final ExtendedItem extendedItem, final boolean localItemExists) {
      if (localItemExists) {
        builder.processChange(new Change(null, new CurrentContentRevision(path.getLocalPath())));
      }
      else {
        builder.processLocallyDeletedFile(path.getLocalPath());
      }
    }

    public void scheduledForDeletion(final ItemPath path, final ExtendedItem extendedItem, final boolean localItemExists) {
      builder.processChange(new Change(new CurrentContentRevision(path.getLocalPath()), null));
    }

    public void outOfDate(final ItemPath path, final ExtendedItem extendedItem, final boolean localItemExists) {
      if (localItemExists) {
        if (StatusProvider.isFileWritable(path)) {
          builder.processModifiedWithoutCheckout(path.getLocalPath().getVirtualFile());
        }
      }
      else {
        builder.processLocallyDeletedFile(path.getLocalPath());
      }
    }

    public void deleted(final ItemPath path, final ExtendedItem extendedItem, final boolean localItemExists) {
      if (localItemExists) {
        builder.processUnversionedFile(path.getLocalPath().getVirtualFile());
      }
    }

    public void upToDate(final ItemPath path, final ExtendedItem extendedItem, final boolean localItemExists) {
      if (localItemExists) {
        if (StatusProvider.isFileWritable(path)) {
          builder.processModifiedWithoutCheckout(path.getLocalPath().getVirtualFile());
        }
      }
      else {
        builder.processLocallyDeletedFile(path.getLocalPath());
      }
    }
  }

  public boolean isModifiedDocumentTrackingRequired() {
    return true;
  }


}
