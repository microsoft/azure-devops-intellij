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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.checkin.CheckinEnvironment;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.MultiLineTooltipUI;
import com.intellij.ui.components.labels.BoldLabel;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.ui.UIUtil;
import com.intellij.vcsUtil.VcsUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.ChangeType;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.core.tfs.operations.ScheduleForAddition;
import org.jetbrains.tfsIntegration.core.tfs.operations.ScheduleForDeletion;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItem;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.*;
import org.jetbrains.tfsIntegration.ui.CheckinParametersDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class TFSCheckinEnvironment implements CheckinEnvironment {
  private final @NotNull TFSVcs myVcs;

  private Map<ServerInfo, CheckinParameters> myCheckinParameters;
  private String myValidationError;

  public TFSCheckinEnvironment(final @NotNull TFSVcs vcs) {
    myVcs = vcs;
  }

  Map<ServerInfo, CheckinParameters> getCheckinParameters() {
    return myCheckinParameters;
  }

  String getValidationError() {
    return myValidationError;
  }

  @Nullable
  public RefreshableOnComponent createAdditionalOptionsPanel(final CheckinProjectPanel checkinProjectPanel) {
    final JComponent panel = new JPanel();
    panel.setLayout(new BorderLayout(5, 0));

    final JLabel messageLabel = new BoldLabel() {

      @Override
      public JToolTip createToolTip() {
        JToolTip toolTip = new JToolTip() {{
          setUI(new MultiLineTooltipUI());
        }};
        toolTip.setComponent(this);
        return toolTip;
      }

    };

    panel.add(messageLabel, BorderLayout.WEST);

    final JButton configureButton = new JButton("Configure...");
    panel.add(configureButton, BorderLayout.EAST);

    configureButton.addActionListener(new ActionListener() {

      public void actionPerformed(final ActionEvent event) {
        LinkedHashMap<ServerInfo, CheckinParameters> paramsToEdit =
          new LinkedHashMap<ServerInfo, CheckinParameters>(myCheckinParameters.size());
        for (Map.Entry<ServerInfo, CheckinParameters> entry : myCheckinParameters.entrySet()) {
          paramsToEdit.put(entry.getKey(), entry.getValue().createCopy());
        }

        CheckinParametersDialog d = new CheckinParametersDialog(myVcs.getProject(), paramsToEdit);
        d.show();
        if (d.isOK()) {
          myCheckinParameters = paramsToEdit;
          updateMessage(messageLabel);
        }
      }
    });

    return new RefreshableOnComponent() {
      public JComponent getComponent() {
        return panel;
      }

      public void refresh() {
        System.out.println("sss");
      }

      public void saveState() {
      }

      public void restoreState() {
        TfsExecutionUtil.ResultWithError<Map<ServerInfo, CheckinParameters>> result =
          loadCheckinParameters(myVcs.getProject(), checkinProjectPanel.getFiles());

        if (result.cancelled) {
          myCheckinParameters = Collections.emptyMap();
          messageLabel.setIcon(UIUtil.getBalloonErrorIcon());
          messageLabel.setText("Validation cancelled");
          myValidationError = "Cancelled by user";
          messageLabel.setToolTipText(null);
          configureButton.setEnabled(false);
        }
        else if (result.error != null) {
          myCheckinParameters = Collections.emptyMap();
          messageLabel.setIcon(UIUtil.getBalloonErrorIcon());
          messageLabel.setText("Validation failed");
          myValidationError = result.error.getMessage();
          messageLabel.setToolTipText(result.error.getMessage());
          configureButton.setEnabled(false);
        }
        else {
          configureButton.setEnabled(true);
          myCheckinParameters = result.result;
          myValidationError = null;
          updateMessage(messageLabel);
        }
      }
    };
  }

  private void updateMessage(JLabel messageLabel) {
    String message = null;
    message = CheckinParameters.validate(myCheckinParameters, Condition.TRUE);

    if (message == null) {
      messageLabel.setText("Ready to commit");
      messageLabel.setIcon(null);
      messageLabel.setToolTipText(null);
    }
    else {
      messageLabel.setText("Problems found");
      messageLabel.setIcon(UIUtil.getBalloonWarningIcon());
      messageLabel.setToolTipText(message);
    }
  }

  private static TfsExecutionUtil.ResultWithError<Map<ServerInfo, CheckinParameters>> loadCheckinParameters(Project project,
                                                                                                            final Collection<File> files) {
    return TfsExecutionUtil
      .executeInBackground("Validating Checkin", project, new TfsExecutionUtil.Process<Map<ServerInfo, CheckinParameters>>() {
        public Map<ServerInfo, CheckinParameters> run() throws TfsException, VcsException {
          Collection<FilePath> filePaths = new ArrayList<FilePath>(files.size());
          for (File file : files) {
            filePaths.add(VcsUtil.getFilePath(file));
          }

          final MultiMap<ServerInfo, String> serverToProjects = new MultiMap<ServerInfo, String>() {
            @Override
            protected Collection<String> createCollection() {
              return new THashSet<String>();
            }
          };
          WorkstationHelper.processByWorkspaces(filePaths, false, new WorkstationHelper.VoidProcessDelegate() {
            public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
              for (ItemPath path : paths) {
                serverToProjects.putValue(workspace.getServer(), VersionControlPath.getPathToProject(path.getServerPath()));
              }
            }
          });

          List<ServerInfo> sortedServers = new ArrayList<ServerInfo>(serverToProjects.keySet());
          Collections.sort(sortedServers, new Comparator<ServerInfo>() {
            public int compare(ServerInfo o1, ServerInfo o2) {
              return o1.getUri().compareTo(o2.getUri());
            }
          });


          Map<ServerInfo, CheckinParameters> result = new LinkedHashMap<ServerInfo, CheckinParameters>();
          // factorize different team projects definitions by name and sort them by display order field
          for (ServerInfo server : sortedServers) {
            final Collection<String> teamProjects = serverToProjects.get(server);
            final List<CheckinNoteFieldDefinition> checkinNoteDefinitions = server.getVCS().queryCheckinNoteDefinition(teamProjects);
            Map<String, CheckinNoteFieldDefinition> nameToDefinition = new HashMap<String, CheckinNoteFieldDefinition>();
            for (CheckinNoteFieldDefinition definition : checkinNoteDefinitions) {
              if (!nameToDefinition.containsKey(definition.getName()) || definition.getReq()) {
                nameToDefinition.put(definition.getName(), definition);
              }
            }
            List<CheckinNoteFieldDefinition> sortedDefinitions = new ArrayList<CheckinNoteFieldDefinition>(nameToDefinition.values());
            Collections.sort(sortedDefinitions, new Comparator<CheckinNoteFieldDefinition>() {
              public int compare(final CheckinNoteFieldDefinition o1, final CheckinNoteFieldDefinition o2) {
                return o1.get_do() - o2.get_do();
              }
            });

            result.put(server, new CheckinParameters(sortedDefinitions, new WorkItemsCheckinParameters()));
          }
          return result;
        }
      });
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
    return "Check In";
  }

  @Nullable
  public List<VcsException> commit(final List<Change> changes, final String preparedComment) {
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
      WorkstationHelper.processByWorkspaces(files, false, new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          try {
            TFSProgressUtil.setProgressText(progressIndicator, "Preparing");
            // get pending changes for given items
            Collection<PendingChange> pendingChanges = workspace.getServer().getVCS()
              .queryPendingSetsByLocalPaths(workspace.getName(), workspace.getOwnerName(), paths, RecursionType.None);

            if (pendingChanges.isEmpty()) {
              return;
            }

            Collection<String> checkIn = new ArrayList<String>();
            // upload files
            TFSProgressUtil.setProgressText(progressIndicator, "Uploading changes");
            for (PendingChange pendingChange : pendingChanges) {
              if (pendingChange.getType() == ItemType.File) {
                EnumMask<ChangeType> changeType = EnumMask.fromString(ChangeType.class, pendingChange.getChg());
                if (changeType.contains(ChangeType.Edit) || changeType.contains(ChangeType.Add)) {
                  TFSProgressUtil
                    .setProgressText2(progressIndicator, VersionControlPath.localPathFromTfsRepresentation(pendingChange.getLocal()));
                  workspace.getServer().getVCS().uploadItem(workspace, pendingChange);
                }
              }
              checkIn.add(pendingChange.getItem());
            }
            TFSProgressUtil.setProgressText2(progressIndicator, "");

            final WorkItemsCheckinParameters state = myCheckinParameters.get(workspace.getServer()).getWorkItems();
            final Map<WorkItem, CheckinWorkItemAction> workItemActions =
              state != null ? state.getWorkItemsActions() : Collections.<WorkItem, CheckinWorkItemAction>emptyMap();
            TFSProgressUtil.setProgressText(progressIndicator, "");
            List<Pair<String, String>> checkinNotes =
              new ArrayList<Pair<String, String>>(myCheckinParameters.get(workspace.getServer()).getCheckinNotes().size());
            for (CheckinParameters.CheckinNote checkinNote : myCheckinParameters.get(workspace.getServer()).getCheckinNotes()) {
              checkinNotes.add(Pair.create(checkinNote.name, StringUtil.notNullize(checkinNote.value)));
            }

            ResultWithFailures<CheckinResult> result = workspace.getServer().getVCS()
              .checkIn(workspace.getName(), workspace.getOwnerName(), checkIn, preparedComment, workItemActions, checkinNotes);
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

              EnumMask<ChangeType> changeType = EnumMask.fromString(ChangeType.class, pendingChange.getChg());
              if (pendingChange.getType() == ItemType.File) {
                if (changeType.contains(ChangeType.Edit) || changeType.contains(ChangeType.Add) || changeType.contains(ChangeType.Rename)) {
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
              if (changeType.contains(ChangeType.Add)) {
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

            if (commitFailed.isEmpty()) {
              CheckinResult checkinResult = result.getResult().iterator().next();
              workspace.getServer().getVCS()
                .updateWorkItemsAfterCheckin(workspace.getOwnerName(), workItemActions, checkinResult.getCset());
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

  public List<VcsException> commit(List<Change> changes, String preparedComment, Object parameters) {
    return commit(changes, preparedComment);
  }

  @Nullable
  public List<VcsException> scheduleMissingFileForDeletion(final List<FilePath> files) {
    final List<VcsException> errors = new ArrayList<VcsException>();
    try {
      WorkstationHelper.processByWorkspaces(files, false, new WorkstationHelper.VoidProcessDelegate() {
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
        WorkstationHelper.processByWorkspaces(TfsFileUtil.getFilePaths(files), false, new WorkstationHelper.VoidProcessDelegate() {
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
        exceptions.add(new VcsException("No Team Foundation Server mapping found for the following items: " + s.toString()));
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

}
