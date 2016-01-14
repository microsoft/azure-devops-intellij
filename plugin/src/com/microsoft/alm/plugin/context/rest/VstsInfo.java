// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.teamfoundation.core.webapi.model.TeamProjectCollectionReference;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitRepository;

/**
 * Wrapper class for JSON response from RemoteGitURL/vsts/info
 */
public class VstsInfo {
    private String serverUrl;
    private TeamProjectCollectionReference collection;
    private GitRepository repository;

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

    @JsonProperty("repository")
    public GitRepository getRepository() {
        return repository;
    }

    @JsonProperty("repository")
    public void setRepository(GitRepository repository) {
        this.repository = repository;
    }
}