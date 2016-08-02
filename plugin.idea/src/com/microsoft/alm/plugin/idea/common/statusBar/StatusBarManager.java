// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.statusBar;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.microsoft.alm.client.utils.StringUtil;
import com.microsoft.alm.plugin.events.ServerEvent;
import com.microsoft.alm.plugin.events.ServerEventListener;
import com.microsoft.alm.plugin.events.ServerEventManager;
import com.microsoft.alm.plugin.idea.common.utils.EventContextHelper;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import com.microsoft.alm.plugin.idea.git.utils.TfGitHelper;
import com.microsoft.alm.plugin.operations.BuildStatusLookupOperation;
import com.microsoft.alm.plugin.operations.Operation;
import com.microsoft.alm.plugin.operations.OperationFactory;
import git4idea.branch.GitBranchUtil;
import git4idea.repo.GitRepository;

import java.util.Map;

public class StatusBarManager {
    private static ServerEventListener serverEventListener;

    public static void setupStatusBar() {
        if (serverEventListener == null) {
            serverEventListener = new ServerEventListener() {
                @Override
                public void serverChanged(final ServerEvent event, final Map<String, Object> contextMap) {
                    // When we receive an event that builds have changed, update the status bar (ON UI THREAD)
                    if (event == ServerEvent.BUILDS_CHANGED) {
                        IdeaHelper.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                // Check the context object to see if these change events were triggered by IntelliJ
                                if (EventContextHelper.isProjectOpened(contextMap)
                                        || EventContextHelper.isRepositoryChanged(contextMap)) {
                                    // On project opened or repo changed we use the project context to update the status bar
                                    updateStatusBar(EventContextHelper.getProject(contextMap), false);
                                } else if (EventContextHelper.isProjectClosing(contextMap)) {
                                    // On project closing we remove our widgets from the status bar
                                    removeWidgets(EventContextHelper.getProject(contextMap));
                                } else {
                                    // If there isn't any context, then we were called by the polling timer
                                    // Just update all the status bars for all the projects
                                    updateStatusBar();
                                }

                            }
                        });
                    }
                }
            };
            // Add the listener to the server event manager
            ServerEventManager.getInstance().addListener(serverEventListener);
        }
    }

    private static void updateStatusBar() {
        final Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        for (final Project p : openProjects) {
            updateStatusBar(p, false);
        }
    }

    public static void updateStatusBar(final Project project, final boolean allowPrompt) {
        final StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
        if (statusBar != null) {
            updateWidgets(statusBar, project, allowPrompt);
        }
    }

    private static void updateWidgets(final StatusBar statusBar, final Project project, final boolean allowPrompt) {
        // Update the build widget
        BuildWidget buildWidget = (BuildWidget) statusBar.getWidget(BuildWidget.getID());
        if (buildWidget == null) {
            buildWidget = new BuildWidget();
            statusBar.addWidget(buildWidget, project);
        }
        // Attempt to get the current repo and branch (if none, then the status stays as it was)
        final GitRepository repository = GitBranchUtil.getCurrentRepository(project);
        if (repository != null) {
            final String repoUrl = TfGitHelper.getTfGitRemoteUrl(repository);
            if (!StringUtil.isNullOrEmpty(repoUrl)) {
                // It's a tf git url so continue
                final BuildWidget widget = buildWidget;
                // TODO: Fix this HACK. There doesn't seem to be a clear way to get the full name of the current branch
                final String branch = "refs/heads/" + GitBranchUtil.getDisplayableBranchText(repository);

                // Create the operation and start the background work to get the latest build information
                final BuildStatusLookupOperation op = OperationFactory.createBuildStatusLookupOperation(repoUrl, branch, allowPrompt);
                op.addListener(new Operation.Listener() {
                    @Override
                    public void notifyLookupStarted() { /* do nothing */ }

                    @Override
                    public void notifyLookupCompleted() { /* do nothing */ }

                    @Override
                    public void notifyLookupResults(final Operation.Results results) {
                        updateBuildWidget(project, statusBar, widget, (BuildStatusLookupOperation.BuildStatusResults) results);
                    }
                });
                op.doWorkAsync(null);
            }
        } else {
            // The repository hasn't been opened yet, we should get an event when it is opened
        }
    }

    private static void updateBuildWidget(final Project project, final StatusBar statusBar, final BuildWidget widget, final BuildStatusLookupOperation.BuildStatusResults results) {
        final BuildStatusLookupOperation.BuildStatusResults r = results;
        final BuildStatusModel model = BuildStatusModel.create(project, results);
        widget.update(model);

        // Tell the UI to update and restart the timer
        // (This should be done on the UI thread)
        IdeaHelper.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                statusBar.updateWidget(BuildWidget.getID());
            }
        });
    }

    private static void removeWidgets(final Project project) {
        final StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
        if (statusBar != null) {
            // Remove build widget
            if (statusBar.getWidget(BuildWidget.getID()) != null) {
                statusBar.removeWidget(BuildWidget.getID());
            }
        }
    }
}
