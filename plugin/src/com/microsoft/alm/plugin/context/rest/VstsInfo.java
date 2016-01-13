// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context.rest;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.teamfoundation.core.webapi.model.TeamProjectCollectionReference;
import com.microsoft.teamfoundation.core.webapi.model.TeamProjectReference;

import java.util.UUID;

/**
 * Wrapper class for JSON response from RemoteGitURL/vsts/info
 */
public class VstsInfo {
    private String serverUrl;
    private TeamProjectCollectionReference collection;
    private TeamProjectReference project;
    private UUID repoId;
    private String repoName;

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    @JsonProperty("collection")
    public TeamProjectCollectionReference getCollectionReference() {
        return collection;
    }

    @JsonProperty("collection")
    public void setCollectionReference(TeamProjectCollectionReference collection) {
        this.collection = collection;
    }

    @JsonProperty("project")
    public TeamProjectReference getProjectReference() {
        return project;
    }

    @JsonProperty("project")
    public void setProjectReference(TeamProjectReference project) {
        this.project = project;
    }

    public UUID getRepoId() {
        return repoId;
    }

    public void setRepoId(UUID repoId) {
        this.repoId = repoId;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }
}