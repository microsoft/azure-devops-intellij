// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.actions;


import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.actions.VcsContextFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import com.microsoft.alm.helpers.Path;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.models.ItemInfo;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.common.actions.InstrumentedAction;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.tfvc.core.TFSVcs;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * This class supports TFVC actions that work on one or more selected items.
 * Subclasses of this class should surround sections of code that may take some time with the
 * runWithProgress method and execute them as a runnable. Within those sections, you should not
 * show UI.
 */
public abstract class MultipleItemAction extends InstrumentedAction {
    public static final Logger logger = LoggerFactory.getLogger(MultipleItemAction.class);

    public MultipleItemAction(final String title, final String message) {
        super(title, message, null, false);
    }

    /**
     * This method must be overridden by the subclass to perform the action requested by the user.
     * The context parameter should contain all the item information needed by the action.
     *
     * @param actionContext
     */
    protected abstract void execute(final @NotNull MultipleItemActionContext actionContext);

    @Override
    public void doUpdate(final AnActionEvent anActionEvent) {
        final Project project = anActionEvent.getData(CommonDataKeys.PROJECT);
        final VirtualFile[] files = VcsUtil.getVirtualFiles(anActionEvent);
        anActionEvent.getPresentation().setEnabled(isEnabled(project, files));
    }

    @Override
    public void doActionPerformed(final AnActionEvent anActionEvent) {
        logger.info("Starting multiple item action");

        final MultipleItemActionContext context = new MultipleItemActionContext();
        context.project = anActionEvent.getData(CommonDataKeys.PROJECT);
        final VirtualFile[] files = VcsUtil.getVirtualFiles(anActionEvent);

        logger.info("Finding the list of selected files and getting itemInfos for each one");
        runWithProgress(context, new Runnable() {
            public void run() {
                ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
                context.serverContext = TFSVcs.getInstance(context.project).getServerContext(true);
                for (final VirtualFile file : files) {
                    final FilePath localPath = VcsContextFactory.SERVICE.getInstance().createFilePathOn(file);
                    final ItemInfo info = CommandUtils.getItemInfo(context.serverContext, localPath.getPath());
                    context.itemInfos.add(info);
                }

                // Set the default path and additional parameters
                if (context.itemInfos.size() > 0) {
                    logger.info("Setting the defaultLocalPath and workingFolder");
                    context.defaultLocalPath = context.itemInfos.get(0).getLocalItem();
                    context.isFolder = Path.directoryExists(context.defaultLocalPath);
                    context.workingFolder = context.isFolder ?
                            context.defaultLocalPath :
                            Path.getDirectoryName(context.defaultLocalPath);
                }
            }
        }, TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_LABEL_PROGRESS_GATHERING_INFORMATION));

        if (context.hasErrors()) {
            logger.info("Errors found; showing them and exiting");
            showErrors(context);
            return;
        }

        if (!context.hasItems()) {
            // Somehow we got here without items selected or we couldn't find the info for them.
            // This shouldn't happen, but just in case we won't continue
            logger.warn("We ended up without any items in the list and no errors occurred. We need to understand how this happened.");
            return;
        }

        // Now that we have all the item infos, we can execute the body of this action
        logger.info("Calling the subclasses execute method to do the actual work.");
        execute(context);

        if (context.cancelled) {
            return;
        }

        if (context.hasErrors()) {
            logger.info("Errors found; showing them and exiting");
            showErrors(context);
        }
    }

    /**
     * Runs the given runnable and catches any exceptions. If an exception does occur, it is added to the errors list.
     * This method returns false if errors exist and true otherwise.
     *
     * @param context
     * @param runnable
     * @return
     */
    protected boolean runWithProgress(final MultipleItemActionContext context, final Runnable runnable, final String progressMessage) {
        ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
            public void run() {
                try {
                    logger.info("running with progress: " + progressMessage);
                    runnable.run();
                } catch (final Throwable t) {
                    logger.warn("runnable threw an error", t);
                    context.errors.add(TFSVcs.convertToVcsException(t));
                }
            }
        }, progressMessage, false, context.project);

        return !context.hasErrors();
    }

    /**
     * This method enables the action if one or more items are selected.
     *
     * @param project
     * @param files
     * @return
     */
    protected boolean isEnabled(final Project project, final VirtualFile[] files) {
        if (files.length == 0) {
            return false;
        }

        final FileStatusManager fileStatusManager = FileStatusManager.getInstance(project);
        for (VirtualFile file : files) {
            final FileStatus fileStatus = fileStatusManager.getStatus(file);
            if (fileStatus != FileStatus.NOT_CHANGED && fileStatus != FileStatus.MODIFIED && fileStatus != FileStatus.HIJACKED) {
                return false;
            }
        }

        return true;
    }

    /**
     * This method shows the errors in the VCS error window.
     *
     * @param context
     */
    protected void showErrors(final MultipleItemActionContext context) {
        AbstractVcsHelper.getInstance(context.project).showErrors(context.errors, TFSVcs.TFVC_NAME);
    }

    protected void showSuccess(final MultipleItemActionContext context, final String title, final String successMessage) {
        Messages.showInfoMessage(context.project, successMessage, title);
    }

    /**
     * This internal class is used to keep track of our context thru all the runnables above.
     */
    protected static class MultipleItemActionContext {
        protected Project project;
        protected ServerContext serverContext;
        protected String defaultLocalPath;
        protected boolean isFolder;
        protected String workingFolder;
        protected boolean cancelled = false;
        protected final List<VcsException> errors = new ArrayList<VcsException>();
        protected final List<ItemInfo> itemInfos = new ArrayList<ItemInfo>();

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public boolean hasItems() {
            return !itemInfos.isEmpty();
        }
    }
}
