// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.tfs.sdk

import com.microsoft.tfs.core.clients.versioncontrol.events.NewPendingChangeListener
import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorListener
import com.microsoft.tfs.core.clients.versioncontrol.events.UndonePendingChangeListener
import com.microsoft.tfs.core.clients.versioncontrol.events.VersionControlEventEngine

fun VersionControlEventEngine.withUndonePendingChangeListener(
    listener: UndonePendingChangeListener,
    action: () -> Unit) {
    addUndonePendingChangeListener(listener)
    try {
        action()
    } finally {
        removeUndonePendingChangeListener(listener)
    }
}

fun VersionControlEventEngine.withNewPendingChangeListener(
    listener: NewPendingChangeListener,
    action: () -> Unit) {
    addNewPendingChangeListener(listener)
    try {
        action()
    } finally {
        removeNewPendingChangeListener(listener)
    }
}


fun VersionControlEventEngine.withNonFatalErrorListener(
    listener: NonFatalErrorListener,
    action: () -> Unit) {
    addNonFatalErrorListener(listener)
    try {
        action()
    } finally {
        removeNonFatalErrorListener(listener)
    }
}
