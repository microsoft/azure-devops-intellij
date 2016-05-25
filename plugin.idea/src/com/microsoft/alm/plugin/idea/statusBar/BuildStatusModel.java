// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.statusBar;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.common.AbstractModel;
import com.microsoft.alm.plugin.idea.utils.DateHelper;
import com.microsoft.alm.plugin.operations.BuildStatusLookupOperation;
import org.jetbrains.annotations.NotNull;

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
    public static BuildStatusModel create(@NotNull final Project project, @NotNull final BuildStatusLookupOperation.BuildStatusResults results) {
        final BuildStatusModel model;
        final boolean signedIn = results.getContext() != null;
        if (results.hasError()) {
            if (AuthHelper.isNotAuthorizedError(results.getError())) {
                // Handle the 401 case and give the user the option to sign in
                model = new BuildStatusModel(project, false, TfPluginBundle.message(TfPluginBundle.KEY_STATUSBAR_BUILD_ERROR_AUTH), results);
            } else {
                // Show the error to the user (if we were not able to get a context object then they can Sign In)
                model = new BuildStatusModel(project, signedIn,
                        TfPluginBundle.message(TfPluginBundle.KEY_STATUSBAR_BUILD_ERROR, results.getError().getMessage()),
                        results);
            }
        } else {
            if (results.isBuildFound()) {
                // We have a build so show the status details of the build
                model = new BuildStatusModel(project,
                        results.getBuildForDisplay().isSuccessful(),
                        results.getBuildForDisplay().getBuildName(),
                        results.getBuildForDisplay().getFinishTime(),
                        results);
            } else if (signedIn) {
                // We couldn't find a build, but we are signed in
                model = new BuildStatusModel(project, true, TfPluginBundle.message(TfPluginBundle.KEY_STATUSBAR_BUILD_NO_BUILDS_FOUND), results);
            } else {
                // We couldn't find a build, and we are not signed in
                model = new BuildStatusModel(project, false, TfPluginBundle.message(TfPluginBundle.KEY_STATUSBAR_BUILD_UNKNOWN_STATUS), results);
            }
        }

        return model;
    }

    private BuildStatusModel(final Project project, final boolean signedIn, final String description, final BuildStatusLookupOperation.BuildStatusResults operationResults) {
        this.project = project;
        this.signedIn = signedIn;
        this.hasBuilds = false;
        this.successful = false;
        this.operationResults = operationResults;
        this.description = description;
    }

    private BuildStatusModel(final Project project, final boolean successful, final String buildName, final Date finishTime, final BuildStatusLookupOperation.BuildStatusResults operationResults) {
        this.project = project;
        this.signedIn = true;
        this.hasBuilds = true;
        this.successful = successful;
        this.operationResults = operationResults;
        if (successful) {
            this.description = TfPluginBundle.message(TfPluginBundle.KEY_STATUSBAR_BUILD_SUCCEEDED, buildName, DateHelper.getFriendlyDateTimeString(finishTime));
        } else {
            this.description = TfPluginBundle.message(TfPluginBundle.KEY_STATUSBAR_BUILD_FAILED, buildName, DateHelper.getFriendlyDateTimeString(finishTime));
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

    public URI getQueueBuildURI(final int index) {
        if (operationResults.getContext() != null
            && operationResults.getContext().getTeamProjectCollectionReference() != null
            && operationResults.getContext().getTeamProjectReference() != null) {
            return UrlHelper.getQueueBuildURI(operationResults.getContext().getServerUri(),
                    operationResults.getContext().getTeamProjectCollectionReference().getId().toString(),
                    operationResults.getContext().getTeamProjectReference().getName(),
                    operationResults.getBuilds().get(index).getDefinitionId());
        }

        return null;
    }

}
