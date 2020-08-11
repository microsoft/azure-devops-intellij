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

package com.microsoft.alm.plugin.idea.tfvc.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.actions.VcsContextFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.models.ItemInfo;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.tfvc.core.TFSVcs;
import com.microsoft.alm.plugin.idea.tfvc.exceptions.TfsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public abstract class SingleItemAction extends DumbAwareAction {
    protected static final Logger logger = LoggerFactory.getLogger(SingleItemAction.class);

    private static final Collection<FileStatus> ALLOWED_STATUSES =
            Arrays.asList(FileStatus.HIJACKED, FileStatus.MODIFIED, FileStatus.NOT_CHANGED, FileStatus.OBSOLETE);

    protected SingleItemAction(final String text, final String description) {
        super(text, description, null);
    }

    protected abstract void execute(final @NotNull SingleItemActionContext actionContext) throws TfsException;

    protected Collection<FileStatus> getAllowedStatuses() {
        return ALLOWED_STATUSES;
    }

    public void actionPerformed(final AnActionEvent e) {
        final Project project = e.getData(CommonDataKeys.PROJECT);
        final VirtualFile file = VcsUtil.getOneVirtualFile(e);

        if (project == null || file == null) {
            // This shouldn't happen, but just in case
            logger.warn("project or file is null in actionPerformed");
            return;
        }

        // checked by isEnabled()
        final FilePath localPath = VcsContextFactory.SERVICE.getInstance().createFilePathOn(file);

        final String actionTitle = StringUtil.trimEnd(e.getPresentation().getText(), "...");
        try {
            final SingleItemActionContext actionContext = new SingleItemActionContext(project, localPath);
            final List<VcsException> errors = new ArrayList<>();
            ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
                try {
                    ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
                    final ServerContext context = TFSVcs.getInstance(project).getServerContext(true);
                    final ItemInfo item = CommandUtils.getItemInfo(context, localPath.getPath());
                    actionContext.setItem(item);
                    actionContext.setServerContext(context);
                } catch (final Throwable t) {
                    errors.add(TFSVcs.convertToVcsException(t));
                }
            }, getProgressMessage(), false, project);

            if (!errors.isEmpty()) {
                AbstractVcsHelper.getInstance(project).showErrors(errors, TFSVcs.TFVC_NAME);
                return;
            }

            execute(actionContext);
        } catch (TfsException ex) {
            Messages.showErrorDialog(project, ex.getMessage(), actionTitle);
        }
    }

    public void update(@NotNull final AnActionEvent e) {
        e.getPresentation().setEnabled(isEnabled(e.getProject(), VcsUtil.getOneVirtualFile(e)));
    }

    protected String getProgressMessage() {
        return TfPluginBundle.message(TfPluginBundle.KEY_TFVC_UPDATE_STATUS_MSG);
    }

    protected final boolean isEnabled(@Nullable final Project project, @Nullable final VirtualFile file) {
        if (project == null
                || file == null
                || !getAllowedStatuses().contains(FileStatusManager.getInstance(project).getStatus(file)))
            return false;

        // TODO: Remove this suppression after migration to IDEA 2019.3. AbstractVcs became non-generic in newer IDEA.
        @SuppressWarnings("rawtypes") AbstractVcs vcs = VcsUtil.getVcsFor(project, file);
        return vcs != null && TFSVcs.getKey().equals(vcs.getKeyInstanceMethod());
    }

    public static class SingleItemActionContext {
        private final Project project;
        private final FilePath filePath;
        private ServerContext serverContext;
        private ItemInfo item;

        public SingleItemActionContext(final Project project, final FilePath filePath) {
            this.project = project;
            this.filePath = filePath;
        }

        public Project getProject() {
            return project;
        }

        public FilePath getFilePath() {
            return filePath;
        }

        public void setItem(ItemInfo item) {
            this.item = item;
        }

        public ItemInfo getItem() {
            return item;
        }

        public void setServerContext(ServerContext serverContext) {
            this.serverContext = serverContext;
        }

        public ServerContext getServerContext() {
            return serverContext;
        }
    }
}
