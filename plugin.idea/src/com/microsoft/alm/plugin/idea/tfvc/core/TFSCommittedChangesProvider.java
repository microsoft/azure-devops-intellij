// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

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

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.CachingCommittedChangesProvider;
import com.intellij.openapi.vcs.ChangeListColumn;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.RepositoryLocation;
import com.intellij.openapi.vcs.VcsException;
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
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.models.ChangeSet;
import com.microsoft.alm.plugin.external.models.VersionSpec;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsRevisionNumber;
import com.microsoft.alm.plugin.idea.tfvc.ui.TFSVersionFilterComponent;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TFSCommittedChangesProvider implements CachingCommittedChangesProvider<TFSChangeList, ChangeBrowserSettings> {
    public static final Logger logger = LoggerFactory.getLogger(TFSCommittedChangesProvider.class);

    private final Project project;
    private final TFSVcs vcs;

    public TFSCommittedChangesProvider(final Project project) {
        this.project = project;
        this.vcs = TFSVcs.getInstance(this.project);
    }

    @NotNull
    public ChangeBrowserSettings createDefaultSettings() {
        return new ChangeBrowserSettings();
    }

    public ChangesBrowserSettingsEditor<ChangeBrowserSettings> createFilterUI(final boolean showDateFilter) {
        return new TFSVersionFilterComponent(showDateFilter);
    }

    @Nullable
    public VcsCommittedListsZipper getZipper() {
        return null;
    }

    @Nullable
    public RepositoryLocation getLocationFor(final FilePath root, final String repositoryPath) {
        return getLocationFor(root);
    }

    public RepositoryLocation getLocationFor(final FilePath root) {
        final Workspace workspace = CommandUtils.getPartialWorkspace(project);
        return new TFSRepositoryLocation(workspace, root.getVirtualFile());
    }

    @Override
    public Pair<TFSChangeList, FilePath> getOneList(final VirtualFile file, final VcsRevisionNumber number) throws VcsException {
        final ChangeBrowserSettings settings = createDefaultSettings();
        settings.USE_CHANGE_AFTER_FILTER = true;
        settings.USE_CHANGE_BEFORE_FILTER = true;
        settings.CHANGE_BEFORE = settings.CHANGE_AFTER = String.valueOf(((TfsRevisionNumber) number).getValue());
        final FilePath filePath = VcsContextFactory.SERVICE.getInstance().createFilePathOn(file);
        final List<TFSChangeList> list = getCommittedChanges(settings, getLocationFor(filePath), 1);
        if (list.size() == 1) {
            return Pair.create(list.get(0), filePath);
        }
        return null;
    }

    @Override
    public RepositoryLocation getForNonLocal(VirtualFile file) {
        return null;
    }

    @Override
    public boolean supportsIncomingChanges() {
        return true;
    }

    public void loadCommittedChanges(final ChangeBrowserSettings settings,
                                     final RepositoryLocation location,
                                     final int maxCount,
                                     final AsynchConsumer<CommittedChangeList> consumer) throws VcsException {
        // TODO: (Jetbrains) if revision and date filters are both set, which one should have priority?
        VersionSpec versionFrom = VersionSpec.create(1);
        if (settings.getChangeAfterFilter() != null) {
            versionFrom = VersionSpec.create((int) settings.getChangeAfterFilter().longValue());
        }
        if (settings.getDateAfterFilter() != null) {
            versionFrom = VersionSpec.create(settings.getDateAfterFilter());
        }

        VersionSpec versionTo = VersionSpec.LATEST;
        if (settings.getChangeBeforeFilter() != null) {
            versionTo = VersionSpec.create((int) settings.getChangeBeforeFilter().longValue());
        }
        if (settings.getDateBeforeFilter() != null) {
            versionTo = VersionSpec.create(settings.getDateBeforeFilter());
        }
        final VersionSpec.Range range = new VersionSpec.Range(versionFrom, versionTo);

        logger.info(String.format("Loading committed changes for range %s to %s", versionFrom.getValue(), versionTo.getValue()));
        final TFSRepositoryLocation tfsRepositoryLocation = (TFSRepositoryLocation) location;
        final ServerContext context = TFSVcs.getInstance(project).getServerContext(false);
        final List<ChangeSet> changeSets = CommandUtils.getHistoryCommand(context, tfsRepositoryLocation.getRoot().getPath(),
                range.toString(), maxCount, true, settings.getUserFilter() == null ? StringUtils.EMPTY : settings.getUserFilter());
        final TFSChangeListBuilder tfsChangeListBuilder = new TFSChangeListBuilder(vcs, tfsRepositoryLocation.getWorkspace());

        // list is in order of newest to oldest so we can assume the next checkin in the list is the actual previous checkin in time
        for (int i = 0; i < changeSets.size() - 1; i++) {
            consumer.consume(tfsChangeListBuilder.createChangeList(changeSets.get(i), changeSets.get(i + 1).getIdAsInt(), changeSets.get(i + 1).getDate()));
        }
        // this is the first checkin to the repo so there is no previous checkin to refer to
        consumer.consume(tfsChangeListBuilder.createChangeList(changeSets.get(changeSets.size() - 1), 0, StringUtils.EMPTY));
        consumer.finished();
    }

    public List<TFSChangeList> getCommittedChanges(final ChangeBrowserSettings settings,
                                                   final RepositoryLocation location,
                                                   final int maxCount) throws VcsException {
        final List<TFSChangeList> result = new ArrayList<TFSChangeList>();
        loadCommittedChanges(settings, location, maxCount, new AsynchConsumer<CommittedChangeList>() {
            public void finished() {
            }

            public void consume(final CommittedChangeList committedChangeList) {
                result.add((TFSChangeList) committedChangeList);
            }
        });
        return result;
    }

    public ChangeListColumn[] getColumns() {
        return new ChangeListColumn[]{new ChangeListColumn.ChangeListNumberColumn(
                TfPluginBundle.message(TfPluginBundle.KEY_TFVC_REPOSITORY_VIEW_COLUMN_REVISION)), ChangeListColumn.NAME,
                ChangeListColumn.DATE, ChangeListColumn.DESCRIPTION};
    }

    public int getFormatVersion() {
        return 1;
    }

    public void writeChangeList(final DataOutput stream, final TFSChangeList list) throws IOException {
        list.writeToStream(stream);
    }

    public TFSChangeList readChangeList(final RepositoryLocation location, final DataInput stream) throws IOException {
        return new TFSChangeList(vcs, stream);
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
        return TfPluginBundle.message(TfPluginBundle.KEY_TFVC_REPOSITORY_VIEW_CHANGELIST_TITLE);
    }

    public boolean isChangeLocallyAvailable(final FilePath filePath,
                                            @Nullable final VcsRevisionNumber localRevision,
                                            final VcsRevisionNumber changeRevision,
                                            final TFSChangeList changeList) {
        return localRevision != null && localRevision.compareTo(changeRevision) >= 0;
    }

    public boolean refreshIncomingWithCommitted() {
        // TODO (Jetbrains)
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