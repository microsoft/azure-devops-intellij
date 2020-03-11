// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package model.tfs

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.PredefinedType.*

@Suppress("unused")
object TfsModel : Root() {
    private val TfsPath = basestruct {}

    private val TfsLocalPath = structdef extends TfsPath {
        field("path", string)
    }

    private val TfsServerPath = structdef extends TfsPath {
        field("workspace", string)
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
        field("password", secureString)
    }

    private val TfsCollectionDefinition = structdef {
        field("serverUri", uri)
        field("credentials", TfsCredentials)
    }

    private val TfsCollection = classdef {
        property("isReady", bool)
            .doc("Whether the client is ready to accept method calls")

        property("mappedPaths", immutableList(TfsPath))
            .doc("A list of path mappings for this collection")

        call("getPendingChanges", immutableList(TfsPath), immutableList(TfsPendingChange))
            .doc("Determines a set of the pending changes in the collection")

        call("invalidatePaths", immutableList(TfsLocalPath), void)
            .doc("Invalidates the paths in the TFS cache")

        call("deleteFilesRecursively", immutableList(TfsPath), void)
            .doc("Scheduled deletion of the files")

        call("undoLocalChanges", immutableList(TfsPath), immutableList(TfsLocalPath))
            .doc("Removes pending changes from a workspace, restoring the local disk files to match the state of the source control server before the change was made.")
    }

    init {
        signal("shutdown", void)
            .doc("Shuts down the application")

        map("collections", TfsCollectionDefinition, TfsCollection)
    }
}