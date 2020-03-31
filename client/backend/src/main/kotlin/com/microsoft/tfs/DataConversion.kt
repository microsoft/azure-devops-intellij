// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.tfs

import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.*
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec
import com.microsoft.tfs.core.util.FileEncoding
import com.microsoft.tfs.model.host.*
import java.text.SimpleDateFormat

private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

private val pendingChangeTypeMap = mapOf(
    ChangeType.ADD to TfsServerStatusType.ADD,
    ChangeType.EDIT to TfsServerStatusType.EDIT,
    ChangeType.ENCODING to TfsServerStatusType.UNKNOWN,
    ChangeType.RENAME to TfsServerStatusType.RENAME,
    ChangeType.DELETE to TfsServerStatusType.DELETE,
    ChangeType.UNDELETE to TfsServerStatusType.UNDELETE,
    ChangeType.BRANCH to TfsServerStatusType.BRANCH,
    ChangeType.MERGE to TfsServerStatusType.MERGE,
    ChangeType.LOCK to TfsServerStatusType.LOCK,
    ChangeType.ROLLBACK to TfsServerStatusType.UNKNOWN,
    ChangeType.SOURCE_RENAME to TfsServerStatusType.RENAME,
    ChangeType.TARGET_RENAME to TfsServerStatusType.UNKNOWN,
    ChangeType.PROPERTY to TfsServerStatusType.EDIT
)

private fun toChangeTypes(changeType: ChangeType): List<TfsServerStatusType> =
    pendingChangeTypeMap.entries.mapNotNull { (k, v) -> if (changeType.contains(k)) v else null }

private fun toPendingChange(pendingSet: PendingSet, pc: PendingChange) = TfsPendingChange(
    pc.serverItem,
    pc.localItem,
    pc.version,
    pc.pendingSetOwner,
    isoDateFormat.format(pc.creationDate.time),
    pc.lockLevelName,
    toChangeTypes(pc.changeType),
    pendingSet.name,
    pendingSet.computer,
    pc.isCandidate,
    pc.sourceServerItem
)

fun toPendingChanges(pendingSet: PendingSet): Iterable<TfsPendingChange> =
    (pendingSet.pendingChanges.asSequence().map { toPendingChange(pendingSet, it) }
            + pendingSet.candidatePendingChanges.map { toPendingChange(pendingSet, it) }).asIterable()

fun TfsPath.toCanonicalPathString(): String = when(this) {
    is TfsLocalPath -> LocalPath.canonicalize(path)
    is TfsServerPath -> path
    else -> throw Exception("Unknown path type: $this")
}

fun TfsPath.toCanonicalPathItemSpec(recursionType: RecursionType): ItemSpec =
    ItemSpec(toCanonicalPathString(), recursionType)

/**
 * Converts a pair of [Item] and [ExtendedItem] to [TfsItemInfo]. Tries to replicate behavior of
 * `com.microsoft.tfs.client.clc.vc.commands.CommandProperties.ItemProperties::setExtendedItem` from the TFS
 * command-line client.
 */
fun toTfsItemInfo(item: Item?, extendedItem: ExtendedItem?): TfsItemInfo {
    if (item == null && extendedItem == null) {
        throw Exception("Bot item and extendedItem should never be null")
    }

    var serverItem = item?.serverItem
    var localItem: String? = null
    var localVersion = 0
    val serverVersion = item?.changeSetID ?: 0
    var change: String? = null
    var itemTypeName: String? = null
    var lockStatus: String? = null
    var lockOwner: String? = null
    val deletionId = item?.deletionID ?: 0
    val checkInDate = item?.checkinDate?.time?.let(isoDateFormat::format)
    var type = item?.itemType
    val encodingName = if (item?.encoding == FileEncoding(VersionControlConstants.ENCODING_UNCHANGED))
        null
    else
        item?.encoding?.name
    val fileSize = item?.contentLength

    if (extendedItem != null) {
        // Only show the server item when the user has the file or has a pending change on the file.
        if (extendedItem.localItem != null || extendedItem.pendingChange != ChangeType.NONE) {
            serverItem = extendedItem.targetServerItem
            localItem = extendedItem.localItem
            localVersion = extendedItem.localVersion
            itemTypeName = extendedItem.itemType.toUIString()
        }

        change =
            if (extendedItem.pendingChange == ChangeType.NONE) "none"
            else extendedItem.pendingChange.toUIString(false, extendedItem)

        lockStatus = extendedItem.lockLevel.toUIString()
        lockOwner = extendedItem.lockOwner
        type = extendedItem.itemType
    }

    val fileEncodingName = if (type == ItemType.FILE) encodingName else null

    return TfsItemInfo(
        serverItem,
        localItem,
        localVersion,
        serverVersion,
        change,
        itemTypeName,
        lockStatus,
        lockOwner,
        deletionId,
        checkInDate,
        fileEncodingName,
        fileSize
    )
}
