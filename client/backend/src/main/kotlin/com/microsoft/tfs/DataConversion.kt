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

fun TfsPath.toCanonicalPathString(): String = when (this) {
    is TfsLocalPath -> LocalPath.canonicalize(path)
    is TfsServerPath -> path
    else -> throw Exception("Unknown path type: $this")
}

fun TfsPath.toCanonicalPathItemSpec(recursionType: RecursionType): ItemSpec =
    ItemSpec(toCanonicalPathString(), recursionType)

private val ExtendedItem.changeTypeName
    get() =
        if (pendingChange == ChangeType.NONE) "none"
        else pendingChange.toUIString(false, this)

private val ExtendedItem.itemTypeName
    get() = itemType.toUIString()

private val ExtendedItem.checkinDateString
    get() = checkinDate?.time?.let(isoDateFormat::format)

private val ExtendedItem.encodingName
    get() =
        if (encoding == FileEncoding(VersionControlConstants.ENCODING_UNCHANGED)) null
        else encoding?.name

private val ExtendedItem.fileEncodingName
    get() = if (itemType == ItemType.FILE) encodingName else null

private val ExtendedItem.lockStatus
    get() = lockLevel.toUIString()

fun ExtendedItem.toLocalItemInfo(): TfsLocalItemInfo =
    TfsLocalItemInfo(
        targetServerItem,
        localItem,
        localVersion,
        latestVersion,
        changeTypeName,
        itemTypeName,
        checkinDateString,
        fileEncodingName
    )

fun ExtendedItem.toExtendedItemInfo(): TfsExtendedItemInfo =
    TfsExtendedItemInfo(
        lockStatus,
        lockOwner,
        targetServerItem,
        localItem,
        localVersion,
        latestVersion,
        changeTypeName,
        itemTypeName,
        checkinDateString,
        fileEncodingName
    )
