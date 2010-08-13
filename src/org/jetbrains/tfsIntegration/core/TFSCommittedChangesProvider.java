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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.*;
import com.intellij.openapi.vcs.actions.VcsContextFactory;
import com.intellij.openapi.vcs.changes.committed.DecoratorManager;
import com.intellij.openapi.vcs.changes.committed.VcsCommittedListsZipper;
import com.intellij.openapi.vcs.changes.committed.VcsCommittedViewAuxiliary;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vcs.versionBrowser.ChangeBrowserSettings;
import com.intellij.openapi.vcs.versionBrowser.ChangesBrowserSettingsEditor;
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.AsynchConsumer;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.*;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.core.tfs.version.ChangesetVersionSpec;
import org.jetbrains.tfsIntegration.core.tfs.version.DateVersionSpec;
import org.jetbrains.tfsIntegration.core.tfs.version.LatestVersionSpec;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.ui.TFSVersionFilterComponent;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;

public class TFSCommittedChangesProvider implements CachingCommittedChangesProvider<TFSChangeList, ChangeBrowserSettings> {
  private final Project myProject;
  private final TFSVcs myVcs;

  public TFSCommittedChangesProvider(TFSVcs vcs, final Project project) {
    myProject = project;
    myVcs = vcs;
  }

  public TFSCommittedChangesProvider(final Project project) {
    myProject = project;
    myVcs = TFSVcs.getInstance(myProject);
  }

  public ChangeBrowserSettings createDefaultSettings() {
    return new ChangeBrowserSettings();
  }

  public ChangesBrowserSettingsEditor<ChangeBrowserSettings> createFilterUI(final boolean showDateFilter) {
    return new TFSVersionFilterComponent(showDateFilter);
  }

  public VcsCommittedListsZipper getZipper() {
    return null;
  }

  @Nullable
  public RepositoryLocation getLocationFor(final FilePath root, final String repositoryPath) {
    return getLocationFor(root);
  }

  public RepositoryLocation getLocationFor(final FilePath root) {
    final Map<WorkspaceInfo, List<FilePath>> pathsByWorkspaces = new HashMap<WorkspaceInfo, List<FilePath>>();
    try {
      WorkstationHelper.processByWorkspaces(Collections.singletonList(root), true, new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          pathsByWorkspaces.put(workspace, TfsUtil.getLocalPaths(paths));
        }
      });
      if (!pathsByWorkspaces.isEmpty()) {
        return new TFSRepositoryLocation(pathsByWorkspaces);
      }
    }
    catch (TfsException e) {
      AbstractVcsHelper.getInstance(myProject).showError(new VcsException(e.getMessage(), e), TFSVcs.TFS_NAME);
    }
    return null;
  }

  @Override
  public Pair<TFSChangeList, FilePath> getOneList(VirtualFile file, VcsRevisionNumber number) throws VcsException {
    final ChangeBrowserSettings settings = createDefaultSettings();
    settings.USE_CHANGE_AFTER_FILTER = true;
    settings.USE_CHANGE_BEFORE_FILTER = true;
    settings.CHANGE_AFTER = number.asString();
    settings.CHANGE_BEFORE = number.asString();
    final FilePath filePath = VcsContextFactory.SERVICE.getInstance().createFilePathOn(file);
    final List<TFSChangeList> list = getCommittedChanges(settings, getLocationFor(filePath), 1);
    if (list.size() == 1) {
      // todo - implement in proper way!
      return new Pair<TFSChangeList, FilePath>(list.get(0), filePath);
    }
    return null;
  }

  public void loadCommittedChanges(ChangeBrowserSettings settings,
                                   RepositoryLocation location,
                                   int maxCount,
                                   AsynchConsumer<CommittedChangeList> consumer)
    throws VcsException {
    // TODO: deletion id
    // TODO: if revision and date filters are both set, which one should have priority?
    VersionSpec versionFrom = new ChangesetVersionSpec(1);
    if (settings.getChangeAfterFilter() != null) {
      versionFrom = new ChangesetVersionSpec((int)settings.getChangeAfterFilter().longValue());
    }
    if (settings.getDateAfterFilter() != null) {
      versionFrom = new DateVersionSpec(settings.getDateAfterFilter());
    }

    VersionSpec versionTo = LatestVersionSpec.INSTANCE;
    if (settings.getChangeBeforeFilter() != null) {
      versionTo = new ChangesetVersionSpec((int)settings.getChangeBeforeFilter().longValue());
    }
    if (settings.getDateBeforeFilter() != null) {
      versionTo = new DateVersionSpec(settings.getDateBeforeFilter());
    }

    TFSRepositoryLocation tfsRepositoryLocation = (TFSRepositoryLocation)location;

    try {
      for (Map.Entry<WorkspaceInfo, List<FilePath>> entry : tfsRepositoryLocation.getPathsByWorkspaces().entrySet()) {
        WorkspaceInfo workspace = entry.getKey();
        final Map<FilePath, ExtendedItem> extendedItems =
          workspace.getExtendedItems(entry.getValue(), myProject, TFSBundle.message("loading.items"));
        for (Map.Entry<FilePath, ExtendedItem> localPath2ExtendedItem : extendedItems.entrySet()) {
          ExtendedItem extendedItem = localPath2ExtendedItem.getValue();
          if (extendedItem == null) {
            continue;
          }
          int itemLatestVersion = getLatestChangesetId(workspace, settings.getUserFilter(), extendedItem);

          if (versionFrom instanceof ChangesetVersionSpec) {
            ChangesetVersionSpec changesetVersionFrom = (ChangesetVersionSpec)versionFrom;
            if (changesetVersionFrom.getChangeSetId() > itemLatestVersion) {
              continue;
            }
          }

          if (versionTo instanceof ChangesetVersionSpec) {
            ChangesetVersionSpec changesetVersionTo = (ChangesetVersionSpec)versionTo;
            if (changesetVersionTo.getChangeSetId() > itemLatestVersion) {
              versionTo = new ChangesetVersionSpec(itemLatestVersion);
            }
          }

          final VersionSpec itemVersion = LatestVersionSpec.INSTANCE;
          final RecursionType recursionType = localPath2ExtendedItem.getKey().isDirectory() ? RecursionType.Full : null;
          ItemSpec itemSpec = VersionControlServer.createItemSpec(extendedItem.getSitem(), recursionType);

          List<Changeset> changeSets = workspace.getServer().getVCS()
            .queryHistory(workspace.getName(), workspace.getOwnerName(), itemSpec, settings.getUserFilter(), itemVersion, versionFrom,
                          versionTo, maxCount, myProject, TFSBundle.message("loading.history"));
          for (Changeset changeset : changeSets) {
            final TFSChangeList newList = new TFSChangeList(workspace, changeset.getCset(), changeset.getOwner(),
                                                            changeset.getDate().getTime(), changeset.getComment(), myVcs);
            consumer.consume(newList);
          }

        }
      }
    }
    catch (TfsException e) {
      throw new VcsException(e);
    }
    finally {
      consumer.finished();
    }
  }

  public List<TFSChangeList> getCommittedChanges(final ChangeBrowserSettings settings,
                                                 final RepositoryLocation location,
                                                 final int maxCount) throws VcsException {
    final List<TFSChangeList> result = new ArrayList<TFSChangeList>();
    loadCommittedChanges(settings, location, maxCount, new AsynchConsumer<CommittedChangeList>() {
      public void finished() {
      }

      public void consume(CommittedChangeList committedChangeList) {
        result.add((TFSChangeList)committedChangeList);
      }
    });
    return result;
  }

  private int getLatestChangesetId(final WorkspaceInfo workspace, String user, final ExtendedItem extendedItem) throws TfsException {
    if (extendedItem.getType() == ItemType.File) {
      return extendedItem.getLatest();
    }
    final VersionSpec itemVersion = LatestVersionSpec.INSTANCE;
    final VersionSpec versionFrom = new ChangesetVersionSpec(1);
    final VersionSpec versionTo = LatestVersionSpec.INSTANCE;
    final int maxCount = 1;
    ItemSpec itemSpec = VersionControlServer.createItemSpec(extendedItem.getSitem(), RecursionType.Full);
    List<Changeset> changeSets = workspace.getServer().getVCS()
      .queryHistory(workspace.getName(), workspace.getOwnerName(), itemSpec, user, itemVersion, versionFrom, versionTo, maxCount, myProject,
                    TFSBundle.message("loading.history"));
    return changeSets.get(0).getCset();
  }

  public ChangeListColumn[] getColumns() {
    return new ChangeListColumn[]{new ChangeListColumn.ChangeListNumberColumn("Revision"), ChangeListColumn.NAME, ChangeListColumn.DATE,
      ChangeListColumn.DESCRIPTION};
  }

  public int getFormatVersion() {
    return 1;
  }

  public void writeChangeList(final DataOutput stream, final TFSChangeList list) throws IOException {
    list.writeToStream(stream);
  }

  public TFSChangeList readChangeList(final RepositoryLocation location, final DataInput stream) throws IOException {
    return new TFSChangeList(myVcs, stream);
  }

  public boolean isMaxCountSupported() {
    return true;
  }

  public Collection<FilePath> getIncomingFiles(final RepositoryLocation location) throws VcsException {
    return null;
  }

  public boolean refreshCacheByNumber() {
    return true;
  }

  public String getChangelistTitle() {
    return "Changelist";
  }

  public boolean isChangeLocallyAvailable(final FilePath filePath,
                                          @Nullable final VcsRevisionNumber localRevision,
                                          final VcsRevisionNumber changeRevision,
                                          final TFSChangeList changeList) {
    return localRevision != null && localRevision.compareTo(changeRevision) >= 0;
  }

  public boolean refreshIncomingWithCommitted() {
    // TODO
    return false;
  }

  public int getUnlimitedCountValue() {
    return 0;
  }

  @Nullable
  public VcsCommittedViewAuxiliary createActions(final DecoratorManager manager, final RepositoryLocation location) {
    return null;
  }
}
