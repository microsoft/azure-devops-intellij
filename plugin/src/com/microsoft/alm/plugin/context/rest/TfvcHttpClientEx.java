// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.microsoft.alm.client.model.ApiResourceVersion;
import com.microsoft.alm.client.model.NameValueCollection;
import com.microsoft.alm.sourcecontrol.webapi.TfvcHttpClient;
import com.microsoft.alm.sourcecontrol.webapi.model.TfvcItem;
import com.microsoft.alm.sourcecontrol.webapi.model.TfvcVersionDescriptor;

import javax.ws.rs.client.Client;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Extending the TFVC Client with new/altered calls
 */
public class TfvcHttpClientEx extends TfvcHttpClient {

    public TfvcHttpClientEx(final Client jaxrsClient, final URI baseUrl) {
        super(jaxrsClient, baseUrl);
    }

    /**
     * This method is needed because the generated getItems method in 0.4.3 generated client does not pass the correct
     * recursion level values. They are case sensitive and the generated client has the incorrect casing.
     *
     * @param project
     * @param scopePath
     * @param recursionLevel
     * @param versionDescriptor
     * @return
     */
    public List<TfvcItem> getItems(
            final UUID project,
            final String scopePath,
            final VersionControlRecursionTypeCaseSensitive recursionLevel,
            final TfvcVersionDescriptor versionDescriptor) {
        final UUID locationId = UUID.fromString("ba9fc436-9a38-4578-89d6-e4f3241f5040"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("scopePath", scopePath); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursionLevel", recursionLevel); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeLinks", true); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, versionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                locationId,
                routeValues,
                apiVersion,
                queryParameters,
                APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<TfvcItem>>() {
        });
    }
}
