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
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vcs.checkin.CheckinChangeListSpecificComponent;
import com.intellij.openapi.vcs.checkin.CheckinEnvironment;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.MultiLineTooltipUI;
import com.intellij.ui.components.labels.BoldLabel;
import com.intellij.util.NullableFunction;
import com.intellij.util.PairConsumer;
import com.intellij.util.ui.UIUtil;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.*;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.checkin.CheckinParameters;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.core.tfs.operations.ScheduleForAddition;
import org.jetbrains.tfsIntegration.core.tfs.operations.ScheduleForDeletion;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItem;
import org.jetbrains.tfsIntegration.exceptions.OperationFailedException;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.ui.CheckinParametersDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class TFSCheckinEnvironment implements CheckinEnvironment {
  private final @NotNull TFSVcs myVcs;

  @Nullable private CheckinParameters myParameters;
  private JLabel myMessageLabel;

  public TFSCheckinEnvironment(final @NotNull TFSVcs vcs) {
    myVcs = vcs;
  }

  @Nullable
  CheckinParameters getCheckinParameters() {
    return myParameters;
  }

  @Nullable
  public RefreshableOnComponent createAdditionalOptionsPanel(final CheckinProjectPanel checkinProjectPanel,
                                                             PairConsumer<Object, Object> additionalDataConsumer) {
    final JComponent panel = new JPanel();
    panel.setLayout(new BorderLayout(5, 0));

    myMessageLabel = new BoldLabel() {

      @Override
      public JToolTip createToolTip() {
        JToolTip toolTip = new JToolTip() {{
          setUI(new MultiLineTooltipUI());
        }};
        toolTip.setComponent(this);
        return toolTip;
      }

    };

    panel.add(myMessageLabel, BorderLayout.WEST);

    final JButton configureButton = new JButton("Configure...");
    panel.add(configureButton, BorderLayout.EAST);

    configureButton.addActionListener(new ActionListener() {

      public void actionPerformed(final ActionEvent event) {
        CheckinParameters copy = myParameters.createCopy();

        CheckinParametersDialog d = new CheckinParametersDialog(checkinProjectPanel.getProject(), copy);
        d.show();
        if (d.isOK()) {
          myParameters = copy;
          updateMessage();
        }
      }
    });

    return new TFSAdditionalOptionsPanel(panel, checkinProjectPanel, configureButton);
  }

  public void updateMessage() {
    if (myParameters == null) {
      return;
    }

    final Pair<String, CheckinParameters.Severity> message = myParameters.getValidationMessage(CheckinParameters.Severity.BOTH);
    if (message == null) {
      myMessageLabel.setText("<html>Ready to commit</html>"); // prevent bold
      myMessageLabel.setIcon(null);
      myMessageLabel.setToolTipText(null);
    }
    else {
      myMessageLabel.setToolTipText(message.first);
      if (message.second == CheckinParameters.Severity.ERROR) {
        myMessageLabel.setText("Errors found");
        myMessageLabel.setIcon(UIUtil.getBalloonErrorIcon());
      }
      else {
        myMessageLabel.setText("Warnings found");
        myMessageLabel.setIcon(UIUtil.getBalloonWarningIcon());
      }
    }
  }

  @Nullable
  public String getDefaultMessageFor(final FilePath[] filesToCheckin) {
    return null;
  }

  @Nullable
  @NonNls
  public String getHelpId() {
    return null;  // TODO: help id for check in
  }

  public String getCheckinOperationName() {
    return "Checkin";
  }

  @Nullable
  public List<VcsException> commit(final List<Change> changes,
                                   final String preparedComment,
                                   @NotNull NullableFunction<Object, Object> parametersHolder) {
    myMessageLabel = null;

    final ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
    final List<FilePath> files = new ArrayList<FilePath>();
    for (Change change : changes) {
      FilePath path = null;
      ContentRevision beforeRevision = change.getBeforeRevision();
      ContentRevision afterRevision = change.getAfterRevision();
      if (afterRevision != null) {
        path = afterRevision.getFile();
      }
      else if (beforeRevision != null) {
        path = beforeRevision.getFile();
      }
      if (path != null) {
        files.add(path);
      }
    }
    final List<VcsException> errors = new ArrayList<VcsException>();
    try {
      WorkstationHelper.processByWorkspaces(files, false, myVcs.getProject(), new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          try {
            TFSProgressUtil.setProgressText(progressIndicator, TFSBundle.message("loading.pending.changes"));
            // get pending changes for given items
            Collection<PendingChange> pendingChanges = workspace.getServer().getVCS()
              .queryPendingSetsByLocalPaths(workspace.getName(), workspace.getOwnerName(), paths, RecursionType.None, myVcs.getProject(),
                                            TFSBundle.message("loading.pending.changes"));

            if (pendingChanges.isEmpty()) {
              return;
            }

            Collection<String> checkIn = new ArrayList<String>();
            // upload files
            TFSProgressUtil.setProgressText(progressIndicator, TFSBundle.message("uploading.files"));
            for (PendingChange pendingChange : pendingChanges) {
              if (pendingChange.getType() == ItemType.File) {
                ChangeTypeMask changeType = new ChangeTypeMask(pendingChange.getChg());
                if (changeType.contains(ChangeType_type0.Edit) || changeType.contains(ChangeType_type0.Add)) {
                  TFSProgressUtil
                    .setProgressText2(progressIndicator, VersionControlPath.localPathFromTfsRepresentation(pendingChange.getLocal()));
                  workspace.getServer().getVCS()
                    .uploadItem(workspace, pendingChange, myVcs.getProject(), null);
                }
              }
              checkIn.add(pendingChange.getItem());
            }
            TFSProgressUtil.setProgressText2(progressIndicator, "");

            final WorkItemsCheckinParameters state = myParameters.getWorkItems(workspace.getServer());
            final Map<WorkItem, CheckinWorkItemAction> workItemActions =
              state != null ? state.getWorkItemsActions() : Collections.<WorkItem, CheckinWorkItemAction>emptyMap();

            List<Pair<String, String>> checkinNotes =
              new ArrayList<Pair<String, String>>(myParameters.getCheckinNotes(workspace.getServer()).size());
            for (CheckinParameters.CheckinNote checkinNote : myParameters.getCheckinNotes(workspace.getServer())) {
              checkinNotes.add(Pair.create(checkinNote.name, StringUtil.notNullize(checkinNote.value)));
            }

            TFSProgressUtil.setProgressText(progressIndicator, TFSBundle.message("checking.in"));
            ResultWithFailures<CheckinResult> result = workspace.getServer().getVCS()
              .checkIn(workspace.getName(), workspace.getOwnerName(), checkIn, preparedComment, workItemActions, checkinNotes,
                       myParameters.getPolicyOverride(workspace.getServer()), myVcs.getProject(), null);
            errors.addAll(TfsUtil.getVcsExceptions(result.getFailures()));

            Collection<String> commitFailed = new ArrayList<String>(result.getFailures().size());
            for (Failure failure : result.getFailures()) {
              TFSVcs.assertTrue(failure.getItem() != null);
              commitFailed.add(failure.getItem());
            }

            Collection<FilePath> invalidateRoots = new ArrayList<FilePath>(pendingChanges.size());
            Collection<FilePath> invalidateFiles = new ArrayList<FilePath>();
            // set readonly status for files
            Collection<VirtualFile> makeReadOnly = new ArrayList<VirtualFile>();
            for (PendingChange pendingChange : pendingChanges) {
              TFSVcs.assertTrue(pendingChange.getItem() != null);
              if (commitFailed.contains(pendingChange.getItem())) {
                continue;
              }

              ChangeTypeMask changeType = new ChangeTypeMask(pendingChange.getChg());
              if (pendingChange.getType() == ItemType.File) {
                if (changeType.contains(ChangeType_type0.Edit) ||
                    changeType.contains(ChangeType_type0.Add) ||
                    changeType.contains(ChangeType_type0.Rename)) {
                  VirtualFile file = VersionControlPath.getVirtualFile(pendingChange.getLocal());
                  if (file != null && file.isValid()) {
                    makeReadOnly.add(file);
                  }
                }
              }

              // TODO don't add recursive invalidate
              // TODO if Rename, invalidate old and new items?
              final FilePath path = VersionControlPath.getFilePath(pendingChange.getLocal(), pendingChange.getType() == ItemType.Folder);
              invalidateRoots.add(path);
              if (changeType.contains(ChangeType_type0.Add)) {
                // [IDEADEV-27087] invalidate parent folders since they can be implicitly checked in with child checkin
                final VirtualFile vcsRoot = ProjectLevelVcsManager.getInstance(myVcs.getProject()).getVcsRootFor(path);
                if (vcsRoot != null) {
                  final FilePath vcsRootPath = TfsFileUtil.getFilePath(vcsRoot);
                  for (FilePath parent = path.getParentPath();
                       parent != null && parent.isUnder(vcsRootPath, false);
                       parent = parent.getParentPath()) {
                    invalidateFiles.add(parent);
                  }
                }
              }
            }

            TfsFileUtil.setReadOnly(makeReadOnly, true);

            TFSProgressUtil.setProgressText(progressIndicator, TFSBundle.message("updating.work.items"));
            if (commitFailed.isEmpty()) {
              CheckinResult checkinResult = result.getResult().iterator().next();
              workspace.getServer().getVCS()
                .updateWorkItemsAfterCheckin(workspace.getOwnerName(), workItemActions, checkinResult.getCset(), myVcs.getProject(),
                                             null);
            }

            TfsFileUtil.markDirty(myVcs.getProject(), invalidateRoots, invalidateFiles);
          }
          catch (IOException e) {
            //noinspection ThrowableInstanceNeverThrown
            errors.add(new VcsException(e));
          }
        }
      });
    }
    catch (TfsException e) {
      //noinspection ThrowableInstanceNeverThrown
      errors.add(new VcsException(e));
    }
    myVcs.fireRevisionChanged();
    return errors;
  }

  public List<VcsException> commit(List<Change> changes, String preparedComment) {
    return commit(changes, preparedComment, NullableFunction.NULL);
  }

  @Nullable
  public List<VcsException> scheduleMissingFileForDeletion(final List<FilePath> files) {
    final List<VcsException> errors = new ArrayList<VcsException>();
    try {
      WorkstationHelper.processByWorkspaces(files, false, myVcs.getProject(), new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) {
          Collection<VcsException> schedulingErrors = ScheduleForDeletion.execute(myVcs.getProject(), workspace, paths);
          errors.addAll(schedulingErrors);
        }
      });
    }
    catch (TfsException e) {
      //noinspection ThrowableInstanceNeverThrown
      errors.add(new VcsException(e));
    }
    return errors;
  }

  @Nullable
  public List<VcsException> scheduleUnversionedFilesForAddition(final List<VirtualFile> files) {
    // TODO: schedule parent folders?
    final List<VcsException> exceptions = new ArrayList<VcsException>();
    try {
      final List<FilePath> orphans =
        WorkstationHelper
          .processByWorkspaces(TfsFileUtil.getFilePaths(files), false, myVcs.getProject(), new WorkstationHelper.VoidProcessDelegate() {
            public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) {
              Collection<VcsException> schedulingErrors = ScheduleForAddition.execute(myVcs.getProject(), workspace, paths);
              exceptions.addAll(schedulingErrors);
            }
          });
      if (!orphans.isEmpty()) {
        StringBuilder s = new StringBuilder();
        for (FilePath orpan : orphans) {
          if (s.length() > 0) {
            s.append("\n");
          }
          s.append(orpan.getPresentableUrl());
        }
        exceptions.add(new VcsException("Team Foundation Server mapping not found for: " + s.toString()));
      }
    }
    catch (TfsException e) {
      //noinspection ThrowableInstanceNeverThrown
      exceptions.add(new VcsException(e));
    }
    return exceptions;
  }

  public boolean keepChangeListAfterCommit(final ChangeList changeList) {
    return false;
  }

  // TODO refactor this class
  private class TFSAdditionalOptionsPanel implements CheckinChangeListSpecificComponent {
    private final JComponent myPanel;
    private final CheckinProjectPanel myCheckinProjectPanel;
    private final JButton myConfigureButton;
    private LocalChangeList myCurrentList;

    public TFSAdditionalOptionsPanel(JComponent panel, CheckinProjectPanel checkinProjectPanel, JButton configureButton) {
      myPanel = panel;
      myCheckinProjectPanel = checkinProjectPanel;
      myConfigureButton = configureButton;
    }

    public JComponent getComponent() {
      return myPanel;
    }

    public void refresh() {
    }

    public void saveState() {
    }

    public void restoreState() {
    }

    public void onChangeListSelected(LocalChangeList list) {
      if (myCurrentList == list) {
        return;
      }
      myCurrentList = list;

      if (!myCheckinProjectPanel.hasDiffs()) {
        myPanel.setVisible(false);
        return;
      }

      myPanel.setVisible(true);

      try {
        myParameters = new CheckinParameters(myCheckinProjectPanel, true);
        myConfigureButton.setEnabled(true);
        updateMessage();
      }
      catch (OperationFailedException e) {
        myParameters = null;
        myConfigureButton.setEnabled(false);
        myMessageLabel.setIcon(UIUtil.getBalloonErrorIcon());
        myMessageLabel.setText("Validation failed");
        myMessageLabel.setToolTipText(e.getMessage());
      }
    }

  }
}
