// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.exceptions;

import com.microsoft.alm.plugin.external.models.ToolVersion;

public class ToolVersionException extends ToolException {
    final ToolVersion versionFound;
    final ToolVersion minimumVersion;

    public ToolVersionException(final ToolVersion versionFound, final ToolVersion minimumVersion) {
        this(versionFound, minimumVersion, null);
    }

    public ToolVersionException(final ToolVersion versionFound, final ToolVersion minimumVersion, Throwable t) {
        super(ToolException.KEY_TF_MIN_VERSION_WARNING, t);
        this.versionFound = versionFound;
        this.minimumVersion = minimumVersion;
    }

    @Override
    public String[] getMessageParameters() {
        return new String[] {versionFound.toString(), minimumVersion.toString()};
    }

}
