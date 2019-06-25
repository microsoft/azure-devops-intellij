// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsConfiguration;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.DiffFromHistoryHandler;
import com.intellij.openapi.vcs.history.HistoryAsTreeProvider;
import com.intellij.openapi.vcs.history.VcsAbstractHistorySession;
import com.intellij.openapi.vcs.history.VcsAppendableHistorySessionPartner;
import com.intellij.openapi.vcs.history.VcsDependentHistoryComponents;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsHistoryProvider;
import com.intellij.openapi.vcs.history.VcsHistorySession;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.ColumnInfo;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.models.ChangeSet;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.tfvc.core.revision.TfsFileRevision;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.List;

public class TFSHistoryProvider implements VcsHistoryProvider {
    public static final Logger logger = LoggerFactory.getLogger(TFSHistoryProvider.class);

    private final Project project;

    public TFSHistoryProvider(@NotNull final Project project) {
        this.project = project;
    }

    public VcsDependentHistoryComponents getUICustomization(final VcsHistorySession session, final JComponent forShortcutRegistration) {
        return VcsDependentHistoryComponents.createOnlyColumns(ColumnInfo.EMPTY_ARRAY);
    }

    public AnAction[] getAdditionalActions(final Runnable refresher) {
        return new AnAction[0];
    }

    public boolean isDateOmittable() {
        return false;
    }

    @Nullable
    @NonNls
    public String getHelpId() {
        return null;
    }

    @Nullable
    public VcsHistorySession createSessionFor(final FilePath filePath) throws VcsException {
        try {
            final List<TfsFileRevision> revisions =
                    getRevisions(project, TFSVcs.getInstance(project).getServerContext(true), filePath, filePath.isDirectory());
            if (revisions.isEmpty()) {
                return null;
            }

            return createSession(revisions.get(0).getRevisionNumber(), revisions, !filePath.isDirectory());
        } catch (Exception e) {
            throw new VcsException(e);
        }
    }

    private static VcsAbstractHistorySession createSession(final VcsRevisionNumber currentRevisionNumber,
                                                           final List<TfsFileRevision> revisions,
                                                           final boolean isFile) {
        return new VcsAbstractHistorySession(revisions) {
            public VcsRevisionNumber calcCurrentRevisionNumber() {
                return currentRevisionNumber;
            }

            public HistoryAsTreeProvider getHistoryAsTreeProvider() {
                return null;
            }

            @Override
            public VcsHistorySession copy() {
                return createSession(currentRevisionNumber, revisions, isFile);
            }

            @Override
            public boolean isContentAvailable(final VcsFileRevision revision) {
                return isFile;
            }
        };
    }

    public void reportAppendableHistory(final FilePath path, final VcsAppendableHistorySessionPartner partner) throws VcsException {
        final VcsHistorySession session = createSessionFor(path);
        partner.reportCreatedEmptySession((VcsAbstractHistorySession) session);
    }

    public static List<TfsFileRevision> getRevisions(final Project project,
                                                     final ServerContext serverContext,
                                                     final FilePath localPath,
                                                     final boolean isDirectory) {
        final VcsConfiguration vcsConfiguration = VcsConfiguration.getInstance(project);
        final int maxCount = vcsConfiguration.LIMIT_HISTORY ? vcsConfiguration.MAXIMUM_HISTORY_ROWS : Integer.MAX_VALUE;
        final List<ChangeSet> changesets = CommandUtils.getHistoryCommand(serverContext, localPath.getPath(), null, maxCount, isDirectory, null, false);

        final List<TfsFileRevision> revisions = new ArrayList<TfsFileRevision>(changesets.size());
        for (final ChangeSet changeSet : changesets) {
            revisions.add(new TfsFileRevision(project, localPath, changeSet.getIdAsInt(),
                    changeSet.getCommitter(), changeSet.getComment(), changeSet.getDate()));
        }

        return revisions;
    }

    public boolean supportsHistoryForDirectories() {
        return true;
    }

    @Override
    public DiffFromHistoryHandler getHistoryDiffHandler() {
        return null;
    }

    @Override
    public boolean canShowHistoryFor(@NotNull final VirtualFile file) {
        return true;
    }

}
