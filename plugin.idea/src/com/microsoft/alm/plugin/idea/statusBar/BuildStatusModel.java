// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.statusBar;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.client.utils.StringUtil;
import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.common.AbstractModel;
import com.microsoft.alm.plugin.idea.utils.DateHelper;
import com.microsoft.alm.plugin.operations.BuildStatusLookupOperation;

import java.net.URI;
import java.util.Date;

public class BuildStatusModel extends AbstractModel {
    private final Project project;
    private final boolean hasBuilds;
    private final boolean signedIn;
    private final boolean successful;
    private final String description;
    private final BuildStatusLookupOperation.BuildStatusResults operationResults;

    // This static method converts the results returned by the Operation into a model that the UI can use
    public static BuildStatusModel create(final Project project, final BuildStatusLookupOperation.BuildStatusResults results) {
        final BuildStatusModel model;
        final boolean signedIn = results.getContext() != null;
        if (results.hasError()) {
            // TODO: we need to handle the 401 case and give the user the option to sign in
            // If there's an error we want to show the error to the user
            model = new BuildStatusModel(project, signedIn, results.getError().getMessage(), results);
        } else {
            if (results.isBuildFound()) {
                // We have a build so show the status details of the build
                model = new BuildStatusModel(project,
                        results.getBuildForDisplay().isSuccessful(),
                        results.getBuildForDisplay().getBuildName(),
                        results.getBuildForDisplay().getFinishTime(),
                        results);
            } else {
                // We couldn't find a build, so show the appropriate message (no builds or not signed in)
                model = new BuildStatusModel(project, signedIn, null, results);
            }
        }

        return model;
    }

    private BuildStatusModel(final Project project, final boolean signedIn, final String errorMessage, final BuildStatusLookupOperation.BuildStatusResults operationResults) {
        this.project = project;
        this.signedIn = signedIn;
        this.hasBuilds = false;
        this.successful = false;
        this.operationResults = operationResults;
        if (!StringUtil.isNullOrEmpty(errorMessage)) {
            this.description = TfPluginBundle.message(TfPluginBundle.KEY_STATUSBAR_BUILD_ERROR, errorMessage);
        } else if (!signedIn) {
            this.description = TfPluginBundle.message(TfPluginBundle.KEY_STATUSBAR_BUILD_UNKNOWN_STATUS); // + "\nClick the status bar icon to Sign In";
        } else {
            this.description = TfPluginBundle.message(TfPluginBundle.KEY_STATUSBAR_BUILD_NO_BUILDS_FOUND); // + "\nClick the status bar icon to go to the Builds web page";
        }
    }

    private BuildStatusModel(final Project project, final boolean successful, final String buildName, final Date finishTime, final BuildStatusLookupOperation.BuildStatusResults operationResults) {
        this.project = project;
        this.signedIn = true;
        this.hasBuilds = true;
        this.successful = successful;
        this.operationResults = operationResults;
        if (successful) {
            this.description = TfPluginBundle.message(TfPluginBundle.KEY_STATUSBAR_BUILD_SUCCEEDED, buildName, DateHelper.getFriendlyDateTimeString(finishTime)); // + "\nClick the status bar icon to go to the build results.";
        } else {
            this.description = TfPluginBundle.message(TfPluginBundle.KEY_STATUSBAR_BUILD_FAILED, buildName, DateHelper.getFriendlyDateTimeString(finishTime)); // + "\nClick the status bar icon to go to the build results.";
        }
    }

    public Project getProject() {
        return project;
    }

    public boolean hasStatusInformation() {
        return this.signedIn && this.hasBuilds;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getDescription() {
        return description;
    }

    public int getBuildCount() {
        return hasStatusInformation() ? operationResults.getBuilds().size() : 0;
    }

    public boolean getBuildSuccess(int index) {
        return operationResults.getBuilds().get(index).isSuccessful();
    }

    public String getBuildBranch(int index) {
        return operationResults.getBuilds().get(index).getBranch();
    }

    public URI getBuildURI(int index) {
        if (operationResults.getContext() != null && operationResults.getContext().getTeamProjectURI() != null) {
            return UrlHelper.getBuildURI(operationResults.getContext().getTeamProjectURI(),
                    operationResults.getBuilds().get(index).getBuildId());
        }

        return null;
    }
}
