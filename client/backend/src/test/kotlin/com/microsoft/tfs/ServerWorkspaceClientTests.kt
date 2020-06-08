// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.tfs

import com.microsoft.tfs.jni.FileSystemUtils
import com.microsoft.tfs.model.host.TfsLocalPath
import com.microsoft.tfs.model.host.TfsServerStatusType
import com.microsoft.tfs.tests.TfsClientTestFixture
import com.microsoft.tfs.tests.cloneTestRepository
import com.microsoft.tfs.tests.createClient
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Path

class ServerWorkspaceClientTests : TfsClientTestFixture() {
    override fun cloneRepository(): Path =
        cloneTestRepository(true)

    private val fileSystem = FileSystemUtils.getInstance()

    private fun isReadOnly(path: Path): Boolean =
        fileSystem.getAttributes(path.toFile()).isReadOnly

    @Test
    fun readOnlyFilesShouldBeClonedByDefault() {
        val filePath = workspacePath.resolve("readme.txt")
        assertTrue(isReadOnly(filePath))
    }

    // @Test for manual run only, because concurrent checkout in tests is not supported
    fun readOnlyFlagShouldBeClearedOnCheckout() {
        val client = createClient(testLifetime)
        val filePath = workspacePath.resolve("readme.txt")
        client.checkoutFilesForEdit(listOf(TfsLocalPath(filePath.toString())), false)
        assertFalse(isReadOnly(filePath))
    }

    // @Test for manual run only, because concurrent checkout in tests is not supported
    fun getPendingChangesShouldReturnACheckedOutFile() {
        val client = createClient(testLifetime)
        val filePath = workspacePath.resolve("readme.txt")
        val paths = listOf(TfsLocalPath(filePath.toString()))
        client.checkoutFilesForEdit(paths, false)

        val pendingChanges = toPendingChanges(client.status(paths).single())
        val change = pendingChanges.single()
        val types = change.changeTypes
        assertEquals(TfsServerStatusType.EDIT, types.single())
    }
}