// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context;

import com.microsoft.alm.client.model.VssServiceException;
import com.microsoft.alm.core.webapi.model.TeamProjectCollectionReference;
import com.microsoft.alm.plugin.AbstractTest;
import com.microsoft.alm.plugin.context.soap.CatalogServiceImpl;
import com.microsoft.alm.plugin.exceptions.TeamServicesException;
import com.microsoft.alm.plugin.mocks.MockCatalogService;
import org.apache.commons.httpclient.HttpException;
import org.junit.Assert;
import org.junit.Test;

public class CatalogServiceImplTest extends AbstractTest {
    @Test
    public void testConstructor() {
        ServerContext context = new ServerContextBuilder().type(ServerContext.Type.TFS).uri("http://server/path").build();
        CatalogServiceImpl catalogService = new CatalogServiceImpl(context);
    }

    @Test
    public void testGetProjectCollection_exists() throws HttpException {
        ServerContext context = new ServerContextBuilder().type(ServerContext.Type.TFS).uri("http://server/path").build();
        MockCatalogService catalogService = new MockCatalogService(context);
        catalogService.addResponse(getCatalogResourceXml("root", CatalogServiceImpl.ORGANIZATIONAL_ROOT));
        catalogService.addResponse(getCatalogResourceXml("server", CatalogServiceImpl.TEAM_FOUNDATION_SERVER_INSTANCE));
        catalogService.addResponse(getCatalogResourceXml("collection1", CatalogServiceImpl.PROJECT_COLLECTION));
        final TeamProjectCollectionReference projectCollection = catalogService.getProjectCollection("collection1");
        Assert.assertNotNull(projectCollection);
    }

    @Test
    public void testGetProjectCollection_notFound() throws HttpException {
        ServerContext context = new ServerContextBuilder().type(ServerContext.Type.TFS).uri("http://server/path").build();
        MockCatalogService catalogService = new MockCatalogService(context);
        catalogService.addResponse(getCatalogResourceXml("root", CatalogServiceImpl.ORGANIZATIONAL_ROOT));
        catalogService.addResponse(getCatalogResourceXml("server", CatalogServiceImpl.TEAM_FOUNDATION_SERVER_INSTANCE));
        catalogService.addResponse(getCatalogResourceXml("collection1", CatalogServiceImpl.PROJECT_COLLECTION));
        try {
            final TeamProjectCollectionReference projectCollection = catalogService.getProjectCollection("notFound");
            Assert.fail("should not get here");
        } catch (VssServiceException ex) {
            Assert.assertEquals(TeamServicesException.KEY_OPERATION_ERRORS, ex.getMessage());
        }
    }

    private String getCatalogResourceXml(String displayName, String typeIdentifier) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" +
                "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" >" +
                "<soap:Body>" +
                "<QueryNodesResponse><QueryNodesResult><CatalogResources>" +
                "<CatalogResource DisplayName=\"" + displayName + "\" ResourceTypeIdentifier=\"" + typeIdentifier + "\" MatchedQuery=\"True\">" +
                "<Properties>" +
                "<KeyValuePair>" +
                "<Key>InstanceId</Key>" +
                "<Value>60000C5E-C093-447e-A177-A09E47A60974</Value>" +
                "</KeyValuePair>" +
                "</Properties>" +
                "<NodeReferencePaths>" +
                "<NodeReferencePath>/reference/path/1/</NodeReferencePath>" +
                "</NodeReferencePaths>" +
                "</CatalogResource>" +
                "</CatalogResources></QueryNodesResult></QueryNodesResponse>" +
                "</soap:Body>" +
                "</soap:Envelope>";
    }


}
