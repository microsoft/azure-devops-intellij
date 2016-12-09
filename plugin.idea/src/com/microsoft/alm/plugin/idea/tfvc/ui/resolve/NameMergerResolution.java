// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.resolve;

import org.apache.commons.lang.StringUtils;

public class NameMergerResolution {
    final String theirNameChoice;
    final String myNameChoice;
    final String userSelection;
    String resolvedLocalPath;

    public NameMergerResolution(final String theirNameChoice, final String myNameChoice, final String userSelection) {
        this.theirNameChoice = theirNameChoice;
        this.myNameChoice = myNameChoice;
        this.userSelection = userSelection;
    }

    public String getMyNameChoice() {
        return myNameChoice;
    }

    public String getTheirNameChoice() {
        return theirNameChoice;
    }

    public String getUserSelection() {
        return userSelection;
    }

    public boolean userChoseTheirs() {
        return StringUtils.equalsIgnoreCase(theirNameChoice, userSelection);
    }

    public void setResolvedLocalPath(String resolvedLocalPath) {
        this.resolvedLocalPath = resolvedLocalPath;
    }

    public String getResolvedLocalPath() {
        return resolvedLocalPath;
    }
}
