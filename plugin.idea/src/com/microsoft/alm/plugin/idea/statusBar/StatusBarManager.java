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
    private static final int TIMER_DELAY = 5 * 60 * 1000; // TODO eventually get from settings

    public static void setupStatusBar() {
        if (listener == null) {
            listener = new ProjectEventListener();
            ProjectManager.getInstance().addProjectManagerListener(listener);
        }
        // TODO: pull out the polling logic into a common class that the VCS tabs can use as well
        if (timer == null) {
            timer = new Timer(TIMER_DELAY, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    timer.stop();
                    updateStatusBar();
                }
            });
            timer.setRepeats(false);
            timer.setInitialDelay(1000);
            timer.start();
        }
    }

    private static void updateStatusBar() {
        final Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        if (openProjects.length > 0) {
            //TODO when is there more than one project open (need to test)
            updateStatusBar(openProjects[0]);
        }
    }

    private static void updateStatusBar(final Project project) {
        final StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
        if (statusBar != null) {
            updateWidgets(statusBar, project);
        }
    }

    private static void updateWidgets(final StatusBar statusBar, final Project project) {
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
                final BuildStatusLookupOperation op = new BuildStatusLookupOperation(repoUrl, branch);
                op.addListener(new Operation.Listener() {
                    @Override
                    public void notifyLookupStarted() { /* do nothing */ }

                    @Override
                    public void notifyLookupCompleted() { /* do nothing */ }

                    @Override
                    public void notifyLookupResults(final Operation.Results results) {
                        updateBuildWidget(statusBar, widget, (BuildStatusLookupOperation.BuildStatusResults) results);
                    }
                });
                op.doWorkAsync(null);
            }
        } else {
            timer.restart();
        }
    }

    private static void updateBuildWidget(final StatusBar statusBar, final BuildWidget widget, final BuildStatusLookupOperation.BuildStatusResults results) {
        final BuildStatusLookupOperation.BuildStatusResults r = results;
        final BuildStatusModel model;
        final boolean signedIn = r.getContext() != null;
        if (r.hasError()) {
            // TODO: we need to handle the 401 case and give the user the option to sign in
            // If there's an error we want to show the error to the user
            model = new BuildStatusModel(signedIn, r.getError().getMessage());
        } else {
            if (r.isBuildFound()) {
                // We have a build so show the status details of the build
                model = new BuildStatusModel(r.isSuccessful(), r.getBuildName(), r.getFinishTime());
            } else {
                // We couldn't find a build, so show the appropriate message (no builds or not signed in)
                model = new BuildStatusModel(signedIn, null);
            }
        }
        // Now that we have the model, update the widget
        widget.update(model);

        // Tell the UI to update and restart the timer
        // (This should be done on the UI thread)
        IdeaHelper.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                statusBar.updateWidget(BuildWidget.getID());
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
            updateStatusBar(project);
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
