// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.common.artifact;

import com.microsoft.alm.common.exceptions.MalformedArtifactIDException;
import com.microsoft.alm.common.exceptions.MalformedURIException;
import org.apache.commons.lang.StringUtils;

/**
 * An artifact ID specifically for Git artifacts.
 * <p/>
 * The toolSpecifiedId can be split up into project id, repo id, and ref name
 * ex: vstfs:///tool/artifact-type/tool-specific-identifier
 */
public class GitRefArtifactID extends ArtifactID {
    private String projectId;
    private String repoId;
    private String refName;

    public GitRefArtifactID(final String uri) {
        super(uri);
        decodeGitRefToolSpecifiedId(toolSpecificId);
    }

    /**
     * Decode the ToolSpecifiedId into its 3 separate components
     *
     * @param toolSpecifiedId
     */
    private void decodeGitRefToolSpecifiedId(final String toolSpecifiedId) {
        if (StringUtils.isEmpty(toolSpecifiedId)) {
            throw new MalformedArtifactIDException(this);
        }

        final String[] components = toolSpecifiedId.split(URI_SEPARATOR);
        if (components.length < 3) {
            throw new MalformedURIException(String.format("The toolSpecifiedId was not able to be decoded: %s", toolSpecifiedId));
        }

        projectId = components[0];
        repoId = components[1];
        refName = components[2];

        // for cases where a ref name contains a forward slash
        if (components.length > 3) {
            for (int index = 3; index < components.length; index++) {
                refName = refName + URI_SEPARATOR + components[index];
            }
        }

        refName = decodeRefName(refName);
    }

    /**
     * Removes the encoding from the ref name. The encoding is just a 2 char prefix
     * Ex: GBBranchName
     *
     * @param refName
     * @return
     */
    private String decodeRefName(final String refName) {
        return refName.substring(2);
    }

    public String getRefName() {
        return refName;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getRepoId() {
        return repoId;
    }
}
