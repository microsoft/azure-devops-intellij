// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.mocks;

import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.soap.CatalogServiceImpl;
import org.apache.commons.httpclient.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MockCatalogService extends CatalogServiceImpl {

    private List<HttpResponse> responses = new ArrayList<HttpResponse>(10);

    public MockCatalogService(ServerContext context) {
        super(context);
    }

    public void addResponse(String response) throws HttpException {
        final HttpResponse httpResponse = new BasicHttpResponse(
                new BasicStatusLine(new ProtocolVersion("https", 1, 0), HttpStatus.SC_OK, "reason"));
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(response.getBytes()));
        httpResponse.setEntity(entity);
        responses.add(httpResponse);
    }

    @Override
    protected HttpResponse executeRequest(HttpPost httpPost) throws IOException {
        HttpResponse r = responses.get(0);
        responses.remove(0);
        return r;
    }
}
