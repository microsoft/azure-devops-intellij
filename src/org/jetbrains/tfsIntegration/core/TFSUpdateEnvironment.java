package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.update.UpdateEnvironment;
import com.intellij.openapi.vcs.update.UpdateSession;
import com.intellij.openapi.vcs.update.UpdatedFiles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class TFSUpdateEnvironment implements UpdateEnvironment {

  public void fillGroups(final UpdatedFiles updatedFiles) {}

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
  public UpdateSession updateDirectories(@NotNull final FilePath[] contentRoots, final UpdatedFiles updatedFiles, final ProgressIndicator progressIndicator)
    throws ProcessCanceledException {
    // TODO: 1. get version from Configurable created in createConfigurable (latest for now)
    // TODO: 2. query get operations for contentRoots 
    // TODO: 3. iterate over get operations: update files (get + updateLocalVersion), collect conflicting files (addConflict)
    return new UpdateSession() {
      @NotNull
      public List<VcsException> getExceptions() {
        // TODO: return exceptions 
        return Collections.emptyList();
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
