package org.jetbrains.tfsIntegration.core.tfs.conflicts;

import com.intellij.openapi.diff.ActionButtonPresentation;
import com.intellij.openapi.diff.DiffManager;
import com.intellij.openapi.diff.MergeRequest;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.peer.PeerFactory;
import com.intellij.vcsUtil.VcsRunnable;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.core.revision.TFSContentRevision;
import org.jetbrains.tfsIntegration.core.tfs.ChangeType;
import org.jetbrains.tfsIntegration.core.tfs.VersionControlServer;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.operations.ApplyGetOperations;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.*;
import org.jetbrains.tfsIntegration.ui.ConflictData;
import org.jetbrains.tfsIntegration.ui.MergeNameDialog;

import java.io.IOException;
import java.util.Arrays;

public class ResolveConflictHelper {
  private Project myProject;
  private WorkspaceInfo myWorkspace;

  public ResolveConflictHelper(final Project project, final WorkspaceInfo workspace) {
    myProject = project;
    myWorkspace = workspace;
  }

  public void conflictResolved(final Conflict conflict,
                               final ResolutionType nameResolutionType,
                               final ResolutionType contentResolutionType,
                               final String newLocalPath) {
    // send "conflict resolved" to server
    try {
      Resolution resolution = Resolution.AcceptMerge;
      if (contentResolutionType == ResolutionType.ACCEPT_YOURS && nameResolutionType == ResolutionType.ACCEPT_YOURS) {
        resolution = Resolution.AcceptYours;
      }
      if (contentResolutionType == ResolutionType.ACCEPT_THEIRS && nameResolutionType == ResolutionType.ACCEPT_THEIRS) {
        resolution = Resolution.AcceptTheirs;
      }
      VersionControlServer.ResolveConflictParams resolveConflictParams =
        new VersionControlServer.ResolveConflictParams(conflict.getCid(), resolution, LockLevel.Unchanged, -2, newLocalPath);

      ResolveResponse response =
        myWorkspace.getServer().getVCS().resolveConflict(myWorkspace.getName(), myWorkspace.getOwnerName(), resolveConflictParams);

      final ArrayOfGetOperation getOperations;
      final ApplyGetOperations.ProcessMode operationType;
      if (resolution == Resolution.AcceptTheirs) {
        getOperations = response.getUndoOperations();
        operationType = ApplyGetOperations.ProcessMode.UNDO;
        TFSVcs.assertTrue(response.getResolveResult().getGetOperation() == null);
      }
      else if (resolution == Resolution.AcceptYoursRenameTheirs) {
        // Currently it is not possible in our implementation...
        TFSVcs.error("AcceptYoursRenameTheirs resolution type not supported");
        getOperations = null;
        operationType = null;
      }
      else { //  resolution == Resolution.AcceptMerge || resolution == Resolution.AcceptYours
        getOperations = response.getResolveResult();
        operationType = ApplyGetOperations.ProcessMode.RESOLVE;
        TFSVcs.assertTrue(response.getUndoOperations().getGetOperation() == null);
      }
      // TODO check for null not needed?
      if (getOperations != null && getOperations.getGetOperation() != null) {
        ApplyGetOperations.DownloadMode downloadMode = resolution == Resolution
          .AcceptTheirs ? ApplyGetOperations.DownloadMode.FORCE : ApplyGetOperations.DownloadMode.ALLOW;
        ApplyGetOperations.execute(myWorkspace, Arrays.asList(getOperations.getGetOperation()), null, null, downloadMode, operationType);
      }
    }
    catch (TfsException e) {
      //noinspection ThrowableInstanceNeverThrown
      AbstractVcsHelper.getInstance(myProject).showError(new VcsException("Conflict resolution failed.", e), TFSVcs.TFS_NAME);
    }
  }

  @NotNull
  public ConflictData getConflictData(final @NotNull Conflict conflict) throws VcsException {
    final ConflictData data = new ConflictData();
    VcsRunnable runnable = new VcsRunnable() {
      public void run() throws VcsException {

        try {
          // names
          FilePath sourceLocalPath = myWorkspace.findLocalPathByServerPath(conflict.getYsitem());
          data.sourceLocalName = sourceLocalPath != null ? FileUtil.toSystemDependentName(sourceLocalPath.getPath()) : null;
          FilePath targetLocalPath = myWorkspace.findLocalPathByServerPath(conflict.getTsitem());
          data.targetLocalName = targetLocalPath != null ? FileUtil.toSystemDependentName(targetLocalPath.getPath()) : null;

          // content
          if (conflict.getYtype() == ItemType.File) {
            String original = new TFSContentRevision(myWorkspace, conflict.getBitemid(), conflict.getBver()).getContent();
            data.baseContent = original != null ? original : ""; // TODO: why null is not OK?
            String current = CurrentContentRevision.create(VcsUtil.getFilePath(conflict.getSrclitem())).getContent();
            data.localContent = current != null ? current : "";
            String last = new TFSContentRevision(myWorkspace, conflict.getTitemid(), conflict.getTver()).getContent();
            data.serverContent = last != null ? last : "";
          }
        }
        catch (TfsException e) {
          throw new VcsException("Unable to get content for item " + data.sourceLocalName);
        }
      }
    };
    VcsUtil.runVcsProcessWithProgress(runnable, "Prepare merge data...", false, myProject);
    return data;
  }

  public void acceptMerge(final @NotNull Conflict conflict, final ConflictData conflictData) throws TfsException {
    ResolutionType nameResolutionType = isNameConflict(conflict) ? ResolutionType.IGNORED : ResolutionType.NO_CONFLICT;
    ResolutionType contentResolutionType = isContentConflict(conflict) ? ResolutionType.IGNORED : ResolutionType.NO_CONFLICT;
    String localName = null;

    // merge names if needed
    if (ChangeType.fromString(conflict.getYchg()).contains(ChangeType.Value.Rename)) {
      MergeNameDialog d = new MergeNameDialog(conflict.getYsitem(), conflict.getTsitem());
      d.show();
      if (d.isOK()) {
        FilePath newLocalPath = myWorkspace.findLocalPathByServerPath(d.getSelectedName());
        if (newLocalPath != null) {
          localName = FileUtil.toSystemDependentName(newLocalPath.getPath());
          nameResolutionType = ResolutionType.MERGED;
        }
      }
    }
    else {
      localName = conflictData.targetLocalName;
    }

    // if content conflict present show merge dialog
    if (conflict.getYtype() == ItemType.File && contentResolutionType == ResolutionType.IGNORED) {
      final VirtualFile vFile = VcsUtil.getVirtualFile(conflictData.sourceLocalName);
      if (vFile != null) {
        MergeRequest request = PeerFactory.getInstance().getDiffRequestFactory().createMergeRequest(
          StreamUtil.convertSeparators(conflictData.serverContent), StreamUtil.convertSeparators(conflictData.localContent),
          StreamUtil.convertSeparators(conflictData.baseContent), vFile, myProject, ActionButtonPresentation.createApplyButton());

        request.setWindowTitle("Merge " + localName);
        request.setVersionTitles(new String[]{"Server content (rev. " + conflict.getTver() + ")", "Merge result", "Local content"});
        // TODO call canShow() first
        DiffManager.getInstance().getDiffTool().show(request);
        contentResolutionType = ResolutionType.MERGED;
      }
    }
    conflictResolved(conflict, nameResolutionType, contentResolutionType, localName);
  }

  public void acceptYours(final @NotNull Conflict conflict) {
    conflictResolved(conflict, ResolutionType.ACCEPT_YOURS, ResolutionType.ACCEPT_YOURS, conflict.getSrclitem());
  }

  public void acceptTheirs(final @NotNull Conflict conflict) throws TfsException, IOException {
    conflictResolved(conflict, ResolutionType.ACCEPT_THEIRS, ResolutionType.ACCEPT_THEIRS, null);
  }

  public static boolean isNameConflict(final @NotNull Conflict conflict) {
    return ChangeType.fromString(conflict.getYchg()).contains(ChangeType.Value.Rename);
  }

  public static boolean isContentConflict(final @NotNull Conflict conflict) {
    return ChangeType.fromString(conflict.getYchg()).contains(ChangeType.Value.Edit);
  }
}
