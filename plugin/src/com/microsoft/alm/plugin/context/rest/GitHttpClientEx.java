// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context.rest;

import com.microsoft.alm.client.AlmHttpClientBase;
import com.microsoft.alm.client.model.ApiResourceVersion;
import com.microsoft.alm.client.model.NameValueCollection;
import com.microsoft.alm.sourcecontrol.webapi.GitHttpClient;
import com.microsoft.alm.sourcecontrol.webapi.model.GitPullRequest;

import javax.ws.rs.client.Client;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Extending GitHttpClient to include new API calls
 */
public class GitHttpClientEx extends GitHttpClient {

    public GitHttpClientEx(final Client jaxrsClient, final URI baseUrl) {
        super(jaxrsClient, baseUrl);
    }

    /**
     * [Preview API 3.1-preview.1] Create a git pull request
     *
     * @param gitPullRequestToCreate
     * @param repositoryId
     * @param linkBranchWorkitems
     * @return GitPullRequest
     */
    public GitPullRequest createPullRequest(
            final GitPullRequest gitPullRequestToCreate,
            final UUID project,
            final UUID repositoryId,
            final Boolean linkBranchWorkitems) {

        final UUID locationId = UUID.fromString("9946fd70-0d40-406e-b686-b4744cbbcc37"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("3.1-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("linkBranchWorkitems", linkBranchWorkitems); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(AlmHttpClientBase.HttpMethod.POST,
                locationId,
                routeValues,
                apiVersion,
                gitPullRequestToCreate,
                APPLICATION_JSON_TYPE,
                queryParameters,
                APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitPullRequest.class);
    }
}
