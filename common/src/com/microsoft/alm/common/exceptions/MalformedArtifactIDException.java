// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.common.exceptions;

import com.microsoft.alm.common.artifact.ArtifactID;

import java.text.MessageFormat;

/**
 * Thrown when an artifact ID is not in the proper format.
 */
public class MalformedArtifactIDException extends RuntimeException {
    public MalformedArtifactIDException(final ArtifactID id) {
        super(MessageFormat.format(
                "Malformed artifact id: tool=[{0}] artifactType=[{1}] toolSpecificId=[{2}]",
                id.getTool(),
                id.getArtifactType(),
                id.getToolSpecificID()));
    }
}
