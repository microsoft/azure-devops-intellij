package org.jetbrains.tfsIntegration.core.tfs;

import org.jetbrains.tfsIntegration.core.tfs.version.VersionSpecBase;

public class UpdateWorkspaceInfo {
    public UpdateWorkspaceInfo(VersionSpecBase myVersion) {
        this.myVersion = myVersion;
    }

    public VersionSpecBase getVersion() {
        return myVersion;
    }

    public void setVersion(VersionSpecBase myVersion) {
        this.myVersion = myVersion;
    }

    private VersionSpecBase myVersion;
}
