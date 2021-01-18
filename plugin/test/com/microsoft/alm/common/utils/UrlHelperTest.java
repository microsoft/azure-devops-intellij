// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.common.utils;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class UrlHelperTest {

    private final UrlHelper.ParseResultValidator validator = new UrlHelper.ParseResultValidator() {
        @Override
        public boolean validate(final UrlHelper.ParseResult result) {
            return result.getProjectName().equals("project");
        }
    };

    @Test
    public void testRemoveUserInfo() {
        assertEquals("https://dev.azure.com", UrlHelper.removeUserInfo("https://username@dev.azure.com"));
        assertEquals("https://microsoft.com", UrlHelper.removeUserInfo("https://username@microsoft.com"));
        assertEquals("https://dev.azure.com", UrlHelper.removeUserInfo("https://dev.azure.com"));
        assertEquals("https://dev.azure.com", UrlHelper.removeUserInfo("https://username:password@dev.azure.com"));
    }

    @Test
    public void testCreateOrganizationUri() {
        assertEquals(URI.create("https://dev.azure.com/username"), UrlHelper.createOrganizationUri("dev.azure.com", "username"));
        assertNull(UrlHelper.createOrganizationUri("microsoft.com", "username"));
        assertEquals(URI.create("https://dev.azure.com/"), UrlHelper.createOrganizationUri("dev.azure.com", ""));
    }

    @Test
    public void testResolveEndpointURI() throws Exception {

        URI expected, resolved;

        expected = URI.create("http://foo/Bar");

        resolved = UrlHelper.resolveEndpointUri(URI.create("http://foo"), "Bar");
        assertEquals(expected, resolved);

        resolved = UrlHelper.resolveEndpointUri(URI.create("http://foo/"), "Bar");
        assertEquals(expected, resolved);

        resolved = UrlHelper.resolveEndpointUri(URI.create("http://foo"), "/Bar");
        assertEquals(expected, resolved);

        resolved = UrlHelper.resolveEndpointUri(URI.create("http://foo/"), "/Bar");
        assertEquals(expected, resolved);

        expected = URI.create("http://foo:8080/Bar");

        resolved = UrlHelper.resolveEndpointUri(URI.create("http://foo:8080"), "Bar");
        assertEquals(expected, resolved);

        resolved = UrlHelper.resolveEndpointUri(URI.create("http://foo:8080/"), "Bar");
        assertEquals(expected, resolved);

        resolved = UrlHelper.resolveEndpointUri(URI.create("http://foo:8080"), "/Bar");
        assertEquals(expected, resolved);

        resolved = UrlHelper.resolveEndpointUri(URI.create("http://foo:8080/"), "/Bar");
        assertEquals(expected, resolved);

        expected = URI.create("http://foo:8080/serverPath/Bar");

        resolved = UrlHelper.resolveEndpointUri(URI.create("http://foo:8080/serverPath"), "Bar");
        assertEquals(expected, resolved);

        resolved = UrlHelper.resolveEndpointUri(URI.create("http://foo:8080/serverPath/"), "Bar");
        assertEquals(expected, resolved);

        resolved = UrlHelper.resolveEndpointUri(URI.create("http://foo:8080/serverPath"), "/Bar");
        assertEquals(expected, resolved);

        resolved = UrlHelper.resolveEndpointUri(URI.create("http://foo:8080/serverPath/"), "/Bar");
        assertEquals(expected, resolved);

        expected = URI.create("http://foo:8080/serverPath/servicePath/Bar");

        resolved = UrlHelper.resolveEndpointUri(URI.create("http://foo:8080/serverPath"), "servicePath/Bar");
        assertEquals(expected, resolved);

        resolved = UrlHelper.resolveEndpointUri(URI.create("http://foo:8080/serverPath/"), "servicePath/Bar");
        assertEquals(expected, resolved);

        resolved = UrlHelper.resolveEndpointUri(URI.create("http://foo:8080/serverPath"), "/servicePath/Bar");
        assertEquals(expected, resolved);

        resolved = UrlHelper.resolveEndpointUri(URI.create("http://foo:8080/serverPath/"), "/servicePath/Bar");
        assertEquals(expected, resolved);

    }

    @Test
    public void testBasicTryParseScenarios() {

        // only support http/https protocol
        UrlHelper.ParseResult result = UrlHelper.tryParse("protocol1://test.visualstudio.com/collection/project/_git/test", validator);
        assertFalse(result.isSuccess());

        // ssh is not supported yet
        result = UrlHelper.tryParse("git@test.com:test/test.git", validator);
        assertFalse(result.isSuccess());

        result = UrlHelper.tryParse("http://test.visualstudio.com/collection/project/_git/repo", validator);
        assertTrue(result.isSuccess());
        assertEquals("project", result.getProjectName());
        assertEquals("repo", result.getRepoName());
        assertEquals("http://test.visualstudio.com/collection/", result.getCollectionUrl());

        result = UrlHelper.tryParse("https://test.visualstudio.com/collection/_git/project", validator);
        assertTrue(result.isSuccess());
        assertEquals("project", result.getProjectName());
        assertEquals("project", result.getRepoName());
        assertEquals("https://test.visualstudio.com/collection/", result.getCollectionUrl());

        result = UrlHelper.tryParse("https://localhost:8080/iispath1/iispath2/iispath3/collection/project/_git/repo?queryParam=query", validator);
        assertTrue(result.isSuccess());
        assertEquals("project", result.getProjectName());
        assertEquals("repo", result.getRepoName());
        assertEquals("https://localhost:8080/iispath1/iispath2/iispath3/collection/", result.getCollectionUrl());

        result = UrlHelper.tryParse("http://localhost:8080/iispath1/iispath2/iispath3/collection%20with%20space/_git/project#withfragments", validator);
        assertTrue(result.isSuccess());
        assertEquals("project", result.getProjectName());
        assertEquals("project", result.getRepoName());
        assertEquals("collection with space", result.getCollectionName());
        assertEquals("http://localhost:8080/iispath1/iispath2/iispath3/collection with space/", result.getCollectionUrl());
    }

    @Test
    public void testTryParseWithLimitedRefs() {
        UrlHelper.ParseResult result = UrlHelper.tryParse("http://test.visualstudio.com/collection/project/_git/_optimized/repo", validator);
        assertTrue(result.isSuccess());
        assertEquals("project", result.getProjectName());
        assertEquals("repo", result.getRepoName());
        assertEquals("http://test.visualstudio.com/collection/", result.getCollectionUrl());

        result = UrlHelper.tryParse("https://test.visualstudio.com/collection/_git/_full/project", validator);
        assertTrue(result.isSuccess());
        assertEquals("project", result.getProjectName());
        assertEquals("project", result.getRepoName());
        assertEquals("https://test.visualstudio.com/collection/", result.getCollectionUrl());
    }

    @Test
    public void testTryParseWithMalformedUrl() {
        UrlHelper.ParseResult result = UrlHelper.tryParse("", validator);
        assertFalse(result.isSuccess());

        result = UrlHelper.tryParse(null, validator);
        assertFalse(result.isSuccess());

        result = UrlHelper.tryParse("abc/def", validator);
        assertFalse(result.isSuccess());

        result = UrlHelper.tryParse("abc\\def", validator);
        assertFalse(result.isSuccess());
    }

    @Test
    public void testTryParseGitHubStyleUrl() {
        UrlHelper.ParseResult result = UrlHelper.tryParse("https://test.com/account/project.git", validator);
        assertFalse(result.isSuccess());

        result = UrlHelper.tryParse("git@test.com:account/project.git", validator);
        assertFalse(result.isSuccess());
    }

    @Test
    public void testTrimTrailing() {
        assertEquals(null, UrlHelper.trimTrailingSeparators(null));
        assertEquals("", UrlHelper.trimTrailingSeparators(""));
        assertEquals("", UrlHelper.trimTrailingSeparators("/"));
        assertEquals("", UrlHelper.trimTrailingSeparators("////"));
        assertEquals("one", UrlHelper.trimTrailingSeparators("one"));
        assertEquals("one", UrlHelper.trimTrailingSeparators("one/"));
        assertEquals("one", UrlHelper.trimTrailingSeparators("one//"));
        assertEquals("one/two", UrlHelper.trimTrailingSeparators("one/two"));
        assertEquals("one/two", UrlHelper.trimTrailingSeparators("one/two/"));
        assertEquals("one/two", UrlHelper.trimTrailingSeparators("one/two//"));
        assertEquals("one/two/three", UrlHelper.trimTrailingSeparators("one/two/three"));
        assertEquals("one/two/three", UrlHelper.trimTrailingSeparators("one/two/three/"));
        assertEquals("one/two/three", UrlHelper.trimTrailingSeparators("one/two/three///"));
        assertEquals("/one/two/three", UrlHelper.trimTrailingSeparators("/one/two/three/"));
    }

    @Test
    public void testTrimLeadingAndTrailing() {
        assertEquals(null, UrlHelper.trimLeadingAndTrailingSeparators(null));
        assertEquals("", UrlHelper.trimLeadingAndTrailingSeparators(""));
        assertEquals("", UrlHelper.trimLeadingAndTrailingSeparators("/"));
        assertEquals("", UrlHelper.trimLeadingAndTrailingSeparators("////"));
        assertEquals("one", UrlHelper.trimLeadingAndTrailingSeparators("one"));
        assertEquals("one", UrlHelper.trimLeadingAndTrailingSeparators("one/"));
        assertEquals("one", UrlHelper.trimLeadingAndTrailingSeparators("one//"));
        assertEquals("one", UrlHelper.trimLeadingAndTrailingSeparators("///one//"));
        assertEquals("one/two", UrlHelper.trimLeadingAndTrailingSeparators("one/two"));
        assertEquals("one/two", UrlHelper.trimLeadingAndTrailingSeparators("one/two/"));
        assertEquals("one/two", UrlHelper.trimLeadingAndTrailingSeparators("one/two//"));
        assertEquals("one/two", UrlHelper.trimLeadingAndTrailingSeparators("//one/two//"));
        assertEquals("one/two/three", UrlHelper.trimLeadingAndTrailingSeparators("one/two/three"));
        assertEquals("one/two/three", UrlHelper.trimLeadingAndTrailingSeparators("one/two/three/"));
        assertEquals("one/two/three", UrlHelper.trimLeadingAndTrailingSeparators("one/two/three///"));
        assertEquals("one/two/three", UrlHelper.trimLeadingAndTrailingSeparators("///one/two/three/"));
    }

    @Test
    public void testCollectionInDomainUrl() {
        //vsts account
        final URI accountUri = URI.create("https://myorganization.visualstudio.com");

        //collection not in domain
        final URI defaultCollectionUri = UrlHelper.getCollectionURI(accountUri, "DefaultCollection");
        assertEquals(URI.create("https://myorganization.visualstudio.com/DefaultCollection"), defaultCollectionUri);

        //collection in domain
        final URI inDomainCollectionUri = UrlHelper.getCollectionURI(accountUri, "myorganization");
        assertEquals(accountUri, inDomainCollectionUri);

        //Azure accounts
        final URI azureUri = URI.create("https://azure.com/acct1");
        assertEquals(azureUri, UrlHelper.getCollectionURI(azureUri, "DefaultCollection"));

        final URI azureOrgUri = URI.create("https://org.azure.com/acct1");
        assertEquals(azureOrgUri, UrlHelper.getCollectionURI(azureOrgUri, "DefaultCollection"));

        //onprem server
        final URI serverUri = URI.create("http://myserver:8080/tfs");

        final URI collectionUri1 = UrlHelper.getCollectionURI(serverUri, "FabrikamCollection");
        assertEquals(URI.create("http://myserver:8080/tfs/FabrikamCollection"), collectionUri1);

        final URI collectionUri2 = UrlHelper.getCollectionURI(serverUri, "myserver");
        assertEquals(URI.create("http://myserver:8080/tfs/myserver"), collectionUri2);

    }

    @Test
    public void testSshGitUrls() {
        assertEquals("https://myorganization.visualstudio.com/myCollection/_git/My.Repo",
                UrlHelper.getHttpsGitUrlFromSshUrl("ssh://myaccount@myorganization.visualstudio.com:22/myCollection/_git/My.Repo"));

        assertEquals("https://myorganization.visualstudio.com/myCollection/_git/My.Repo",
                UrlHelper.getHttpsGitUrlFromSshUrl("myaccount@myorganization.visualstudio.com:22/myCollection/_git/My.Repo"));

        assertEquals("https://myorganization.visualstudio.com/myCollection/_git/My.Repo",
                UrlHelper.getHttpsGitUrlFromSshUrl("https://myorganization.visualstudio.com/myCollection/_git/My.Repo"));

        assertNull(UrlHelper.getHttpsGitUrlFromSshUrl("ssh://git@github.com:Microsoft/vso-agent-tasks.git"));

        assertNull(UrlHelper.getHttpsGitUrlFromSshUrl("git@github.com:Microsoft/vso-agent-tasks.git"));

    }

    @Test
    public void testParseUriForBranch_Happy() {
        String uri = "vstfs:///Git/Ref/00000000-0000-0000-0000-000000000000%2F11111111-1111-1111-1111-111111111111%2FGBMy%2FBranch%2FName";
        assertEquals("My/Branch/Name", UrlHelper.parseUriForBranch(uri));
    }

    @Test
    public void testParseUriForBranch_Empty() {
        assertEquals(StringUtils.EMPTY, UrlHelper.parseUriForBranch(null));

        assertEquals(StringUtils.EMPTY, UrlHelper.parseUriForBranch(StringUtils.EMPTY));
    }

    @Test
    public void testCombine() {
        assertEquals("", UrlHelper.combine(null));
        assertEquals("", UrlHelper.combine(""));
        assertEquals("", UrlHelper.combine("", null, ""));
        assertEquals("", UrlHelper.combine("", "", ""));
        assertEquals("", UrlHelper.combine(null, null, null));
        assertEquals("one", UrlHelper.combine("one"));
        assertEquals("one/two", UrlHelper.combine("one", "two"));
        assertEquals("one/two/three", UrlHelper.combine("one", "two", "three"));
        assertEquals("one/two/three", UrlHelper.combine("/one//", "//two/", "/three"));
    }

    @Test
    public void testGetFileURI_NoBranch() {
        String remoteUrl = "https://myorganization.visualstudio.com";
        String filePath = "path/to/file.txt";

        final URI result = UrlHelper.getFileURI(remoteUrl, filePath, StringUtils.EMPTY);
        assertEquals(remoteUrl.concat(UrlHelper.URL_PATH_SEGMENT).concat(filePath), result.toString());
    }

    @Test
    public void testGetFileURI_Branch() {
        String remoteUrl = "https://myorganization.visualstudio.com";
        String filePath = "path/to/file.txt";
        String branchName = "branch";

        final URI result = UrlHelper.getFileURI(remoteUrl, filePath, branchName);
        assertEquals(remoteUrl.concat(UrlHelper.URL_PATH_SEGMENT).concat(filePath).concat(UrlHelper.URL_GIT_VERSION_SEGMENT).concat(branchName),
                result.toString());
    }

    @Test
    public void testGetTfvcAnnotateURI() {
        final String collection = "https://server:8081/tfs/DefaultCollection/";
        final String projectName = "Project Name";
        final String filePath = "$/path/to/the/file.txt";

        final URI result = UrlHelper.getTfvcAnnotateURI(collection, projectName, filePath);
        assertEquals("https://server:8081/tfs/DefaultCollection/Project%20Name/_versionControl/?path=%24%2Fpath%2Fto%2Fthe%2Ffile.txt&_a=contents&annotate=true&hideComments=true",
                result.toString());
    }

    @Test
    public void testIsOrganizationUrl() {
        assertEquals(false, UrlHelper.isOrganizationUrl( "https://myorganization.visualstudio.com/DefaultCollection"));
        assertEquals(false, UrlHelper.isOrganizationUrl( "https://myorganization.visualstudio.com/"));
        assertEquals(false, UrlHelper.isOrganizationUrl( "https://www.google.com/"));
        assertEquals(true, UrlHelper.isOrganizationUrl( "https://azure.com/account"));
        assertEquals(true, UrlHelper.isOrganizationUrl( "https://AZURE.COM/ACCOUNT"));
        assertEquals(true, UrlHelper.isOrganizationUrl( "https://msft.azure.com/account"));
        assertEquals(false, UrlHelper.isOrganizationUrl( "not a url"));
    }

    @Test
    public void testIsOrganizationUri() {
        assertEquals(false, UrlHelper.isOrganizationURI( URI.create("https://myorganization.visualstudio.com/DefaultCollection")));
        assertEquals(false, UrlHelper.isOrganizationURI( URI.create("https://myorganization.visualstudio.com/")));
        assertEquals(false, UrlHelper.isOrganizationURI( URI.create("https://www.google.com/")));
        assertEquals(true, UrlHelper.isOrganizationURI( URI.create("https://azure.com/account")));
        assertEquals(true, UrlHelper.isOrganizationURI( URI.create("https://AZURE.COM/ACCOUNT")));
        assertEquals(true, UrlHelper.isOrganizationURI( URI.create("https://msft.azure.com/account")));
    }

    @Test
    public void testIsOrganizationHost() {
        assertEquals(false, UrlHelper.isOrganizationHost( "myorganization.visualstudio.com"));
        assertEquals(false, UrlHelper.isOrganizationHost( "myorganization.visualstudio.com"));
        assertEquals(false, UrlHelper.isOrganizationHost( "www.google.com"));
        assertEquals(true, UrlHelper.isOrganizationHost( "azure.com"));
        assertEquals(true, UrlHelper.isOrganizationHost( "AZURE.COM"));
        assertEquals(true, UrlHelper.isOrganizationHost( "msft.azure.com"));
        assertEquals(true, UrlHelper.isOrganizationHost( "msft.azure.COM"));
    }

    @Test
    public void testGetAccountFromOrganization() {
        assertEquals("account", UrlHelper.getAccountFromOrganization( "https://azure.com/account"));
        assertEquals("ACCOUNT", UrlHelper.getAccountFromOrganization( "https://AZURE.COM/ACCOUNT"));
        assertEquals("account", UrlHelper.getAccountFromOrganization( "https://msft.azure.com/account"));
        // the below does not look correct, but is here to capture current behavior of other functions
        // in code this result will fail later on when we attempt to make a request
        assertEquals("not a url", UrlHelper.getAccountFromOrganization( "not a url"));
    }

    @Test
    public void testGetAccountFromOrganizationUri() {
        assertEquals("account", UrlHelper.getAccountFromOrganizationUri( URI.create("https://azure.com/account")));
        assertEquals("ACCOUNT", UrlHelper.getAccountFromOrganizationUri( URI.create("https://AZURE.COM/ACCOUNT")));
        assertEquals("account", UrlHelper.getAccountFromOrganizationUri( URI.create("https://msft.azure.com/account")));
        assertEquals("", UrlHelper.getAccountFromOrganizationUri( URI.create("https://msft.cazure.com/")));
        assertEquals("", UrlHelper.getAccountFromOrganizationUri( URI.create("https://msft.azure.com")));
    }

    @Test
    public void testHaveSameAccount() {
        assertEquals(true, UrlHelper.haveSameAccount( URI.create("https://azure.com/account/blah"), URI.create("https://azure.com/account/blah2")));
        assertEquals(false, UrlHelper.haveSameAccount( URI.create("https://azure.com/account1/blah"), URI.create("https://azure.com/account2/blah2")));
        assertEquals(true, UrlHelper.haveSameAccount( URI.create("http://localhost:8080/test"), URI.create("http://localhost:8080/test2")));
        assertEquals(false, UrlHelper.haveSameAccount( URI.create("http://localhost:8080/test"), URI.create("http://localhost:5080/test2")));
        assertEquals(true, UrlHelper.haveSameAccount( URI.create("https://AZURE.COM/account/blah"), URI.create("https://azure.com/account/blah2")));
        assertEquals(true, UrlHelper.haveSameAccount( URI.create("http://LOCALHOST:8080/test"), URI.create("http://localhost:8080/test2")));
    }
}
