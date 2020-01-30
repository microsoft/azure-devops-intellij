// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.exceptions;

import com.microsoft.alm.plugin.external.models.ToolVersion;

public class VisualStudioClientVersionException extends ToolException {
    final ToolVersion versionFound;
    final ToolVersion minimumVersion;

    public VisualStudioClientVersionException(final ToolVersion versionFound, final ToolVersion minimumVersion) {
        super(ToolException.KEY_TF_VS_MIN_VERSION_WARNING);
        this.versionFound = versionFound;
        this.minimumVersion = minimumVersion;
    }

    @Override
    public String[] getMessageParameters() {
        return new String[] {versionFound.toString(), minimumVersion.toString()};
    }

}
