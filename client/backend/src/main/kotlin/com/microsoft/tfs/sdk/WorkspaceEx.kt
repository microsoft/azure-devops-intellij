package com.microsoft.tfs.sdk

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace
import com.microsoft.tfs.model.host.TfsLocalPath
import com.microsoft.tfs.model.host.TfsPath
import com.microsoft.tfs.model.host.TfsServerPath

fun Workspace.isPathMapped(path: TfsPath): Boolean = when (path) {
    is TfsLocalPath -> isLocalPathMapped(path.path)
    is TfsServerPath -> isServerPathMapped(path.path)
    else -> throw Exception("Unknown path type: $path")
}