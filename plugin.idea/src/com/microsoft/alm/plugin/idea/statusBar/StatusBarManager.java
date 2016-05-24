// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.statusBar;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.microsoft.alm.client.utils.StringUtil;
import com.microsoft.alm.plugin.idea.utils.IdeaHelper;
import com.microsoft.alm.plugin.idea.utils.TfGitHelper;
import com.microsoft.alm.plugin.operations.BuildStatusLookupOperation;
import com.microsoft.alm.plugin.operations.Operation;
import git4idea.branch.GitBranchUtil;
import git4idea.repo.GitRepository;

import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class StatusBarManager {
    private static ProjectEventListener listener;
    private static Timer timer;
    private static final int NORMAL_TIMER_DELAY = 5 * 60 * 1000; // TODO eventually get from settings
    private static final int MIN_TIMER_DELAY = 5 * 1000;

    public static void setupStatusBar() {
        if (listener == null) {
            listener = new ProjectEventListener();
            ProjectManager.getInstance().addProjectManagerListener(listener);
        }
        // TODO: pull out the polling logic into a common class that the VCS tabs can use as well
        if (timer == null) {
            timer = new Timer(NORMAL_TIMER_DELAY, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    timer.stop();
                    updateStatusBar();
                }
            });
            timer.setRepeats(false);
            timer.setInitialDelay(MIN_TIMER_DELAY); // The very first time we want to try quickly, after that we back off to TIMER_DELAY
            timer.start();
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
                final BuildStatusLookupOperation op = new BuildStatusLookupOperation(repoUrl, branch, allowPrompt);
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
            // The repository hasn't been opened yet, so try again quickly
            timer.setInitialDelay(MIN_TIMER_DELAY);
            timer.restart();
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
                // Update again based on the Normal delay
                timer.setInitialDelay(NORMAL_TIMER_DELAY);
                timer.restart();
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

    private static class ProjectEventListener implements ProjectManagerListener {
        @Override
        public void projectOpened(final Project project) {
            updateStatusBar(project, false);
        }

        @Override
        public boolean canCloseProject(final Project project) {
            return true;
        }

        @Override
        public void projectClosed(final Project project) {
            // nothing to do here
        }

        @Override
        public void projectClosing(final Project project) {
            // remove all our widgets
            removeWidgets(project);
        }
    }

}
