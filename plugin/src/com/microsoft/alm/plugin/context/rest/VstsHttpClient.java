// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context.rest;

import com.microsoft.alm.client.model.VssException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

/**
 * Class to handle REST calls that are not part of the VSTS Java REST SDK yet, can be removed once these methods are part of the REST SDK.
 */
public class VstsHttpClient {
    public static final Logger logger = LoggerFactory.getLogger(VstsHttpClient.class);

    /**
     * Send a HTTP GET request to a URI and read JSON response as object of specified class
     *
     * @param client
     * @param uri
     * @param resultClass
     * @param <TResult>
     * @return Object of type resultClass
     */
    public static <TResult> TResult sendRequest(final Client client, final String uri, final Class<TResult> resultClass) {
        final WebTarget t = client.target(uri);
        final Invocation.Builder b = t.request();
        final Response r = b.get();
        if (r.getStatus() == 200) {
            return r.readEntity(resultClass);
        } else {
            logger.warn("sendRequest error: " + r.getStatus() + " : " + r.getStatusInfo().getReasonPhrase());
            throw new VstsHttpClientException(r.getStatus(), r.getStatusInfo().getReasonPhrase(), null);
        }
    }

    public static class VstsHttpClientException extends VssException {
        final int statusCode;

        public VstsHttpClientException(final int statusCode, final String message, final Exception innerException) {
            super(message, innerException);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
