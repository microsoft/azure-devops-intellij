package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.update.UpdateEnvironment;
import com.intellij.openapi.vcs.update.UpdateSession;
import com.intellij.openapi.vcs.update.UpdatedFiles;
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
  public void fillGroups(final UpdatedFiles updatedFiles) {
  }

  /**
   * Performs the update/integrate/status operation.
   *
   * @param contentRoots      the content roots for which update/integrate/status was requested by the user.
   * @param updatedFiles      the holder for the results of the update/integrate/status operation.
   * @param progressIndicator the indicator that can be used to report the progress of the operation.
   * @return the update session instance, which can be used to get information about errors that have occurred
   *         during the operation and to perform additional post-update processing.
   * @throws ProcessCanceledException if the update operation has been cancelled by the user. Alternatively,
   *                                  cancellation can be reported by returning true from
   *                                  {@link UpdateSession#isCanceled}.
   */
  @NotNull
  public UpdateSession updateDirectories(@NotNull final FilePath[] contentRoots,
                                         final UpdatedFiles updatedFiles,
                                         final ProgressIndicator progressIndicator) throws ProcessCanceledException {

    final List<VcsException> exceptions = new ArrayList<VcsException>();
    // TODO: get version from Configurable created in createConfigurable (latest for now)
    final VersionSpec versionSpec = LatestVersionSpec.getLatest();

    try {
        WorkstationHelper.processByWorkspaces(Arrays.asList(contentRoots), new WorkstationHelper.VoidProcessDelegate() {
          public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths)
            throws TfsException {

            TFSProgressUtil.setProgressText(progressIndicator, "Request updating information");

            List<VersionControlServer.GetRequestParams> requests = new ArrayList<VersionControlServer.GetRequestParams>();
            for (ItemPath path : paths) {
              requests.add(new VersionControlServer.GetRequestParams(path, RecursionType.Full, versionSpec));
              TFSProgressUtil.checkCanceled(progressIndicator);
            }
            // query get operations for contentRoots
            Map<ItemPath, List<GetOperation>> getOperations = workspace.getServer().getVCS().get(workspace.getName(), workspace.getOwnerName(), requests);

            Map<ItemPath, GetOperation> itemPaths2operations = new HashMap<ItemPath, GetOperation>();
            for (List<GetOperation> operations : getOperations.values()) {
              for (GetOperation operation : operations) {
                itemPaths2operations.put(new ItemPath(VcsUtil.getFilePath(operation.getTlocal()), operation.getTitem()), operation);
                TFSProgressUtil.checkCanceled(progressIndicator);
              }
            }
            if (!itemPaths2operations.isEmpty()) {
              UpdateChangeProcessor processor = new UpdateChangeProcessor(updatedFiles);
              // iterate over get operations: update files (get + updateLocalVersion), collect conflicting files (addConflict)
              StatusProvider.processPaths(workspace, new ArrayList<ItemPath>(itemPaths2operations.keySet()), processor, progressIndicator);

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
                VersionControlServer.downloadItem(workspace, operation, true, true);

                TFSProgressUtil.checkCanceled(progressIndicator);
              }

              workspace.getServer().getVCS().updateLocalVersions(workspace.getName(), workspace.getOwnerName(), localVersions);
              final List<ItemPath> toMerge = processor.getPathsToMerge();
              // TODO: implement merge later
            }
          }
        });

    }
    catch (TfsException e) {
      exceptions.add(new VcsException("Update failed.", e));
    }
    return new UpdateSession() {
      @NotNull
      public List<VcsException> getExceptions() {
        return exceptions;
      }

      public void onRefreshFilesCompleted() {
        // TODO: resolve conflicts here
      }

      public boolean isCanceled() {
        // TODO: return completed status
        return false;
      }
    };
  }

  @Nullable
  public Configurable createConfigurable(final Collection<FilePath> files) {
    // TODO: will be implemented later. For now always update to latest version
    return null;
  }

}
