// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.common;

/**
 * Status for tabs in VersionControl tool window
 */
public enum VcsTabStatus {
    NOT_TF_GIT_REPO, //the git repository current project belongs to is not a TF git repository, could be Github or another Git remote
    NO_AUTH_INFO, //unable to find authenticated context for the git remote url
    LOADING_IN_PROGRESS,
    LOADING_COMPLETED,
    LOADING_COMPLETED_ERRORS, //loading completed but there were unexpected errors
}
