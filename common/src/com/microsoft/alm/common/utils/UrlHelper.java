// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.common.utils;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class UrlHelper {
    private static final Logger logger = LoggerFactory.getLogger(UrlHelper.class);

    public static final String URL_SEPARATOR = "/";

    // this formatter assumes the entire text is a hyperlink, works for short messages
    public static final String SHORT_HTTP_LINK_FORMATTER = "<html><body><a href=\"%s\">%s</a></body></html>";

    private static final String HOST_VSO = "visualstudio.com";
    private static final String HOST_TFS_ALL_IN = "tfsallin.com";

    private static final String URL_GIT_PATH_SEGMENT = "_git";
    private static final String URL_OPTIMIZED_REF_PATH_SEGMENT = "_optimized";
    private static final String URL_FULL_REF_PATH_SEGMENT = "_full";

    private static final String HTTP_PROTOCOL = "http";
    private static final String HTTPS_PROTOCOL = "https";


    //TODO: how much validation should we do, should we handle other exceptions here?
    public static boolean isValidServerUrl(final String serverUrl) {
        try {
            new URL(serverUrl);

            return true;
        } catch (MalformedURLException e) {
            //URL is not in a valid form
            logger.warn(serverUrl, e);
        }
        return false;
    }

    public static URI getBaseUri(final String uri) {
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            logger.warn(uri, e);
        }
        return null;
    }

    public static String asString(final URI uri) {
        if (uri != null) {
            return uri.toString();
        }

        return null;
    }

    public static boolean isVSO(final URI uri) {
        final String host = uri.getHost().toLowerCase();
        if (host.endsWith(HOST_VSO)) {
            return true;
        } else if (host.endsWith(HOST_TFS_ALL_IN)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isGitRemoteUrl(final String gitRemoteUrl) {
        return StringUtils.contains(gitRemoteUrl, "/_git/");
    }

    public static URI getVSOAccountURI(final String accountName) {
        return URI.create("https://" + accountName + "." + HOST_VSO); //TODO: how to get account url correctly?
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
            baseUri = URI.create(baseUri.toString() + "/");
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
     * http://go.microsoft.com/fwlink/?LinkId=77936
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

        final URI gitUri = getBaseUri(gitUrl);
        if (gitUri == null) {
            return ParseResult.FAILED;
        }

        // only support http and https (ssh support will come later when the format of the url is better understood)
        try {
            final String scheme = gitUri.getScheme() != null ? gitUri.getScheme().toLowerCase() : null;
            if (HTTPS_PROTOCOL.equals(scheme) || HTTP_PROTOCOL.equals(scheme)) {
                return HttpGitUrlParser.tryParse(gitUri, validator);
            }
        } catch (Throwable t) {
            logger.error("tryParse: unexpected error");
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
            return UrlHelper.isVSO(UrlHelper.getBaseUri(this.serverUrl));
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
    }

    private static class HttpGitUrlParser {

        private static ParseResult tryParse(final URI gitUri, final ParseResultValidator validator) {
            // not all valid uri is valid http url
            if (!isValidServerUrl(gitUri.toString())) {
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
