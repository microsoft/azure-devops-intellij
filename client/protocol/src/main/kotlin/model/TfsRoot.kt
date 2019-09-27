package model.tfs

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.PredefinedType.*

object TfsRoot : Root() {
    private val VersionNumber = structdef {
        field("major", int)
        field("minor", int)
    }

    private val TfsLocalPath = structdef {
        field("path", string)
    }

    private val TfsServerStatusType = enum {
        +"ADD"
        +"RENAME"
        +"EDIT"
        +"DELETE"
        +"UNDELETE"
        +"LOCK"
        +"BRANCH"
        +"MERGE"
        +"UNKNOWN"
    }

    private val TfsPendingChange = structdef {
        field("serverItem", string)
        field("localItem", string)
        field("version", int)
        field("owner", string)
        field("date", string)
        field("lock", string)
        field("changeTypes", immutableList(TfsServerStatusType))
        field("workspace", string)
        field("computer", string)
        field("isCandidate", bool)
        field("sourceItem", string.nullable)
    }

    private val TfsCredentials = structdef {
        field("login", string)
        field("password", string)
    }

    private val TfsWorkspaceDefinition = structdef {
        field("localPath", TfsLocalPath)
        field("credentials", TfsCredentials)
    }

    private val TfsWorkspace = classdef {
        property("isReady", bool)
            .doc("Whether the workspace is ready to accept the method calls")

        call("getPendingChanges", immutableList(TfsLocalPath), immutableList(TfsPendingChange))
            .doc("Determines a set of the pending changes in the workspace")
    }

    init {
        property("version", VersionNumber)

        call("healthCheck", void, string.nullable)
            .doc("Performs a health check, returns optional error message")

        signal("shutdown", void)
            .doc("Shuts down the application")

        map("workspaces", TfsWorkspaceDefinition, TfsWorkspace)
    }
}