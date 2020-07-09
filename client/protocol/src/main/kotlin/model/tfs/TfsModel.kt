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

    private val TfsItemInfo = basestruct {
        field("serverItem", string.nullable)
        field("localItem", string.nullable)
        field("localVersion", int)
        field("serverVersion", int)
        field("change", string.nullable)
        field("type", string.nullable)
        field("lastModified", string.nullable)
        field("fileEncoding", string.nullable)
    }

    private val TfsLocalItemInfo = structdef extends TfsItemInfo {}

    private val TfsExtendedItemInfo = structdef extends TfsItemInfo {
        field("lock", string.nullable)
        field("lockOwner", string.nullable)
    }

    private val TfsDeleteResult = structdef {
        field("deletedPaths", immutableList(TfsLocalPath))
        field("notFoundPaths", immutableList(TfsPath))
        field("errorMessages", immutableList(string))
    }

    private val TfvcCheckoutParameters = structdef {
        field("filePaths", immutableList(TfsLocalPath))
        field("recursive", bool)
    }

    private val TfvcCheckoutResult = structdef {
        field("checkedOutFiles", immutableList(TfsLocalPath))
        field("notFoundFiles", immutableList(TfsLocalPath))
        field("errorMessages", immutableList(string))
    }

    private val TfvcRenameRequest = structdef {
        field("oldPath", TfsLocalPath)
        field("newPath", TfsLocalPath)
    }

    private val TfsCollection = classdef {
        property("isReady", bool)
            .doc("Whether the client is ready to accept method calls")

        property("mappedPaths", immutableList(TfsPath))
            .doc("A list of path mappings for this collection")

        call("getPendingChanges", immutableList(TfsPath), immutableList(TfsPendingChange))
            .doc("Determines a set of the pending changes in the collection")

        call("getLocalItemsInfo", immutableList(TfsLocalPath), immutableList(TfsLocalItemInfo))
            .doc("Provides information on local repository items")

        call("getExtendedLocalItemsInfo", immutableList(TfsLocalPath), immutableList(TfsExtendedItemInfo))
            .doc("Provides extended information (i.e. including locks) on local repository items")

        call("invalidatePaths", immutableList(TfsLocalPath), void)
            .doc("Invalidates the paths in the TFS cache")

        call("deleteFilesRecursively", immutableList(TfsPath), TfsDeleteResult)
            .doc("Scheduled deletion of the files")

        call("undoLocalChanges", immutableList(TfsPath), immutableList(TfsLocalPath))
            .doc("Removes pending changes from a workspace, restoring the local disk files to match the state of the source control server before the change was made.")

        call("checkoutFilesForEdit", TfvcCheckoutParameters, TfvcCheckoutResult)
            .doc("Makes one or more local files writable and creates \"edit\" pending changes for them in the current workspace.")

        call("renameFile", TfvcRenameRequest, bool)
            .doc("Creates a \"rename\" pending change, which moves or renames a file or folder. Returns success status")
    }

    init {
        signal("shutdown", void)
            .doc("Shuts down the application")

        map("collections", TfsCollectionDefinition, TfsCollection)
    }
}