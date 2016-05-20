// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.statusBar;

import com.microsoft.alm.client.utils.StringUtil;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.utils.DateHelper;

import java.util.Date;

public class BuildStatusModel {
    private final boolean hasBuilds;
    private final boolean signedIn;
    private final boolean successful;
    private final String description;

    public static final BuildStatusModel EMPTY_STATUS = new BuildStatusModel();

    public BuildStatusModel() {
        this(false, null);
    }

    public BuildStatusModel(final boolean signedIn, final String errorMessage) {
        this.signedIn = signedIn;
        this.hasBuilds = false;
        this.successful = false;
        if (!StringUtil.isNullOrEmpty(errorMessage)) {
            this.description = TfPluginBundle.message(TfPluginBundle.KEY_STATUSBAR_BUILD_ERROR, errorMessage);
        } else if (!signedIn) {
            this.description = TfPluginBundle.message(TfPluginBundle.KEY_STATUSBAR_BUILD_UNKNOWN_STATUS); // + "\nClick the status bar icon to Sign In";
        } else {
            this.description = TfPluginBundle.message(TfPluginBundle.KEY_STATUSBAR_BUILD_NO_BUILDS_FOUND); // + "\nClick the status bar icon to go to the Builds web page";
        }
    }

    public BuildStatusModel(final boolean successful, final String buildName, final Date finishTime) {
        this.signedIn = true;
        this.hasBuilds = true;
        this.successful = successful;
        if (successful) {
            this.description = TfPluginBundle.message(TfPluginBundle.KEY_STATUSBAR_BUILD_SUCCEEDED, buildName, DateHelper.getFriendlyDateTimeString(finishTime)); // + "\nClick the status bar icon to go to the build results.";
        } else {
            this.description = TfPluginBundle.message(TfPluginBundle.KEY_STATUSBAR_BUILD_FAILED, buildName, DateHelper.getFriendlyDateTimeString(finishTime)); // + "\nClick the status bar icon to go to the build results.";
        }
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
}
