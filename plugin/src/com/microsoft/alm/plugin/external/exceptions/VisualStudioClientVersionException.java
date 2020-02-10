// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.exceptions;

import com.microsoft.alm.plugin.external.models.ToolVersion;
import org.jetbrains.annotations.NotNull;

public class VisualStudioClientVersionException extends ToolException {
    @NotNull final ToolVersion versionFound;
    @NotNull final ToolVersion minimumVersion;
    @NotNull final String minimalVersionNickname;

    public VisualStudioClientVersionException(
            @NotNull ToolVersion versionFound,
            @NotNull ToolVersion minimumVersion,
            @NotNull String minimalVersionNickname) {
        super(ToolException.KEY_TF_VS_MIN_VERSION_WARNING);
        this.versionFound = versionFound;
        this.minimumVersion = minimumVersion;
        this.minimalVersionNickname = minimalVersionNickname;
    }

    @Override
    public String[] getMessageParameters() {
        return new String[] {versionFound.toString(), minimumVersion.toString(), minimalVersionNickname};
    }
}
