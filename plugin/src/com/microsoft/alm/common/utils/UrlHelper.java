// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.common.utils;

import com.microsoft.alm.common.artifact.GitRefArtifactID;
import com.microsoft.alm.helpers.UriHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class UrlHelper {
    private static final Logger logger = LoggerFactory.getLogger(UrlHelper.class);

    public static final String URL_SEPARATOR = "/";

    // this formatter assumes the entire text is a hyperlink, works for short messages
    public static final String SHORT_HTTP_LINK_FORMATTER = "<html><body><a href=\"%s\">%s</a></body></html>";

    public static final String HOST_VSO = "visualstudio.com";
    public static final String HOST_TFS_ALL_IN = "tfsallin.net"; //azure test subscriptions
    public static final String HOST_AZURE = "azure.com";
    public static final String HOST_AZURE_ORG = ".azure.com";
    public static final String DEFAULT_COLLECTION = "DefaultCollection";

    private static final String URL_GIT_PATH_SEGMENT = "_git";
    private static final String URL_BUILD_PATH_SEGMENT = "_build";
    private static final String URL_BUILD_SPECIFIC_ITEM_PATH_SEGMENT = "?buildid=%d&_a=summary";
    private static final String URL_BUILD_ASPX_SEGMENT = "web/build.aspx?pcguid=%s";
    private static final String URL_BUILD_TEAM_PROJECT_SEGMENT = "&projectname=%s";
    private static final String URL_BUILD_DEFINITION_ID_SEGMENT = "&definitionid=%d";
    private static final String URL_BUILD_QUEUE_ACTION = "&action=queuebuild";
    private static final String URL_WIT_PATH_SEGMENT = "_workitems";
    private static final String URL_WIT_REF_SEGMENT = "_apis/wit/workItems";
    private static final String URL_OPTIMIZED_REF_PATH_SEGMENT = "_optimized";
    private static final String URL_FULL_REF_PATH_SEGMENT = "_full";
    private static final String URL_WIT_SPECIFIC_ITEM_PATH_SEGMENT = "?id=%d&_a=edit";
    private static final String URL_BRANCH_SEGMENT = "?path=%2F&_a=contents&version=GB";
    private static final String URL_COMMIT_SEGMENT = "commit";
    protected static final String URL_PATH_SEGMENT = "#path=";
    protected static final String URL_GIT_VERSION_SEGMENT = "&version=GB";
    protected static final String URL_TFVC_PATH_SEGMENT = "_versionControl";
    protected static final String URL_TFVC_ANNOTATE_FILE_SEGMENT = "?path=%s&_a=contents&annotate=true&hideComments=true";
    protected static final String URL_TFVC_CHANGESET_SEGMENT = "changeset";

    private static final String HTTP_PROTOCOL = "http";
    private static final String HTTPS_PROTOCOL = "https";
    private static final String DEFAULT_TEAM_SERVICES_LINK = "https://www.visualstudio.com/team-services/";

    public static URI createUri(final String url) {
        return URI.create(getCmdLineFriendlyUrl(url));
    }

    public static boolean isValidUrl(final String serverUrl) {
        try {
            new URL(serverUrl);

            return true;
        } catch (MalformedURLException e) {
            //URL is not in a valid form
            logger.warn(serverUrl, e);
        }
        return false;
    }

    public static String asString(final URI uri) {
        if (uri != null) {
            return uri.toString();
        }

        return null;
    }

    public static String trimTrailingSeparators(final String uri) {
        if (StringUtils.isNotEmpty(uri)) {
            int lastIndex = uri.length();
            while (lastIndex > 0 && uri.charAt(lastIndex - 1) == URL_SEPARATOR.charAt(0)) {
                lastIndex--;
            }
            if (lastIndex >= 0) {
                return uri.substring(0, lastIndex);
            }
        }

        return uri;
    }

    public static String trimLeadingAndTrailingSeparators(final String uri) {
        if (StringUtils.isNotEmpty(uri)) {
            int startingIndex = 0;
            while (startingIndex < uri.length() && uri.charAt(startingIndex) == URL_SEPARATOR.charAt(0)) {
                startingIndex++;
            }
            int lastIndex = uri.length();
            while (lastIndex > 0 && uri.charAt(lastIndex - 1) == URL_SEPARATOR.charAt(0)) {
                lastIndex--;
            }
            if (startingIndex < uri.length() && lastIndex >= 0 && startingIndex < lastIndex) {
                return uri.substring(startingIndex, lastIndex);
            }
            return StringUtils.EMPTY;
        }

        return uri;
    }

    /**
     * Removes any user information from an URL, e.g. will remove the "username@" part from the URL
     * "https://username@dev.azure.com/".
     */
    public static String removeUserInfo(final String url) {
        if (url == null) {
            return null;
        }

        try {
            return new URIBuilder(url).setUserInfo(null).build().toString();
        } catch (URISyntaxException e) {
            logger.warn("Invalid URL passed to removeUserInfo: {}", url);
            return url;
        }
    }

    @Nullable
    public static URI createOrganizationUri(@NotNull String host, @NotNull String organization) {
        URI uri = URI.create("https://" + host);
        if (!UriHelper.isAzureHost(uri)) {
            logger.error("Non-azure host passed to convertToOrganizationUrl: {}", host);
            return null;
        }

        return URI.create("https://" + host + "/" + organization);
    }

    public static boolean isVSO(final URI uri) {
        if (uri != null && uri.getHost() != null) {
            final String host = uri.getHost().toLowerCase();
            if (StringUtils.endsWith(host, HOST_VSO) ||
                    StringUtils.endsWith(host, HOST_TFS_ALL_IN) ||
                    UrlHelper.isOrganizationHost(host)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isOrganizationUrl(final String url) {
        try {
            URI uri = createUri(url);
            return isOrganizationURI(uri);
        }
        catch(IllegalArgumentException ex) {
            logger.debug(url, ex);
        }
        return false;
    }

    public static boolean isOrganizationURI(final URI uri) {
        if (uri != null && uri.getHost() != null) {
            return isOrganizationHost(uri.getHost());
        }
        return false;
    }

    public static boolean isOrganizationHost(final String host) {
       if (StringUtils.equalsIgnoreCase(host, HOST_AZURE) ||
               StringUtils.endsWithIgnoreCase(host, HOST_AZURE_ORG)) {
            return true;
       }
       return false;
    }

    public static String getAccountFromOrganization(final String url){
        try {
            URI uri = UrlHelper.createUri(url);
            return getAccountFromOrganizationUri(uri);
        }
        catch(IllegalArgumentException ex) {
            logger.debug(url, ex);
        }
        return null;
    }

    public static String getAccountFromOrganizationUri(final URI uri) {
        // Organization should be in the form azure.com/account1, we ignore any other parameters
        String[] pathSegments = HttpGitUrlParser.getPathSegments(uri);
        if (pathSegments.length > 0) {
            return pathSegments[0];
        }
        else {
            logger.debug("getAccountFromOrganizationUri: expected path for organization account = " + uri);
        }
        return null;
    }

    public static boolean isTeamServicesUrl(final String url) {
        if (StringUtils.containsIgnoreCase(url, HOST_VSO) ||
                StringUtils.containsIgnoreCase(url, HOST_TFS_ALL_IN) ||
                UrlHelper.isOrganizationUrl(url)) {
            return true;
        }
        return false;
    }

    public static boolean isGitRemoteUrl(final String gitRemoteUrl) {
        return StringUtils.contains(gitRemoteUrl, "/_git/");
    }

    public static boolean isSshGitRemoteUrl(final String gitRemoteUrl) {
        if (isGitRemoteUrl(gitRemoteUrl)) {
            if (StringUtils.startsWithIgnoreCase(gitRemoteUrl, "https://") ||
                    StringUtils.startsWithIgnoreCase(gitRemoteUrl, "http://")) {
                return false;
            }

            if (StringUtils.startsWithIgnoreCase(gitRemoteUrl, "ssh://")) {
                return true;
            }

            // check for @ in url - team project name, repo name, collection name and account name don't allow @
            // E.g of valid url formats:
            // ssh://account@organization.visualstudio.com:22/Collection/_git/Repo
            // account@organization.visualstudio.com:22/Collection/_git/Repo
            if (StringUtils.contains(gitRemoteUrl, "@")) {
                return true;
            }
        }
        return false;
    }

    public static String getHttpsUrlFromHttpUrl(final String httpUrl) {
        final URI uri = createUri(httpUrl);
        String httpsUrl = httpUrl;
        if (uri != null && StringUtils.equalsIgnoreCase(uri.getScheme(), "http")) {
            final URI httpsUri = createUri("https://" + uri.getAuthority() + uri.getPath());
            httpsUrl = httpsUri.toString();
        }

        if (StringUtils.startsWithIgnoreCase(httpsUrl, "https://")) {
            return httpsUrl;
        } else {
            return null;
        }
    }

    public static String getHttpsGitUrlFromSshUrl(final String sshGitRemoteUrl) {

        if (isSshGitRemoteUrl(sshGitRemoteUrl)) {
            final URI sshUrl;
            if (!StringUtils.startsWithIgnoreCase(sshGitRemoteUrl, "ssh://")) {
                sshUrl = UrlHelper.createUri("ssh://" + sshGitRemoteUrl);
            } else {
                sshUrl = UrlHelper.createUri(sshGitRemoteUrl);
            }
            final String host = sshUrl.getHost();
            final String path = sshUrl.getPath();
            final URI httpsUrl = UrlHelper.createUri("https://" + host + path);
            return httpsUrl.toString();
        } else if (StringUtils.startsWithIgnoreCase(sshGitRemoteUrl, HTTP_PROTOCOL)
                || StringUtils.startsWithIgnoreCase(sshGitRemoteUrl, HTTPS_PROTOCOL)) {
            // http/https url so return the url untouched
            return sshGitRemoteUrl;
        } else {
            // unsure what this url is
            logger.warn("getHttpsGitUrlFromSshUrl: can't determine https url from " + sshGitRemoteUrl);
            return null;
        }
    }

    public static String getCmdLineFriendlyUrl(final String url) {
        return StringUtils.replace(url, " ", "%20");
    }

    public static URI getVSOAccountURI(final String accountName) {
        return UrlHelper.createUri("https://" + accountName + "." + HOST_VSO); //TODO: how to get account url correctly?
    }

    public static URI getCollectionURI(final URI serverUri, final String collectionName) {
        if (isOrganizationURI(serverUri) ||
                isVSO(serverUri) && getVSOAccountURI(collectionName).equals(serverUri)) {
            // Either we are using an organization where the collection is the same as the server uri
            // or collection in the domain case on VSTS on the old style visualstudio.com domain
            return serverUri;
        }
        return UrlHelper.createUri(combine(serverUri.toString(), collectionName));
    }

    public static URI getTeamProjectURI(final URI serverUri, final String collectionName, final String teamProjectName) {
        return UrlHelper.createUri(combine(getCollectionURI(serverUri, collectionName).toString(), teamProjectName));
    }

    public static URI getBuildsPageURI(final URI projectUri) {
        return UrlHelper.createUri(combine(projectUri.toString(), URL_BUILD_PATH_SEGMENT));
    }

    public static URI getBuildURI(final URI projectUri, final int buildId) {
        return UrlHelper.createUri(combine(projectUri.toString(), URL_BUILD_PATH_SEGMENT)
                .concat(String.format(URL_BUILD_SPECIFIC_ITEM_PATH_SEGMENT, buildId)));
    }

    public static URI getQueueBuildURI(final URI serverUri, final String collectionId, final String projectName, final int buildDefinitionId) {
        return UrlHelper.createUri(
                combine(serverUri.toString(), String.format(URL_BUILD_ASPX_SEGMENT, collectionId))
                        .concat(String.format(URL_BUILD_TEAM_PROJECT_SEGMENT, encode(projectName)))
                        .concat(String.format(URL_BUILD_DEFINITION_ID_SEGMENT, buildDefinitionId))
                        .concat(URL_BUILD_QUEUE_ACTION));
    }

    public static URI getCreateWorkItemURI(final URI projectUri) {
        //TODO: this url isn't exactly correct because we don't know the WI Type to create
        return UrlHelper.createUri(combine(projectUri.toString(), URL_WIT_PATH_SEGMENT));
    }

    public static URI getSpecificWorkItemURI(final URI projectUri, final int workItemId) {
        return UrlHelper.createUri(combine(projectUri.toString(), URL_WIT_PATH_SEGMENT)
                .concat(String.format(URL_WIT_SPECIFIC_ITEM_PATH_SEGMENT, workItemId)));
    }

    public static URI getWorkItemRefURI(final URI teamProjectUri, final String workItemId) {
        return UrlHelper.createUri(combine(teamProjectUri.toString(), URL_WIT_REF_SEGMENT, workItemId));
    }

    public static URI getMyWorkItemsURI(final URI projectUri) {
        // The default query when you navigate to the work items section is the "Assigned to me" query results
        return UrlHelper.createUri(combine(projectUri.toString(), URL_WIT_PATH_SEGMENT));
    }

    public static URI getBranchURI(final URI repoUri, final String branchName) {
        return UrlHelper.createUri(repoUri.toString().concat(URL_BRANCH_SEGMENT).concat(encode(branchName)));
    }

    public static URI getCommitURI(final String remoteUrl, final String commitId) {
        return UrlHelper.createUri(combine(getHttpsGitUrlFromSshUrl(remoteUrl), URL_COMMIT_SEGMENT, commitId));
    }

    public static URI getFileURI(final String remoteUrl, final String filePath, final String gitRemoteBranchName) {
        String uri = getHttpsGitUrlFromSshUrl(remoteUrl).concat(URL_PATH_SEGMENT).concat(filePath);

        if (uri == null) {
            //could not determine uri so open default link
            return UrlHelper.createUri(DEFAULT_TEAM_SERVICES_LINK);
        }

        if (StringUtils.isNotEmpty(gitRemoteBranchName)) {
            uri = uri.concat(URL_GIT_VERSION_SEGMENT).concat(gitRemoteBranchName);
        }
        return UrlHelper.createUri(uri);
    }

    public static URI getTfvcChangesetURI(final String remoteUrl, final String changesetNumber) {
        return UrlHelper.createUri(combine(getHttpsGitUrlFromSshUrl(remoteUrl), URL_TFVC_PATH_SEGMENT,
                URL_TFVC_CHANGESET_SEGMENT, changesetNumber));
    }

    public static URI getTfvcAnnotateURI(final String collectionUrl, final String projectName, final String filePath) {
        return createUri(combine(collectionUrl, projectName, URL_TFVC_PATH_SEGMENT,
                String.format(URL_TFVC_ANNOTATE_FILE_SEGMENT, encode(filePath))));
    }

    /*
     * This is the portion that must match in order to use the same PAT
     */
    public static boolean haveSameAccount(final URI remoteUrl1, final URI remoteUrl2) {
        if (remoteUrl1 != null && remoteUrl2 != null) {
            return StringUtils.equalsIgnoreCase(getServerAccountKey(remoteUrl1), getServerAccountKey(remoteUrl2));
        }

        return false;
    }

    public static String encode(String urlParameter) {
        try {
            return URLEncoder.encode(urlParameter, "UTF-8");
        } catch (final UnsupportedEncodingException ex) {
            /*
             * we should never get here (UTF-8 URL encoding is the recommended
             * encoding and should be supported on all platforms), so convert
             * into a runtime exception and throw
             */
            throw new RuntimeException(ex);
        }
    }

    public static String decode(String urlParameter) {
        try {
            return URLDecoder.decode(urlParameter, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            /*
             * we should never get here (UTF-8 URL encoding is the recommended
             * encoding and should be supported on all platforms), so convert
             * into a runtime exception and throw
             */
            throw new RuntimeException(ex);
        }
    }

    public static String combine(String... urlParts) {
        StringBuilder sb = new StringBuilder();
        if (urlParts != null) {
            for (String part : urlParts) {
                if (StringUtils.isEmpty(part)) {
                    continue;
                }

                if (sb.length() > 0) {
                    sb.append(URL_SEPARATOR);
                }

                sb.append(trimLeadingAndTrailingSeparators(part));
            }
        }
        return sb.toString();
    }

    /**
     * Parse URI for branch name
     *
     * @param uri ex: vstfs:///Git/Ref/ProjectId%2FRepoId%2FGB<BranchName>
     * @return branch name
     */
    public static String parseUriForBranch(final String uri) {
        if (!StringUtils.isEmpty(uri)) {
            GitRefArtifactID artifactID = new GitRefArtifactID(uri);
            return artifactID.getRefName();
        } else {
            return StringUtils.EMPTY;
        }
    }

    /**
     * This method returns the account name based on the VSO url provided.
     *
     * @param vsoUri the VSO url to parse
     * @return the string after the "http://" and before the first "/".
     */
    public static String getVSOAccountName(final URI vsoUri) {
        final String[] hostParts = vsoUri.getHost().split("\\.");
        final String accountName = hostParts.length > 0 ? hostParts[0] : "";
        return accountName;
    }

    public static URI resolveEndpointUri(URI baseUri, String endpointPath) {
        if (!baseUri.getPath().endsWith("/")) {
            baseUri = createUri(baseUri.toString() + "/");
        }

        if (endpointPath.startsWith("/")) {
            endpointPath = endpointPath.substring(1);
        }

        return baseUri.resolve(endpointPath);
    }

    /**
     * Given a git url, try parse the collection and project name out of this url
     * <p/>
     * valid schema:
     * schema://host[:port][/IIS-path]/collection[/project]/_git[/(_optimized|_full)]/repository[/]
     * <p/>
     * name restrictions for TFS:
     * https://go.microsoft.com/fwlink/?LinkId=77936
     *
     * @param gitUrl    git fetch/push url
     * @param validator a provided method to validate the validity of the parsed result
     *                  this abstracts the authentication requirement away from this utility
     * @return a parseResult,
     * parsed and validated result. If parsing isn't successful, every field is null
     */
    public static ParseResult tryParse(final String gitUrl, final ParseResultValidator validator) {
        if (StringUtils.isEmpty(gitUrl)) {
            return ParseResult.FAILED;
        }

        final URI gitUri;
        try {
            gitUri = createUri(gitUrl);
        } catch (Throwable t) {
            logger.warn("tryParse: creating Uri failed for Git url: {}", gitUrl, t);
            return ParseResult.FAILED;
        }

        // only support http and https (ssh support will come later when the format of the url is better understood)
        try {
            final String scheme = gitUri.getScheme() != null ? gitUri.getScheme().toLowerCase() : null;
            if (HTTPS_PROTOCOL.equals(scheme) || HTTP_PROTOCOL.equals(scheme)) {
                return HttpGitUrlParser.tryParse(gitUri, validator);
            }
        } catch (Throwable t) {
            logger.error("tryParse: unexpected error for gitUrl = " + gitUrl);
            logger.warn("tryParse", t);
        }

        return ParseResult.FAILED;
    }

    public static class ParseResult {
        public static final ParseResult FAILED = new ParseResult(null, null, null, null, null, null);

        private final String accountName;
        private final String serverUrl;
        private final String collectionUrl;
        private final String collectionName;
        private final String projectName;
        private final String repoName;

        // A ParseResult.Builder might make a lot of sense here if we really want this ctor to be public.
        public ParseResult(final String serverUrl, final String collectionUrl, final String collectionName,
                           final String projectName, final String repoName, final String accountName) {
            this.serverUrl = serverUrl;
            this.collectionUrl = collectionUrl;
            this.collectionName = collectionName;
            this.projectName = projectName;
            this.repoName = repoName;
            this.accountName = accountName;
        }

        public boolean isSuccess() {
            return this != FAILED;
        }

        public boolean isVSO() {
            return UrlHelper.isVSO(createUri(this.serverUrl));
        }

        public String getServerUrl() {
            return serverUrl;
        }

        public String getCollectionUrl() {
            return collectionUrl;
        }

        public String getCollectionName() {
            return collectionName;
        }

        public String getProjectName() {
            return projectName;
        }

        public String getRepoName() {
            return repoName;
        }

        public String getAccountName() {
            return accountName;
        }

        @Override
        public String toString() {
            return "ServerUrl = " + serverUrl + " " +
                    "CollectionUrl = " + collectionUrl + " " +
                    "CollectionName = " + collectionName + " " +
                    "ProjectName = " + projectName + " " +
                    "RepoName = " + repoName + " " +
                    "AccountName = " + accountName;
        }
    }

    private static String getServerAccountKey(final URI uri) {
        return UrlHelper.isOrganizationURI(uri)
                ? uri.getAuthority() + '\\' + getAccountFromOrganizationUri(uri)
                : uri.getAuthority();
    }

    private static class HttpGitUrlParser {

        private static ParseResult tryParse(final URI gitUri, final ParseResultValidator validator) {
            // not all valid uri is valid http url
            if (!isValidUrl(gitUri.toString())) {
                return ParseResult.FAILED;
            }

            final String[] pathSegments = getPathSegments(gitUri);
            if (!isPathSegmentValid(pathSegments)) {
                return ParseResult.FAILED;
            }

            // first assuming no "project" in git url, this is true for the default/first repository in a project
            // there is no "project" path segment, collection url is all the way upto _git, and project name == repo name
            final ParseResult result = buildParseResult(gitUri, pathSegments, false);
            if (validator.validate(result)) {
                return result;
            }

            // so first attempt of no "project" segment failed, repo is not the same as project, try the explicit project name at
            // the third to last segment
            final ParseResult alternateResult = buildParseResult(gitUri, pathSegments, true);
            if (validator.validate(alternateResult)) {
                return alternateResult;
            }

            return ParseResult.FAILED;
        }

        private static String[] getPathSegments(final URI gitUri) {
            //strip leading and trailing slashes
            final String gitUrlPath = gitUri.normalize().getPath()
                    .replaceAll("/$", "")
                    .replaceAll("^/", "");

            return gitUrlPath.split("/");
        }

        private static boolean isPathSegmentValid(final String[] segments) {
            // must have "collection", "_git", and "repository" at a minimum
            if (segments == null || segments.length < 3) {
                return false;
            }

            // if "_git" is the n-1 segment for a regular url, it's good
            final String secondToLastSegment = segments[segments.length - 2];
            if (URL_GIT_PATH_SEGMENT.equals(secondToLastSegment)) {
                return true;
            }

            // or "_git" must be the n-2 segment for a limited ref url, and n-1 segment is either "_optimized" or "_full"
            final String thirdToLastSegment = segments[segments.length - 3];
            if (URL_GIT_PATH_SEGMENT.equals(thirdToLastSegment)
                    && (URL_OPTIMIZED_REF_PATH_SEGMENT.equals(secondToLastSegment) || URL_FULL_REF_PATH_SEGMENT.equals(secondToLastSegment))) {
                return true;
            }

            return false;
        }

        private static int getGitPathSegmentPosition(final String[] segments) {
            /* assuming the array passed in is wellformed, i.e. should have passed the isPathSegmentValid test */
            final String secondToLastSegment = segments[segments.length - 2];
            if (URL_GIT_PATH_SEGMENT.equals(secondToLastSegment)) {
                return segments.length - 2;
            }

            final String thirdToLastSegment = segments[segments.length - 3];
            if (URL_GIT_PATH_SEGMENT.equals(thirdToLastSegment)) {
                return segments.length - 3;
            }

            // error position
            return -1;
        }

        private static ParseResult buildParseResult(final URI gitUri, final String[] pathSegments,
                                                    final boolean explicitProjectName) {
            // if this is vso then the account name is the very first part of the url
            final String accountName = UrlHelper.getVSOAccountName(gitUri);

            // carry over scheme and authority (host+port)
            final StringBuilder urlBuilder = new StringBuilder(gitUri.getScheme()).append("://")
                    .append(gitUri.getAuthority()).append("/");

            final int gitSegmentPos = getGitPathSegmentPosition(pathSegments);
            final int collectionSegmentPos = explicitProjectName ? gitSegmentPos - 2 : gitSegmentPos - 1;

            // Add all segments before the collection segment to collectionUrlBuilder
            for (int i = 0; i < collectionSegmentPos; ++i) {
                urlBuilder.append(pathSegments[i]).append("/");
            }
            final String serverUrl = urlBuilder.toString();

            // now add the collection name
            final String collectionName = pathSegments[collectionSegmentPos];
            urlBuilder.append(collectionName).append("/");
            final String collectionUrl = urlBuilder.toString();

            final int size = pathSegments.length;

            // repo is always the last segment
            final String repositoryName = pathSegments[size - 1];
            final String projectName = explicitProjectName ? pathSegments[collectionSegmentPos + 1] : repositoryName;

            return new ParseResult(serverUrl, collectionUrl, collectionName, projectName, repositoryName, accountName);
        }
    }

    public interface ParseResultValidator {
        boolean validate(final ParseResult parseResult);
    }
}
