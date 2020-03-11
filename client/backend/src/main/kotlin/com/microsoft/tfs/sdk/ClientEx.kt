// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.tfs.sdk

import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.WorkspaceNotFoundException
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace
import com.microsoft.tfs.model.host.TfsLocalPath
import com.microsoft.tfs.model.host.TfsPath
import com.microsoft.tfs.model.host.TfsServerPath

fun VersionControlClient.tryGetWorkspace(path: TfsPath): Workspace? = when(path) {
    is TfsLocalPath -> tryGetWorkspace(path.path)
    is TfsServerPath -> {
        try {
            getWorkspace(path.workspace, ".")
        } catch (ex: WorkspaceNotFoundException) {
            null
        }
    }
    else -> throw Exception("Unknown path type: $path")
}