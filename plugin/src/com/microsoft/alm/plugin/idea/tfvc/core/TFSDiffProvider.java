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
import com.intellij.openapi.vcs.diff.DiffProviderEx;
import com.intellij.openapi.vcs.diff.ItemLatestState;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.rest.VersionControlRecursionTypeCaseSensitive;
import com.microsoft.alm.plugin.external.models.ItemInfo;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.common.services.LocalizationServiceImpl;
import com.microsoft.alm.plugin.idea.tfvc.core.revision.TFSContentRevision;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsRevisionNumber;
import com.microsoft.alm.sourcecontrol.webapi.model.TfvcItem;
import com.microsoft.alm.sourcecontrol.webapi.model.TfvcVersionDescriptor;
import com.microsoft.alm.sourcecontrol.webapi.model.TfvcVersionType;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TFSDiffProvider extends DiffProviderEx {
    private static final Logger logger = LoggerFactory.getLogger(TFSDiffProvider.class);
    private static final int MINIMAL_WAIT_FOR_RETRY = 60 * 1000;

    private final Project project;
    private List<Workspace.Mapping> mappings;
    private Calendar lastUpdated;

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
                return TFSContentRevision.create(project, path, getChangeset(vcsRevisionNumber), getModificationDate(vcsRevisionNumber));
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
            // need to make a file because the VirtualFile object path is in system-independent format
            final File localFile = VfsUtilCore.virtualToIoFile(virtualFile);
            final String filePath = localFile.getPath();
            final ServerContext context = TFSVcs.getInstance(project).getServerContext(true);
            final ItemInfo itemInfo = CommandUtils.getItemInfo(context, filePath);
            return createRevision(itemInfo, filePath);
        } catch (Exception e) {
            logger.warn("Unable to getCurrentRevision", e);
            AbstractVcsHelper.getInstance(project).showError(
                    new VcsException(LocalizationServiceImpl.getInstance().getExceptionMessage(e), e), TFSVcs.TFVC_NAME);
        }
        return VcsRevisionNumber.NULL;
    }

    @Override
    public Map<VirtualFile, VcsRevisionNumber> getCurrentRevisions(Iterable<VirtualFile> files) {
        final ServerContext context = TFSVcs.getInstance(project).getServerContext(true);
        final List<String> filePaths = ContainerUtil.newArrayList();
        for (VirtualFile file : files) {
            String filePath = file.getPath();
            filePaths.add(filePath);
        }

        TfvcClient client = TfvcClient.getInstance();
        final LocalFileSystem fs = LocalFileSystem.getInstance();
        final Map<VirtualFile, VcsRevisionNumber> revisionMap = ContainerUtil.newHashMap();
        client.getLocalItemsInfo(project, context, filePaths, info -> {
            final String itemPath = info.getLocalItem();
            final VirtualFile virtualFile = fs.findFileByPath(itemPath);
            if (virtualFile == null) {
                logger.error("VirtualFile not found for item " + itemPath);
                return;
            }

            revisionMap.put(virtualFile, createRevision(info, itemPath));
        });

        return revisionMap;
    }

    public ItemLatestState getLastRevision(final FilePath localPath) {
        try {
            // need to make a file because the FilePath object path is in system-independent format
            final File localFile = localPath.getIOFile();
            final VcsRevisionNumber revisionNumber = getRevisionNumber(localFile.getPath(), localFile.getName());
            if (revisionNumber != VcsRevisionNumber.NULL) {
                return new ItemLatestState(revisionNumber, true, false);
            }
        } catch (final Exception e) {
            logger.warn("Unable to getLastRevision", e);
            AbstractVcsHelper.getInstance(project).showError(
                    new VcsException(LocalizationServiceImpl.getInstance().getExceptionMessage(e), e), TFSVcs.TFVC_NAME);
        }
        return new ItemLatestState(VcsRevisionNumber.NULL, false, false);
    }

    public VcsRevisionNumber getLatestCommittedRevision(final VirtualFile vcsRoot) {
        // todo.
        return null;
    }

    private TfsRevisionNumber createRevision(final ItemInfo itemInfo, final String itemPath) {
        return new TfsRevisionNumber(
                itemInfo.getLocalVersionAsInt(),
                itemPath,
                null);
    }

    /**
     * Creates a revision number for the given file
     *
     * @param filePath
     * @param fileName
     * @return
     */
    private VcsRevisionNumber getRevisionNumber(final String filePath, final String fileName) {
        final TfvcVersionDescriptor versionDescriptor = new TfvcVersionDescriptor();
        versionDescriptor.setVersionType(TfvcVersionType.LATEST);
        final ServerContext context = TFSVcs.getInstance(project).getServerContext(true);
        final List<TfvcItem> item = context.getTfvcHttpClient().getItems(context.getTeamProjectReference().getId(),
                TfsFileUtil.translateLocalItemToServerItem(filePath, getUpdatedMappings()),
                VersionControlRecursionTypeCaseSensitive.NONE, versionDescriptor);

        if (!item.isEmpty() && item.get(0) != null) {
            return new TfsRevisionNumber(item.get(0).getChangesetVersion(), fileName, item.get(0).getChangeDate().toString());
        }
        return VcsRevisionNumber.NULL;
    }

    /**
     * Gets the mappings from the current workspaces based on the last minute. We want to cache this information
     * because sometimes revision numbers are retrieved for all files in a repo at once and if we resolve the workspace
     * mappings every time the performance is horrible
     * <p>
     * The mappings will update if more than a minute is passed to make sure the mapping is up-to-date
     *
     * @return
     */
    private List<Workspace.Mapping> getUpdatedMappings() {
        if (mappings == null || lastUpdated == null || (Calendar.getInstance().getTimeInMillis() - lastUpdated.getTimeInMillis() > MINIMAL_WAIT_FOR_RETRY)) {
            updateMappings();
        }
        return mappings;
    }

    /**
     * Updates the cached mappings
     * <p>
     * TODO: call this from places where we know a mapping change has occurred
     */
    public void updateMappings() {
        final Workspace workspace = Objects.requireNonNull(TfvcWorkspaceLocator.getPartialWorkspace(project, false));
        mappings = workspace.getMappings();
        lastUpdated = Calendar.getInstance();
    }
}