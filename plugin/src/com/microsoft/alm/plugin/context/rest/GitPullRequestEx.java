// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context.rest;

import com.microsoft.alm.sourcecontrol.webapi.model.GitPullRequest;
import com.microsoft.visualstudio.services.webapi.model.ResourceRef;

/**
 * Extending GitPullRequest to include more attributes
 */
public class GitPullRequestEx extends GitPullRequest {
    private ResourceRef[] workItemRefs;

    public ResourceRef[] getWorkItemRefs() {
        return workItemRefs;
    }

    public void setWorkItemRefs(final ResourceRef[] workItemRefs) {
        this.workItemRefs = workItemRefs;
    }
}