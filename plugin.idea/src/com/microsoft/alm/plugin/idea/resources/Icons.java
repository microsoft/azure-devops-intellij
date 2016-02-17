// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.resources;

import com.intellij.openapi.util.IconLoader;

import javax.swing.Icon;

public class Icons {

    private static Icon load(String path) {
        return IconLoader.getIcon(path);
    }

    public static final Icon Help = load("/icons/help.png");
    public static final Icon Frown = load("/icons/frown.png");
    public static final Icon Smile = load("/icons/smile.png");
    public static final Icon VSLogo = load("/icons/vs-logo.png");
    public static final Icon VSLogoSmall = load("/icons/vs-logo_small.png");
    public static final Icon WindowsAccount = load("/icons/windows-account.png");
    public static final Icon VsoAccount = load("/icons/vso-account.png");

    //pull requests tab
    public static final Icon PR_STATUS_SUCCEEDED = load("/icons/pr-status-succeeded.png");
    public static final Icon PR_STATUS_FAILED = load("/icons/pr-status-failed.png");
    public static final Icon PR_STATUS_NO_RESPONSE = load("/icons/pr-status-noresponse.png");
    public static final Icon PR_STATUS_WAITING = load("/icons/pr-status-waiting.png");
}
