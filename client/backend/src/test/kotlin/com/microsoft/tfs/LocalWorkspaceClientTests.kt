// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.tfs

import com.microsoft.tfs.model.host.TfsLocalPath
import com.microsoft.tfs.tests.TfsClientTestFixture
import com.microsoft.tfs.tests.cloneTestRepository
import com.microsoft.tfs.tests.createClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path

class LocalWorkspaceClientTests : TfsClientTestFixture() {

    override fun cloneRepository(): Path =
        cloneTestRepository()

    @Test
    fun clientShouldReturnDeletedPath() {
        val client = createClient(testLifetime)
        val filePath = workspacePath.resolve("readme.txt")
        assertTrue("Test file should exits", filePath.toFile().exists())
        val localPathList = listOf(TfsLocalPath(filePath.toString()))

        val result = client.deletePathsRecursively(localPathList)

        assertEquals(localPathList, result.deletedPaths)
        assertEquals(emptyList<TfsLocalPath>(), result.notFoundPaths)
        assertEquals(emptyList<String>(), result.errorMessages)
    }

    @Test
    fun clientShouldThrowExceptionWhenTryingToDeleteIgnoredItem() {
        val client = createClient(testLifetime)
        val filePath = workspacePath.resolve(".idea/libraries/something.xml")

        createTestFile(filePath)
        ignoreItem(workspacePath, ".idea")
        assertNoPendingChanges(client, filePath)

        val result = client.deletePathsRecursively(listOf(TfsLocalPath(filePath.toString())))
        assertEquals(emptyList<TfsLocalPath>(), result.deletedPaths)
        assertEquals(listOf(TfsLocalPath(filePath.toString())), result.notFoundPaths)
        assertEquals(emptyList<String>(), result.errorMessages)
    }

    @Test
    fun clientShouldBeAbleToRenameAFile() {
        val client = createClient(testLifetime)
        val oldPath = workspacePath.resolve("readme.txt")
        val newPath = workspacePath.resolve("donotreadme.txt")

        val result = client.renameFile(TfsLocalPath(oldPath.toString()), TfsLocalPath(newPath.toString()))

        assertTrue(result)
    }
}
