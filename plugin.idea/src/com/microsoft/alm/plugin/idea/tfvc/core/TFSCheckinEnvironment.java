// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.checkin.CheckinEnvironment;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.FunctionUtil;
import com.intellij.util.NullableFunction;
import com.intellij.util.PairConsumer;
import com.microsoft.alm.plugin.external.commands.AddCommand;
import com.microsoft.alm.plugin.external.commands.Command;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Handles VCS checkin features
 * TODO: comment back in the features as needed
 */
public class TFSCheckinEnvironment implements CheckinEnvironment {
    private static final String CHECKIN_OPERATION_NAME = "Checkin";

    @NotNull
    private final TFSVcs myVcs;

    public TFSCheckinEnvironment(final @NotNull TFSVcs vcs) {
        myVcs = vcs;
    }

    @Nullable
    public RefreshableOnComponent createAdditionalOptionsPanel(final CheckinProjectPanel checkinProjectPanel,
                                                               PairConsumer<Object, Object> additionalDataConsumer) {
//        boolean isAffected = false;
//        for (File file : checkinProjectPanel.getFiles()) {
//            if (TFSVcs.isUnderTFS(VcsUtil.getFilePath(file), checkinProjectPanel.getProject())) {
//                isAffected = true;
//                break;
//            }
//        }
//        if (!isAffected) {
//            return null;
//        }
//
//        final JComponent panel = new JPanel();
//        panel.setLayout(new BorderLayout(5, 0));
//
//        myVcs.getCheckinData().messageLabel = new BoldLabel() {
//
//            @Override
//            public JToolTip createToolTip() {
//                JToolTip toolTip = new JToolTip() {{
//                    setUI(new MultiLineTooltipUI());
//                }};
//                toolTip.setComponent(this);
//                return toolTip;
//            }
//
//        };
//
//        panel.add(myVcs.getCheckinData().messageLabel, BorderLayout.WEST);
//
//        final JButton configureButton = new JButton("Configure...");
//        panel.add(configureButton, BorderLayout.EAST);
//
//        configureButton.addActionListener(new ActionListener() {
//
//            public void actionPerformed(final ActionEvent event) {
//                CheckinParameters copy = myVcs.getCheckinData().parameters.createCopy();
//
//                CheckinParametersDialog d = new CheckinParametersDialog(checkinProjectPanel.getProject(), copy);
//                if (d.showAndGet()) {
//                    myVcs.getCheckinData().parameters = copy;
//                    updateMessage(myVcs.getCheckinData());
//                }
//            }
//        });
//
//        return new TFSAdditionalOptionsPanel(panel, checkinProjectPanel, configureButton);
        return null;
    }

//    public static void updateMessage(TFSVcs.CheckinData checkinData) {
//        if (checkinData.parameters == null) {
//            return;
//        }
//
//        final Pair<String, CheckinParameters.Severity> message = checkinData.parameters.getValidationMessage(CheckinParameters.Severity.BOTH);
//        if (message == null) {
//            checkinData.messageLabel.setText("<html>Ready to commit</html>"); // prevent bold
//            checkinData.messageLabel.setIcon(null);
//            checkinData.messageLabel.setToolTipText(null);
//        } else {
//            checkinData.messageLabel.setToolTipText(message.first);
//            if (message.second == CheckinParameters.Severity.ERROR) {
//                checkinData.messageLabel.setText("Errors found");
//                checkinData.messageLabel.setIcon(UIUtil.getBalloonErrorIcon());
//            } else {
//                checkinData.messageLabel.setText("Warnings found");
//                checkinData.messageLabel.setIcon(UIUtil.getBalloonWarningIcon());
//            }
//        }
//    }

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
        return CHECKIN_OPERATION_NAME;
    }

    @Nullable
    public List<VcsException> commit(final List<Change> changes,
                                     final String preparedComment,
                                     @NotNull NullableFunction<Object, Object> parametersHolder, Set<String> feedback) {
//        myVcs.getCheckinData().messageLabel = null;
//
//        final ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
//        final List<FilePath> files = new ArrayList<FilePath>();
//        for (Change change : changes) {
//            FilePath path = null;
//            ContentRevision beforeRevision = change.getBeforeRevision();
//            ContentRevision afterRevision = change.getAfterRevision();
//            if (afterRevision != null) {
//                path = afterRevision.getFile();
//            } else if (beforeRevision != null) {
//                path = beforeRevision.getFile();
//            }
//            if (path != null) {
//                files.add(path);
//            }
//        }
        final List<VcsException> errors = new ArrayList<VcsException>();
//    try {
//      WorkstationHelper.processByWorkspaces(files, false, myVcs.getProject(), new WorkstationHelper.VoidProcessDelegate() {
//        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
//          try {
//            TFSProgressUtil.setProgressText(progressIndicator, TFSBundle.message("loading.pending.changes"));
//            // get pending changes for given items
//            Collection<PendingChange> pendingChanges = workspace.getServer().getVCS()
//              .queryPendingSetsByLocalPaths(workspace.getName(), workspace.getOwnerName(), paths, RecursionType.None, myVcs.getProject(),
//                                            TFSBundle.message("loading.pending.changes"));
//
//            if (pendingChanges.isEmpty()) {
//              return;
//            }
//
//            Collection<String> checkIn = new ArrayList<String>();
//            // upload files
//            TFSProgressUtil.setProgressText(progressIndicator, TFSBundle.message("uploading.files"));
//            for (PendingChange pendingChange : pendingChanges) {
//              if (pendingChange.getType() == ItemType.File) {
//                ChangeTypeMask changeType = new ChangeTypeMask(pendingChange.getChg());
//                if (changeType.contains(ChangeType_type0.Edit) || changeType.contains(ChangeType_type0.Add)) {
//                  TFSProgressUtil
//                    .setProgressText2(progressIndicator, VersionControlPath.localPathFromTfsRepresentation(pendingChange.getLocal()));
//                  workspace.getServer().getVCS()
//                    .uploadItem(workspace, pendingChange, myVcs.getProject(), null);
//                }
//              }
//              checkIn.add(pendingChange.getItem());
//            }
//            TFSProgressUtil.setProgressText2(progressIndicator, "");
//
//            final WorkItemsCheckinParameters state = myVcs.getCheckinData().parameters.getWorkItems(workspace.getServer());
//            final Map<WorkItem, CheckinWorkItemAction> workItemActions =
//              state != null ? state.getWorkItemsActions() : Collections.<WorkItem, CheckinWorkItemAction>emptyMap();
//
//            List<Pair<String, String>> checkinNotes =
//              new ArrayList<Pair<String, String>>(myVcs.getCheckinData().parameters.getCheckinNotes(workspace.getServer()).size());
//            for (CheckinParameters.CheckinNote checkinNote : myVcs.getCheckinData().parameters.getCheckinNotes(workspace.getServer())) {
//              checkinNotes.add(Pair.create(checkinNote.name, StringUtil.notNullize(checkinNote.value)));
//            }
//
//            TFSProgressUtil.setProgressText(progressIndicator, TFSBundle.message("checking.in"));
//            ResultWithFailures<CheckinResult> result = workspace.getServer().getVCS()
//              .checkIn(workspace.getName(), workspace.getOwnerName(), checkIn, preparedComment, workItemActions, checkinNotes,
//                       myVcs.getCheckinData().parameters.getPolicyOverride(workspace.getServer()), myVcs.getProject(), null);
//            errors.addAll(TfsUtil.getVcsExceptions(result.getFailures()));
//
//            Collection<String> commitFailed = new ArrayList<String>(result.getFailures().size());
//            for (Failure failure : result.getFailures()) {
//              TFSVcs.assertTrue(failure.getItem() != null);
//              commitFailed.add(failure.getItem());
//            }
//
//            Collection<FilePath> invalidateRoots = new ArrayList<FilePath>(pendingChanges.size());
//            Collection<FilePath> invalidateFiles = new ArrayList<FilePath>();
//            // set readonly status for files
//            Collection<VirtualFile> makeReadOnly = new ArrayList<VirtualFile>();
//            for (PendingChange pendingChange : pendingChanges) {
//              TFSVcs.assertTrue(pendingChange.getItem() != null);
//              if (commitFailed.contains(pendingChange.getItem())) {
//                continue;
//              }
//
//              ChangeTypeMask changeType = new ChangeTypeMask(pendingChange.getChg());
//              if (pendingChange.getType() == ItemType.File) {
//                if (changeType.contains(ChangeType_type0.Edit) ||
//                    changeType.contains(ChangeType_type0.Add) ||
//                    changeType.contains(ChangeType_type0.Rename)) {
//                  VirtualFile file = VersionControlPath.getVirtualFile(pendingChange.getLocal());
//                  if (file != null && file.isValid()) {
//                    makeReadOnly.add(file);
//                  }
//                }
//              }
//
//              // TODO don't add recursive invalidate
//              // TODO if Rename, invalidate old and new items?
//              final FilePath path = VersionControlPath.getFilePath(pendingChange.getLocal(), pendingChange.getType() == ItemType.Folder);
//              invalidateRoots.add(path);
//              if (changeType.contains(ChangeType_type0.Add) || changeType.contains(ChangeType_type0.Rename)) {
//                // [IDEADEV-27087] invalidate parent folders since they can be implicitly checked in with child checkin
//                final VirtualFile vcsRoot = ProjectLevelVcsManager.getInstance(myVcs.getProject()).getVcsRootFor(path);
//                if (vcsRoot != null) {
//                  final FilePath vcsRootPath = TfsFileUtil.getFilePath(vcsRoot);
//                  for (FilePath parent = path.getParentPath();
//                       parent != null && parent.isUnder(vcsRootPath, false);
//                       parent = parent.getParentPath()) {
//                    invalidateFiles.add(parent);
//                  }
//                }
//              }
//            }
//
//            TfsFileUtil.setReadOnly(makeReadOnly, true);
//
//            TFSProgressUtil.setProgressText(progressIndicator, TFSBundle.message("updating.work.items"));
//            if (commitFailed.isEmpty()) {
//              CheckinResult checkinResult = result.getResult().iterator().next();
//              workspace.getServer().getVCS()
//                .updateWorkItemsAfterCheckin(workspace.getOwnerName(), workItemActions, checkinResult.getCset(), myVcs.getProject(),
//                                             null);
//            }
//
//            TfsFileUtil.markDirty(myVcs.getProject(), invalidateRoots, invalidateFiles);
//          }
//          catch (IOException e) {
//            //noinspection ThrowableInstanceNeverThrown
//            errors.add(new VcsException(e));
//          }
//        }
//      });
//    }
//    catch (TfsException e) {
//      //noinspection ThrowableInstanceNeverThrown
//      errors.add(new VcsException(e));
//    }
//    myVcs.getCheckinData().parameters = null;
//    myVcs.fireRevisionChanged();
        return errors;
    }

    public List<VcsException> commit(List<Change> changes, String preparedComment) {
        return commit(changes, preparedComment, FunctionUtil.<Object, Object>nullConstant(), null);
    }

    @Nullable
    public List<VcsException> scheduleMissingFileForDeletion(final List<FilePath> files) {
        final List<VcsException> errors = new ArrayList<VcsException>();
//    try {
//      WorkstationHelper.processByWorkspaces(files, false, myVcs.getProject(), new WorkstationHelper.VoidProcessDelegate() {
//        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) {
//          Collection<VcsException> schedulingErrors = ScheduleForDeletion.execute(myVcs.getProject(), workspace, paths);
//          errors.addAll(schedulingErrors);
//        }
//      });
//    }
//    catch (TfsException e) {
//      //noinspection ThrowableInstanceNeverThrown
//      errors.add(new VcsException(e));
//    }
        return errors;
    }

    @Nullable
    public List<VcsException> scheduleUnversionedFilesForAddition(final List<VirtualFile> files) {
        // TODO: schedule parent folders? (Jetbrains)
        final List<VcsException> exceptions = new ArrayList<VcsException>();
        try {
            final List<String> filesToAddPaths = new ArrayList<String>(files.size());
            for (final VirtualFile file : files) {
                filesToAddPaths.add(file.getPath());
            }
            final Command<List<String>> addCommand = new AddCommand(null, filesToAddPaths);
            final List<String> successfullyAdded = addCommand.runSynchronously();

            //check all files were added
            if (successfullyAdded.size() != filesToAddPaths.size()) {
                // remove all added files from original list of files to add to give us which files weren't added
                filesToAddPaths.removeAll(successfullyAdded);
                exceptions.add(new VcsException(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_ADD_ERROR, StringUtils.join(filesToAddPaths, ", "))));
            }
        } catch (RuntimeException e) {
            exceptions.add(new VcsException(e));
        }
        return exceptions;
    }

    public boolean keepChangeListAfterCommit(final ChangeList changeList) {
        return false;
    }

    @Override
    public boolean isRefreshAfterCommitNeeded() {
        return true;
    }

//    // TODO refactor this class
//    private class TFSAdditionalOptionsPanel implements CheckinChangeListSpecificComponent {
//        private final JComponent myPanel;
//        private final CheckinProjectPanel myCheckinProjectPanel;
//        private final JButton myConfigureButton;
//        private LocalChangeList myCurrentList;
//
//        public TFSAdditionalOptionsPanel(JComponent panel, CheckinProjectPanel checkinProjectPanel, JButton configureButton) {
//            myPanel = panel;
//            myCheckinProjectPanel = checkinProjectPanel;
//            myConfigureButton = configureButton;
//        }
//
//        public JComponent getComponent() {
//            return myPanel;
//        }
//
//        public void refresh() {
//        }
//
//        public void saveState() {
//        }
//
//        public void restoreState() {
//        }
//
//        public void onChangeListSelected(LocalChangeList list) {
//            if (myCurrentList == list) {
//                return;
//            }
//            myCurrentList = list;
//
//            if (!myCheckinProjectPanel.hasDiffs()) {
//                myPanel.setVisible(false);
//                return;
//            }
//
//            myPanel.setVisible(true);
//
//            try {
//                myVcs.getCheckinData().parameters = new CheckinParameters(myCheckinProjectPanel, true);
//                myConfigureButton.setEnabled(true);
//                updateMessage(myVcs.getCheckinData());
//            } catch (OperationFailedException e) {
//                myVcs.getCheckinData().parameters = null;
//                myConfigureButton.setEnabled(false);
//                myVcs.getCheckinData().messageLabel.setIcon(UIUtil.getBalloonErrorIcon());
//                myVcs.getCheckinData().messageLabel.setText("Validation failed");
//                myVcs.getCheckinData().messageLabel.setToolTipText(e.getMessage());
//            }
//        }
//
//    }
}
