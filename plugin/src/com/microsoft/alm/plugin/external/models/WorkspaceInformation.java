package com.microsoft.alm.plugin.external.models;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

/**
 * A piece of information about workspace, either basic or detailed.
 */
public class WorkspaceInformation {
    public static class BasicInformation {
        private final String workspaceName;
        private final URI collectionUri;

        public BasicInformation(String workspaceName, URI collectionUri) {
            this.workspaceName = workspaceName;
            this.collectionUri = collectionUri;
        }

        public String getWorkspaceName() {
            return workspaceName;
        }

        public URI getCollectionUri() {
            return collectionUri;
        }
    }

    private final BasicInformation basic;
    private final Workspace detailed;

    private WorkspaceInformation(BasicInformation basic, Workspace detailed) {
        this.basic = basic;
        this.detailed = detailed;
    }

    @NotNull
    public static WorkspaceInformation basic(@NotNull BasicInformation basic) {
        return new WorkspaceInformation(basic, null);
    }

    @NotNull
    public static WorkspaceInformation detailed(@NotNull Workspace detailed) {
        return new WorkspaceInformation(null, detailed);
    }

    @Nullable
    public BasicInformation getBasic() {
        return basic;
    }

    @Nullable
    public Workspace getDetailed() {
        return detailed;
    }
}
