// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.common.artifact;

import org.junit.Assert;
import org.junit.Test;

public class GitRefArtifactIDTest {
    private final String TOOL = "Git";
    private final String ARTIFACT_TYPE = "Ref";
    private final String PROJECT_ID = "00000000-0000-0000-0000-000000000000";
    private final String REPO_ID = "11111111-1111-1111-1111-111111111111";
    private final String REF_NAME = "MyBranch1";
    private final String TOOL_SPECIFIC_ID = PROJECT_ID + ArtifactID.URI_SEPARATOR + REPO_ID + ArtifactID.URI_SEPARATOR + "GB" + REF_NAME;
    private final String TOOL_SPECIFIC_ID_ENCODED = PROJECT_ID + "%2F" + REPO_ID + "%2FGB" + REF_NAME;
    private final String ENCODED_URI = ArtifactID.VSTFS_PREFIX + TOOL + ArtifactID.URI_SEPARATOR + ARTIFACT_TYPE + ArtifactID.URI_SEPARATOR + TOOL_SPECIFIC_ID_ENCODED;

    @Test
    public void testConstructorDecodeGitRefToolSpecifiedId_SimpleBranchName() {
        GitRefArtifactID artifact = new GitRefArtifactID(ENCODED_URI);
        Assert.assertEquals(TOOL, artifact.getTool());
        Assert.assertEquals(ARTIFACT_TYPE, artifact.getArtifactType());
        Assert.assertEquals(TOOL_SPECIFIC_ID, artifact.getToolSpecificID());
        Assert.assertEquals(REPO_ID, artifact.getRepoId());
        Assert.assertEquals(PROJECT_ID, artifact.getProjectId());
        Assert.assertEquals(REF_NAME, artifact.getRefName());
    }

    @Test
    public void testConstructorDecodeGitRefToolSpecifiedId_BranchNameWithSeparator() {
        GitRefArtifactID artifact = new GitRefArtifactID(ENCODED_URI + "%2F" + "BranchNameContinued");
        Assert.assertEquals(TOOL, artifact.getTool());
        Assert.assertEquals(ARTIFACT_TYPE, artifact.getArtifactType());
        Assert.assertEquals(TOOL_SPECIFIC_ID + ArtifactID.URI_SEPARATOR + "BranchNameContinued", artifact.getToolSpecificID());
        Assert.assertEquals(REPO_ID, artifact.getRepoId());
        Assert.assertEquals(PROJECT_ID, artifact.getProjectId());
        Assert.assertEquals(REF_NAME + ArtifactID.URI_SEPARATOR + "BranchNameContinued", artifact.getRefName());
    }
}
