// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context.rest;

import com.microsoft.vss.client.core.model.VssServiceResponseException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

/**
 * Class to handle REST calls that are not part of the VSTS Java REST SDK yet, can be removed once these methods are part of the REST SDK.
 */
public class VstsHttpClient {

    /**
     * Send a HTTP GET request to a URI and read JSON response as object of specified class
     * @param client
     * @param uri
     * @param resultClass
     * @param <TResult>
     * @return Object of type resultClass
     */
    public static <TResult> TResult sendRequest(final Client client, final String uri, final Class<TResult> resultClass) {
        WebTarget t = client.target(uri);
        Invocation.Builder b = t.request();
        Response r = b.get();
        if (r.getStatus() == 200) {
            return r.readEntity(resultClass);
        } else {
            throw new VssServiceResponseException(r.getStatusInfo(), r.getStatusInfo().getReasonPhrase(), null);
        }
    }
}
