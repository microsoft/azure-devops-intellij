// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TFSDiffProvider implements DiffProvider {
    private
    @NotNull
    final Project myProject;

    public TFSDiffProvider(@NotNull final Project project) {
        myProject = project;
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
                final ServerContext serverContext = TFSVcs.getInstance(myProject).getServerContext(true);
                final TfsRevisionNumber revisionNumber = (TfsRevisionNumber) vcsRevisionNumber;
                return TFSContentRevision.create(myProject, serverContext, path, revisionNumber.getValue(), revisionNumber.getModificationDate());
            } catch (Exception e) {
                AbstractVcsHelper.getInstance(myProject).showError(new VcsException(e), TFSVcs.TFVC_NAME);
                return null;
            }
        }
    }

    @Nullable
    public VcsRevisionNumber getCurrentRevision(final VirtualFile virtualFile) {
        try {
            // TODO: we may have an issue here with new files that don't exist on the server and have not been explicity added (NEED TO TEST)
            final ItemInfo info = CommandUtils.getItemInfo(TFSVcs.getInstance(myProject).getServerContext(true), virtualFile.getPath());
            if (info != null) {
                return new TfsRevisionNumber(info.getLocalVersionAsInt(), virtualFile.getName(), info.getLastModified());
            }
        } catch (Exception e) {
            AbstractVcsHelper.getInstance(myProject).showError(new VcsException(e.getMessage(), e), TFSVcs.TFVC_NAME);
        }
        return VcsRevisionNumber.NULL;
    }

    public ItemLatestState getLastRevision(final FilePath localPath) {
        try {
            // TODO: we may have an issue here with new files that don't exist on the server and have not been explicity added (NEED TO TEST)
            final ItemInfo info = CommandUtils.getItemInfo(TFSVcs.getInstance(myProject).getServerContext(true), localPath.getPath());
            if (info != null) {
                final VcsRevisionNumber.Int revisionNumber = new TfsRevisionNumber(info.getServerVersionAsInt(), localPath.getName(), info.getLastModified());
                return new ItemLatestState(revisionNumber, info.getServerVersionAsInt() == 0, false);
            }

            /*
            Collection<WorkspaceInfo> workspaces = Workstation.getInstance().findWorkspaces(localPath, false, myProject);
            if (workspaces.isEmpty()) {
                return new ItemLatestState(VcsRevisionNumber.NULL, false, false);
            }
            final WorkspaceInfo workspace = workspaces.iterator().next();
            final ExtendedItem extendedItem = workspace.getServer().getVCS()
                    .getExtendedItem(workspace.getName(), workspace.getOwnerName(), localPath, RecursionType.None, DeletedState.Any, myProject,
                            TFSBundle.message("loading.item"));
            if (extendedItem == null) {
                return new ItemLatestState(VcsRevisionNumber.NULL, false, false);
            }
            // there may be several extended items for a given name (see VersionControlServer.chooseExtendedItem())
            // so we need to query item by name
            final Item item = workspace.getServer().getVCS()
                    .queryItem(workspace.getName(), workspace.getOwnerName(), extendedItem.getSitem(), LatestVersionSpec.INSTANCE, DeletedState.Any,
                            false, myProject, TFSBundle.message("loading.item"));
            if (item != null) {
                VcsRevisionNumber.Int revisionNumber = new TfsRevisionNumber(item.getCs(), item.getItemid());
                return new ItemLatestState(revisionNumber, item.getDid() == Integer.MIN_VALUE, false);
            }
            else {
                return new ItemLatestState(VcsRevisionNumber.NULL, false, false);
            }
            */
            return new ItemLatestState(VcsRevisionNumber.NULL, false, false);
        } catch (Exception e) {
            AbstractVcsHelper.getInstance(myProject).showError(new VcsException(e.getMessage(), e), TFSVcs.TFVC_NAME);
            return new ItemLatestState(VcsRevisionNumber.NULL, false, false);
        }
    }

    public VcsRevisionNumber getLatestCommittedRevision(VirtualFile vcsRoot) {
        // todo.
        return null;
    }
}
