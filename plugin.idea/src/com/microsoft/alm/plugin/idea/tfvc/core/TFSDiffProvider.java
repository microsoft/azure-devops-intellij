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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.diff.DiffProvider;
import com.intellij.openapi.vcs.diff.ItemLatestState;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.models.ItemInfo;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.tfvc.core.revision.TFSContentRevision;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsRevisionNumber;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TFSDiffProvider implements DiffProvider {
    private static final Logger logger = LoggerFactory.getLogger(TFSDiffProvider.class);

    private final Project project;

    public TFSDiffProvider(@NotNull final Project project) {
        this.project = project;
    }

    @Nullable
    public ItemLatestState getLastRevision(final VirtualFile virtualFile) {
        final FilePath localPath = TfsFileUtil.getFilePath(virtualFile);
        return getLastRevision(localPath);
    }

    @Nullable
    public ContentRevision createFileContent(final VcsRevisionNumber vcsRevisionNumber, final VirtualFile virtualFile) {
        if (VcsRevisionNumber.NULL.equals(vcsRevisionNumber)) {
            return null;
        } else {
            final FilePath path = TfsFileUtil.getFilePath(virtualFile);
            try {
                final ServerContext serverContext = TFSVcs.getInstance(project).getServerContext(true);
                return TFSContentRevision.create(project, serverContext, path, getChangeset(vcsRevisionNumber), getModificationDate(vcsRevisionNumber));
            } catch (Exception e) {
                logger.warn("Unable to create file content", e);
                AbstractVcsHelper.getInstance(project).showError(new VcsException(e), TFSVcs.TFVC_NAME);
                return null;
            }
        }
    }

    private String getModificationDate(final VcsRevisionNumber vcsRevisionNumber) {
        if (vcsRevisionNumber instanceof TfsRevisionNumber) {
            final TfsRevisionNumber revisionNumber = (TfsRevisionNumber) vcsRevisionNumber;
            return revisionNumber.getModificationDate();
        }
        return StringUtils.EMPTY;
    }

    private int getChangeset(final VcsRevisionNumber vcsRevisionNumber) {
        if (vcsRevisionNumber instanceof VcsRevisionNumber.Int) {
            final VcsRevisionNumber.Int revisionNumber = (VcsRevisionNumber.Int) vcsRevisionNumber;
            return revisionNumber.getValue();
        }
        return 0;
    }

    @Nullable
    public VcsRevisionNumber getCurrentRevision(final VirtualFile virtualFile) {
        try {
            // TODO: we may have an issue here with new files that don't exist on the server and have not been explicity added (NEED TO TEST)
            final ItemInfo info = CommandUtils.getItemInfo(TFSVcs.getInstance(project).getServerContext(true), virtualFile.getPath());
            if (info != null) {
                return new TfsRevisionNumber(info.getLocalVersionAsInt(), virtualFile.getName(), info.getLastModified());
            }
        } catch (Exception e) {
            AbstractVcsHelper.getInstance(project).showError(new VcsException(e.getMessage(), e), TFSVcs.TFVC_NAME);
        }
        return VcsRevisionNumber.NULL;
    }

    public ItemLatestState getLastRevision(final FilePath localPath) {
        try {
            // TODO: we may have an issue here with new files that don't exist on the server and have not been explicity added (NEED TO TEST)
            final ItemInfo info = CommandUtils.getItemInfo(TFSVcs.getInstance(project).getServerContext(true), localPath.getPath());
            if (info != null) {
                final VcsRevisionNumber.Int revisionNumber = new TfsRevisionNumber(info.getServerVersionAsInt(), localPath.getName(), info.getLastModified());
                return new ItemLatestState(revisionNumber, info.getServerVersionAsInt() == 0, false);
            }

            /*
            Collection<WorkspaceInfo> workspaces = Workstation.getInstance().findWorkspaces(localPath, false, project);
            if (workspaces.isEmpty()) {
                return new ItemLatestState(VcsRevisionNumber.NULL, false, false);
            }
            final WorkspaceInfo workspace = workspaces.iterator().next();
            final ExtendedItem extendedItem = workspace.getServer().getVCS()
                    .getExtendedItem(workspace.getName(), workspace.getOwnerName(), localPath, RecursionType.None, DeletedState.Any, project,
                            TFSBundle.message("loading.item"));
            if (extendedItem == null) {
                return new ItemLatestState(VcsRevisionNumber.NULL, false, false);
            }
            // there may be several extended items for a given name (see VersionControlServer.chooseExtendedItem())
            // so we need to query item by name
            final Item item = workspace.getServer().getVCS()
                    .queryItem(workspace.getName(), workspace.getOwnerName(), extendedItem.getSitem(), LatestVersionSpec.INSTANCE, DeletedState.Any,
                            false, project, TFSBundle.message("loading.item"));
            if (item != null) {
                VcsRevisionNumber.Int revisionNumber = new TfsRevisionNumber(item.getCs(), item.getItemid());
                return new ItemLatestState(revisionNumber, item.getDid() == Integer.MIN_VALUE, false);
            }
            else {
                return new ItemLatestState(VcsRevisionNumber.NULL, false, false);
            }
            */
            return new ItemLatestState(VcsRevisionNumber.NULL, false, false);
        } catch (final Exception e) {
            logger.warn("Unable to getLastRevision", e);
            AbstractVcsHelper.getInstance(project).showError(new VcsException(e.getMessage(), e), TFSVcs.TFVC_NAME);
            return new ItemLatestState(VcsRevisionNumber.NULL, false, false);
        }
    }

    public VcsRevisionNumber getLatestCommittedRevision(final VirtualFile vcsRoot) {
        // todo.
        return null;
    }
}
