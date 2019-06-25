// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.intellij.openapi.vcs.FilePath;
import com.microsoft.alm.plugin.external.models.ChangeSet;
import com.microsoft.alm.plugin.external.models.CheckedInChange;
import com.microsoft.alm.plugin.external.models.ServerStatusType;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.VersionControlPath;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates TFSChangeList objects given a changeset
 */
public class TFSChangeListBuilder {
    private static final Logger logger = LoggerFactory.getLogger(TFSChangeListBuilder.class);

    private final TFSVcs vcs;
    private final Workspace workspace;
    private final Map<String, FilePath> filePathCache;

    public TFSChangeListBuilder(final TFSVcs vcs, final Workspace workspace) {
        this.vcs = vcs;
        this.workspace = workspace;
        this.filePathCache = new HashMap<String, FilePath>();
    }

    public TFSChangeList createChangeList(final ChangeSet changeSet, final int previousChangeSetId, final String previousChangeSetDate) {
        final List<FilePath> addedFiles = new ArrayList<FilePath>(changeSet.getChanges().size());
        final List<FilePath> deletedFiles = new ArrayList<FilePath>(changeSet.getChanges().size());
        final List<FilePath> renamedFiles = new ArrayList<FilePath>(changeSet.getChanges().size());
        final List<FilePath> editedFiles = new ArrayList<FilePath>(changeSet.getChanges().size());

        for (final CheckedInChange pendingChange : changeSet.getChanges()) {
            final FilePath path;
            if (filePathCache.containsKey(pendingChange.getServerItem())) {
                path = filePathCache.get(pendingChange.getServerItem());
            } else {
                final String localPath = TfsFileUtil.translateServerItemToLocalItem(workspace.getMappings(), pendingChange.getServerItem());
                if (StringUtils.isEmpty(localPath)) {
                    logger.warn("Could not find a local path for file: " + pendingChange.getServerItem());
                    continue;
                }
                final File file = new File(localPath);
                path = VersionControlPath.getFilePath(file.getPath(), file.isDirectory());
                filePathCache.put(pendingChange.getServerItem(), path);
            }

            // figuring out what type of change it is
            if (pendingChange.getChangeTypes().contains(ServerStatusType.ADD)
                    || pendingChange.getChangeTypes().contains(ServerStatusType.UNDELETE)
                    || pendingChange.getChangeTypes().contains(ServerStatusType.BRANCH)) {
                addedFiles.add(path);
            } else if (pendingChange.getChangeTypes().contains(ServerStatusType.DELETE)) {
                deletedFiles.add(path);
            } else if (pendingChange.getChangeTypes().contains(ServerStatusType.RENAME)) {
                renamedFiles.add(path);
            } else if (pendingChange.getChangeTypes().contains(ServerStatusType.EDIT)
                    || pendingChange.getChangeTypes().contains(ServerStatusType.MERGE)) {
                editedFiles.add(path);
            }
        }

        return new TFSChangeList(addedFiles, deletedFiles, renamedFiles, editedFiles, changeSet.getIdAsInt(),
                changeSet.getCommitter(), changeSet.getComment(), changeSet.getDate(), previousChangeSetId,
                previousChangeSetDate, vcs, workspace.getName());
    }
}