// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.common.artifact;

import com.microsoft.alm.common.exceptions.MalformedURIException;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;

public class ArtifactIDTest {
    private final String TOOL = "Git";
    private final String ARTIFACT_TYPE = "Ref";
    private final String TOOL_SPECIFIC_ID = "00000000-0000-0000-0000-000000000000/00000000-0000-0000-0000-000000000000/GBMyBranch1";
    private final String TOOL_SPECIFIC_ID_ENCODED = "00000000-0000-0000-0000-000000000000%2F00000000-0000-0000-0000-000000000000%2FGBMyBranch1";
    private final String ENCODED_URI = ArtifactID.VSTFS_PREFIX + TOOL + ArtifactID.URI_SEPARATOR + ARTIFACT_TYPE + ArtifactID.URI_SEPARATOR + TOOL_SPECIFIC_ID_ENCODED;

    @Test
    public void testConstructorDecoding_Happy() {
        ArtifactID artifact = new ArtifactID(ENCODED_URI);
        Assert.assertEquals(TOOL, artifact.getTool());
        Assert.assertEquals(ARTIFACT_TYPE, artifact.getArtifactType());
        Assert.assertEquals(TOOL_SPECIFIC_ID, artifact.getToolSpecificID());
    }

    @Test (expected = MalformedURIException.class)
    public void testConstructorDecoding_NullUri() {
        new ArtifactID(null);
    }

    @Test (expected = MalformedURIException.class)
    public void testConstructorDecoding_MissingPrefix() {
        new ArtifactID(ENCODED_URI.replaceFirst(ArtifactID.VSTFS_PREFIX, StringUtils.EMPTY));
    }

    @Test (expected = MalformedURIException.class)
    public void testConstructorDecoding_TooManyComponents() {
        new ArtifactID(ENCODED_URI + ArtifactID.URI_SEPARATOR + TOOL_SPECIFIC_ID_ENCODED);
    }

    @Test (expected = MalformedURIException.class)
    public void testConstructorDecoding_MissingComponents() {
        new ArtifactID(ArtifactID.VSTFS_PREFIX + TOOL + ArtifactID.URI_SEPARATOR + ARTIFACT_TYPE);
    }

    @Test
    public void testCheckUriIsWellFormed_Happy() {
        ArtifactID.checkURIIsWellFormed(ENCODED_URI);
    }

    @Test (expected = MalformedURIException.class)
    public void testCheckUriIsWellFormed_Bad() {
        ArtifactID.checkURIIsWellFormed(StringUtils.EMPTY);
    }

    @Test
    public void testEquals_True() {
        ArtifactID artifact1 = new ArtifactID(TOOL, ARTIFACT_TYPE, TOOL_SPECIFIC_ID);
        ArtifactID artifact2 = new ArtifactID(TOOL, ARTIFACT_TYPE, TOOL_SPECIFIC_ID);
        Assert.assertTrue(artifact1.equals(artifact2));
        Assert.assertEquals(artifact1.hashCode(), artifact2.hashCode());
    }
    @Test
    public void testEquals_False() {
        ArtifactID artifact1 = new ArtifactID(TOOL, ARTIFACT_TYPE, TOOL_SPECIFIC_ID);
        ArtifactID artifact2 = new ArtifactID(TOOL, ARTIFACT_TYPE, TOOL_SPECIFIC_ID_ENCODED);
        Assert.assertFalse(artifact1.equals(artifact2));
        Assert.assertNotEquals(artifact1.hashCode(), artifact2.hashCode());
    }

    @Test
    public void testEncodeUri() {
        ArtifactID artifact = new ArtifactID(TOOL, ARTIFACT_TYPE, TOOL_SPECIFIC_ID);
        Assert.assertEquals(ENCODED_URI, artifact.encodeURI());
    }

    @Test
    public void testIsWellFormed_Happy() {
        ArtifactID artifact = new ArtifactID(TOOL, ARTIFACT_TYPE, TOOL_SPECIFIC_ID);
        Assert.assertTrue(artifact.isWellFormed());
    }

    @Test
    public void testIsWellFormed_BadTool() {
        ArtifactID artifact = new ArtifactID(StringUtils.EMPTY, ARTIFACT_TYPE, TOOL_SPECIFIC_ID);
        Assert.assertFalse(artifact.isWellFormed());

        artifact = new ArtifactID("G/it", ARTIFACT_TYPE, TOOL_SPECIFIC_ID);
        Assert.assertFalse(artifact.isWellFormed());

        artifact = new ArtifactID("G\\it", ARTIFACT_TYPE, TOOL_SPECIFIC_ID);
        Assert.assertFalse(artifact.isWellFormed());

        artifact = new ArtifactID("G.it", ARTIFACT_TYPE, TOOL_SPECIFIC_ID);
        Assert.assertFalse(artifact.isWellFormed());
    }

    @Test
    public void testIsWellFormed_BadArtifactType() {
        ArtifactID artifact = new ArtifactID(TOOL, StringUtils.EMPTY, TOOL_SPECIFIC_ID);
        Assert.assertFalse(artifact.isWellFormed());

        artifact = new ArtifactID(TOOL, "G/it", TOOL_SPECIFIC_ID);
        Assert.assertFalse(artifact.isWellFormed());
    }

    @Test
    public void testIsWellFormed_BadToolSpecificId() {
        ArtifactID artifact = new ArtifactID(TOOL, ARTIFACT_TYPE, StringUtils.EMPTY);
        Assert.assertFalse(artifact.isWellFormed());
    }
}
