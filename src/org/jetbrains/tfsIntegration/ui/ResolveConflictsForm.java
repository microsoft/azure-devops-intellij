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
package org.jetbrains.tfsIntegration.ui;

import com.intellij.openapi.diff.ActionButtonPresentation;
import com.intellij.openapi.diff.DiffManager;
import com.intellij.openapi.diff.MergeRequest;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.peer.PeerFactory;
import com.intellij.vcsUtil.VcsRunnable;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.ResolutionData;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.core.revision.TFSContentRevision;
import org.jetbrains.tfsIntegration.core.tfs.ChangeType;
import org.jetbrains.tfsIntegration.core.tfs.VersionControlServer;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.version.WorkspaceVersionSpec;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class ResolveConflictsForm {
  private JTable myItemsTable;
  private JPanel myContentPanel;
  private JButton myAcceptYoursButton;
  private JButton myAcceptTheirsButton;
  private JButton myMergeButton;

  private ItemsTableModel myItemsTableModel;
  private List<Conflict> myUnresolvedConflicts = new ArrayList<Conflict>();
  private Map<Conflict, ConflictData> myConflict2ConflictData = new HashMap<Conflict, ConflictData>();
  private Map<Conflict, ResolutionData> myMergeResult = new HashMap<Conflict, ResolutionData>();
  private Project myProject;
  private Map<Conflict, WorkspaceInfo> myConflict2workspace;

  public JComponent getPanel() {
    return myContentPanel;
  }

  @NotNull
  public ConflictData getConflictData(final @NotNull Conflict conflict) throws VcsException {
    final ConflictData data = new ConflictData();
    VcsRunnable runnable = new VcsRunnable() {
      public void run() throws VcsException {

        try {
          WorkspaceInfo workspace = myConflict2workspace.get(conflict);
          // names
          FilePath sourceLocalPath = workspace.findLocalPathByServerPath(conflict.getYsitemsrc());
          data.sourceLocalName = sourceLocalPath != null ? sourceLocalPath.getPath() : null;
          FilePath targetLocalPath = workspace.findLocalPathByServerPath(conflict.getYsitem());
          data.targetLocalName = targetLocalPath != null ? targetLocalPath.getPath() : null;
          data.vFile = VcsUtil.getVirtualFile(conflict.getSrclitem());

          // content
          String original = new TFSContentRevision(workspace, conflict.getYitemid(), conflict.getYver()).getContent();
          data.baseContent = original != null ? original : "";
          String current = CurrentContentRevision.create(VcsUtil.getFilePath(data.vFile.getPath())).getContent();
          data.localContent = current != null ? current : "";
          String last = new TFSContentRevision(workspace, conflict.getYitemid(), conflict.getTver()).getContent();
          data.serverContent = last != null ? last : "";
        }
        catch (TfsException e) {
          throw new VcsException("Unable to get content for item " + data.sourceLocalName);
        }
      }
    };
    VcsUtil.runVcsProcessWithProgress(runnable, "Prepare merge data...", false, myProject);

    return data;
  }

  public ResolveConflictsForm(final Map<Conflict, WorkspaceInfo> conflict2workspace, Project project) throws VcsException {
    myProject = project;
    myConflict2workspace = conflict2workspace;
    myUnresolvedConflicts.addAll(conflict2workspace.keySet());
    for (Conflict conflict : myUnresolvedConflicts) {
      ResolutionType nameResolutionType =
        ChangeType.fromString(conflict.getYchg()).contains(ChangeType.Value.Rename) ? ResolutionType.IGNORED : ResolutionType.NO_CONFLICT;
      ResolutionType contentResolutionType =
        ChangeType.fromString(conflict.getYchg()).contains(ChangeType.Value.Edit) ? ResolutionType.IGNORED : ResolutionType.NO_CONFLICT;
      myMergeResult.put(conflict, new ResolutionData(nameResolutionType, contentResolutionType, conflict.getSrclitem()));
      final ConflictData conflictData = getConflictData(conflict);
      myConflict2ConflictData.put(conflict, conflictData);
    }

    myItemsTableModel = new ItemsTableModel();
    myItemsTable.setModel(myItemsTableModel);
    myItemsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    myItemsTableModel.setItems(myUnresolvedConflicts);

    myItemsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(final ListSelectionEvent se) {
        int[] selectedIndices = myItemsTable.getSelectedRows();
        enableButtons(selectedIndices);
      }
    });
    
    myAcceptYoursButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        int[] selectedIndices = myItemsTable.getSelectedRows();
        for (int index : selectedIndices) {
          Conflict conflict = myUnresolvedConflicts.get(index);
          ResolutionData resolutionData = myMergeResult.get(conflict);
          resolutionData.contentResolutionType = ResolutionType.ACCEPT_YOURS;
          resolutionData.nameResolutionType = ResolutionType.ACCEPT_YOURS;
          resolutionData.localName = conflict.getSrclitem();
          conflictResolved(conflict);
          myUnresolvedConflicts.remove(conflict);
          myItemsTableModel.fireTableDataChanged();
        }
      }
    });
    myAcceptTheirsButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent ae) {
        int[] selectedIndices = myItemsTable.getSelectedRows();
        try {
          for (int index : selectedIndices) {
            Conflict conflict = myUnresolvedConflicts.get(index);
            ResolutionData resolutionData = myMergeResult.get(conflict);
            resolutionData.contentResolutionType = ResolutionType.ACCEPT_THEIRS;
            resolutionData.nameResolutionType = ResolutionType.ACCEPT_THEIRS;

            WorkspaceInfo workspace = myConflict2workspace.get(conflict);
            FilePath localPath = workspace.findLocalPathByServerPath(conflict.getYsitemsrc());
            File localFile;
            if (localPath != null) {
              resolutionData.localName = localPath.getPath();
              localFile = localPath.getIOFile();
              // delete or rename localFile if needed
              if (conflict.getTsitem() == null) {
                // delete localFile
                resolutionData.localName = null;
                localFile.delete();
              }
              else {
                if (!conflict.getTsitem().equals(conflict.getYsitemsrc())) {
                  FilePath newLocalPath = workspace.findLocalPathByServerPath(conflict.getTsitem());
                  if (newLocalPath != null) {
                    resolutionData.localName = newLocalPath.getPath();
                    File newLocalFile = newLocalPath.getIOFile();
                    localFile.renameTo(newLocalFile);
                    localFile = newLocalFile;
                  }
                  else {
                    // TODO: Is it possible?
                    TFSVcs.error("Update: mapping not found for " + conflict.getTsitem());
                  }
                }
                if (conflict.getTsitem() != null) {
                  // set content to server content
                  ConflictData conflictData = myConflict2ConflictData.get(conflict);
                  BufferedWriter out = new BufferedWriter(new FileWriter(localFile));
                  out.write(conflictData.serverContent);
                  out.close();
                }
                localFile.setReadOnly();
              }
            }
            else {
              // TODO: Is it possible?
              TFSVcs.error("Update: mapping not found for " + conflict.getYsitem());
            }
            conflictResolved(conflict);
            myUnresolvedConflicts.remove(conflict);
            myItemsTableModel.fireTableDataChanged();
          }
        }
        catch (TfsException e) {
          // TODO: show error message dialog
        }
        catch (IOException e) {
          // TODO: show error message dialog
        }
      }
    });
    myMergeButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent ae) {
        int[] selectedIndices = myItemsTable.getSelectedRows();
        try {
          for (int index : selectedIndices) {
            Conflict conflict = myUnresolvedConflicts.get(index);
            WorkspaceInfo workspace = myConflict2workspace.get(conflict);
            ResolutionData resolutionData = myMergeResult.get(conflict);
            ConflictData conflictData = myConflict2ConflictData.get(conflict);

            // merge names if needed
            if (ChangeType.fromString(conflict.getYchg()).contains(ChangeType.Value.Rename)) {
              MergeNameDialog mergeNameDialog = new MergeNameDialog(conflict.getYsitem(), conflict.getTsitem());
              mergeNameDialog.show();
              if (mergeNameDialog.isOK()) {
                resolutionData.nameResolutionType = ResolutionType.MERGED;
                FilePath newLocalPath = workspace.findLocalPathByServerPath(mergeNameDialog.getSelectedName());
                if (newLocalPath != null) {
                  resolutionData.localName = newLocalPath.getPath();
                }
              }
            }
            // rename file on disk if needed
            File localFile = new File(conflictData.sourceLocalName);
            if (!conflictData.sourceLocalName.equals(resolutionData.localName)) {
              File newLocalFile = new File(resolutionData.localName);
              localFile.renameTo(newLocalFile);
            }
            // if content conflict present show merge names dialog
            final VirtualFile vFile = VcsUtil.getVirtualFile(localFile);
            MergeRequest request =
              PeerFactory.getInstance().getDiffRequestFactory().createMergeRequest(conflictData.serverContent.replaceAll("\r", ""),
                                                                                   conflictData.localContent.replaceAll("\r", ""),
                                                                                   conflictData.baseContent.replaceAll("\r", ""),
                                                                                   vFile,
                                                                                   myProject,
                                                                                   ActionButtonPresentation.createApplyButton());
            request.setWindowTitle("Title");
            request.setVersionTitles(new String[] {"1", "2", "3"} );
            DiffManager.getInstance().getDiffTool().show(request);
            resolutionData.contentResolutionType = ResolutionType.MERGED;
            conflictResolved(conflict);
          }
        }
        catch (TfsException e) {
          //noinspection ThrowableInstanceNeverThrown
          AbstractVcsHelper.getInstance(myProject).showError(new VcsException(e.getMessage(), e), TFSVcs.TFS_NAME);
        }
      }
    });
  }

  public boolean isBinary(final Conflict conflict) throws VcsException {
    // Binary files has encoding = -1
    boolean isBinary;
    try {
      WorkspaceInfo workspace = myConflict2workspace.get(conflict);
      String serverPath = conflict.getYsitem(); // TODO: is it always Ysitem? 
      Item item = workspace.getServer().getVCS().queryItem(workspace.getName(), workspace.getOwnerName(), serverPath,
                                                           new WorkspaceVersionSpec(workspace.getName(), workspace.getOwnerName()),
                                                           DeletedState.NonDeleted, false);
      isBinary = (item != null && item.getEnc() == -1);
      return isBinary;
    }
    catch (TfsException e) {
      //noinspection ThrowableInstanceNeverThrown
      AbstractVcsHelper.getInstance(myProject).showError(new VcsException("File type detection failed.", e), TFSVcs.TFS_NAME);
    }
    return false;
  }

  private void conflictResolved(final Conflict conflict) {
    // send "conflict resolved" to server
    try {
      WorkspaceInfo workspace = myConflict2workspace.get(conflict);
      ResolutionData resolutionData = myMergeResult.get(conflict);
      Resolution resolution = Resolution.AcceptMerge;
      if (resolutionData.contentResolutionType == ResolutionType.ACCEPT_YOURS &&
          resolutionData.nameResolutionType == ResolutionType.ACCEPT_YOURS) {
        resolution = Resolution.AcceptYours;
      }
      if (resolutionData.contentResolutionType == ResolutionType.ACCEPT_THEIRS &&
          resolutionData.nameResolutionType == ResolutionType.ACCEPT_THEIRS) {
        resolution = Resolution.AcceptTheirs;
      }
      final String newLocalPath = resolutionData.localName;
      VersionControlServer.ResolveConflictParams resolveConflictParams = new VersionControlServer.ResolveConflictParams(
        conflict.getCid(), resolution, LockLevel.Unchanged, conflict.getYenc(),
        newLocalPath);

      ResolveResponse response =
        workspace.getServer().getVCS().resolveConflict(workspace.getName(), workspace.getOwnerName(), resolveConflictParams);
      // process response
      GetOperation getOperation = null;
      if (response.getResolveResult().getGetOperation() != null) {
        getOperation = response.getResolveResult().getGetOperation()[0];
      }
      else if (response.getUndoOperations().getGetOperation() != null) {
        getOperation = response.getUndoOperations().getGetOperation()[0];
      }
      if (getOperation != null) {
        workspace.getServer().getVCS()
          .updateLocalVersions(workspace.getName(), workspace.getOwnerName(), Collections.singletonList(getOperation));
      }
    }
    catch (TfsException e) {
        //noinspection ThrowableInstanceNeverThrown
        AbstractVcsHelper.getInstance(myProject).showError(new VcsException("Conflict resolution failed.", e), TFSVcs.TFS_NAME);
    }
  }

  private void enableButtons(final int[] selectedIndices) {
    myAcceptYoursButton.setEnabled(selectedIndices.length > 0);
    myAcceptTheirsButton.setEnabled(selectedIndices.length > 0);
    myMergeButton.setEnabled(selectedIndices.length > 0);
    for (int index : selectedIndices) {
      Conflict conflict = myUnresolvedConflicts.get(index);
      if (conflict.getTsitem() == null) {
        // item deleted on server, so it is
        myMergeButton.setEnabled(false);
      }
      // TODO: disable myMergeButton if names do not conflict and content conflicts and is binary   
    }
  }

  Map<Conflict, ResolutionData> getMergeResult() {
    return myMergeResult;
  }

  private class ItemsTableModel extends AbstractTableModel {
    private List<Conflict> myConflicts;

    public void setItems(List<Conflict> conflicts) {
      myConflicts = conflicts;
      fireTableDataChanged();
    }

    public List<Conflict> getMergeData() {
      return myConflicts;
    }

    public String getColumnName(final int column) {
      return Column.values()[column].getCaption();
    }

    public int getRowCount() {
      return myConflicts != null ? myConflicts.size() : 0;
    }

    public int getColumnCount() {
      return Column.values().length;
    }

    public Object getValueAt(final int rowIndex, final int columnIndex) {
      Conflict conflict = myConflicts.get(rowIndex);
      return Column.values()[columnIndex].getValue(conflict, myMergeResult.get(conflict));
    }
  }

  private enum Column {

    Name("Name") {
      public String getValue(Conflict conflict, ResolutionData resolutionData) {
        return conflict.getSrclitem(); 
      }
    },
    ConflictType("Conflict type") {
      public String getValue(Conflict conflict, ResolutionData resolutionData) {
        ArrayList<String> types = new ArrayList<String>();
        if (resolutionData.nameResolutionType == ResolutionType.IGNORED) {
          types.add("Rename");
        }
        if (resolutionData.contentResolutionType == ResolutionType.IGNORED) {
          types.add("Content");
        }
        String res = null;
        for (String type : types) {
          if (res == null) {
            res = type;
          }
          else {
            res += (", " + type);
          }
        }
        return res;
      }
    };

    private String myCaption;

    Column(String caption) {
      myCaption = caption;
    }

    public String getCaption() {
      return myCaption;
    }

    public abstract String getValue(Conflict conflict, ResolutionData resolutionData);

  }
}

