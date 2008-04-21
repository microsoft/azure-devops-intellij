package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.update.UpdateEnvironment;
import com.intellij.openapi.vcs.update.UpdateSession;
import com.intellij.openapi.vcs.update.UpdatedFiles;
import com.intellij.openapi.vcs.update.FileGroup;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.core.tfs.version.LatestVersionSpec;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.GetOperation;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.LocalVersionUpdate;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.RecursionType;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.VersionSpec;

import java.util.*;

public class TFSUpdateEnvironment implements UpdateEnvironment {
  private TFSVcs myVcs;

  TFSUpdateEnvironment(TFSVcs tfsVcs) {
    myVcs = tfsVcs;
  }

  public void fillGroups(final UpdatedFiles updatedFiles) {
  }

  @NotNull
  public UpdateSession updateDirectories(@NotNull final FilePath[] contentRoots,
                                         final UpdatedFiles updatedFiles,
                                         final ProgressIndicator progressIndicator) throws ProcessCanceledException {

    final List<VcsException> exceptions = new ArrayList<VcsException>();
    // TODO: get version from Configurable created in createConfigurable (latest for now)
    final VersionSpec versionSpec = LatestVersionSpec.INSTANCE;

    try {
      List<FilePath> unversioned =
      WorkstationHelper.processByWorkspaces(Arrays.asList(contentRoots), new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {

          TFSProgressUtil.setProgressText(progressIndicator, "Request update information");

          List<VersionControlServer.GetRequestParams> requests = new ArrayList<VersionControlServer.GetRequestParams>(paths.size());
          for (ItemPath path : paths) {
            requests.add(new VersionControlServer.GetRequestParams(path.getServerPath(), RecursionType.Full, versionSpec));
            TFSProgressUtil.checkCanceled(progressIndicator);
          }
          // query get operations for contentRoots
          List<List<GetOperation>> getOperations =
            workspace.getServer().getVCS().get(workspace.getName(), workspace.getOwnerName(), requests);

          Map<ItemPath, GetOperation> itemPaths2operations = new HashMap<ItemPath, GetOperation>();
          for (List<GetOperation> operations : getOperations) {
            for (GetOperation operation : operations) {
              itemPaths2operations.put(new ItemPath(VcsUtil.getFilePath(operation.getTlocal()), operation.getTitem()), operation);
              TFSProgressUtil.checkCanceled(progressIndicator);
            }
          }
          if (!itemPaths2operations.isEmpty()) {
            UpdateStatusVisitor processor = new UpdateStatusVisitor(workspace, updatedFiles);
            // iterate over get operations: update files (get + updateLocalVersion), collect conflicting files (addLocalConflict)
            StatusProvider.visitByStatus(workspace, new ArrayList<ItemPath>(itemPaths2operations.keySet()), progressIndicator, processor);

            final List<ItemPath> toDownload = processor.getPathsToDownload();
            final List<LocalVersionUpdate> localVersions = new ArrayList<LocalVersionUpdate>();
            for (ItemPath path : toDownload) {

              TFSProgressUtil.setProgressText(progressIndicator, "Update " + path.getLocalPath());

              GetOperation operation = itemPaths2operations.get(path);
              LocalVersionUpdate localVersion = new LocalVersionUpdate();
              localVersion.setItemid(operation.getItemid());
              localVersion.setLver(operation.getLver());
              localVersion.setTlocal(operation.getTlocal());

              localVersions.add(localVersion);
              // TODO: rename if was renamed on server: depth-first order required 
              VersionControlServer.downloadItem(workspace, operation, true, true);

              TFSProgressUtil.checkCanceled(progressIndicator);
            }
            if (!localVersions.isEmpty()) {
              workspace.getServer().getVCS().updateLocalVersions(workspace.getName(), workspace.getOwnerName(), localVersions);
            }
          }
        }
      });
      for (FilePath path : unversioned) {
        updatedFiles.getGroupById(FileGroup.UNKNOWN_ID).add(FileUtil.toSystemIndependentName(path.getPath()));
      }
    }
    catch (TfsException e) {
      //noinspection ThrowableInstanceNeverThrown
      exceptions.add(new VcsException("Update failed.", e));
    }

    return new TFSUpdateSession(myVcs, contentRoots, exceptions, updatedFiles);
  }

  @Nullable
  public Configurable createConfigurable(final Collection<FilePath> files) {
    // TODO: will be implemented later. For now always update to latest version
    return null;
  }

}
