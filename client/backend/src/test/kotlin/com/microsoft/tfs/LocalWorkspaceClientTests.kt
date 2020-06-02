// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.tfs

import com.microsoft.tfs.model.host.TfsLocalPath
import com.microsoft.tfs.tests.TfsClientTestFixture
import com.microsoft.tfs.tests.cloneTestRepository
import com.microsoft.tfs.tests.createClient
import org.junit.Assert
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

        Assert.assertEquals(localPathList, result.deletedPaths)
        Assert.assertEquals(emptyList<TfsLocalPath>(), result.notFoundPaths)
        Assert.assertEquals(emptyList<String>(), result.errorMessages)
    }

    @Test
    fun clientShouldThrowExceptionWhenTryingToDeleteIgnoredItem() {
        val client = createClient(testLifetime)
        val filePath = workspacePath.resolve(".idea/libraries/something.xml")

        createTestFile(filePath)
        ignoreItem(workspacePath, ".idea")
        assertNoPendingChanges(client, filePath)

        val result = client.deletePathsRecursively(listOf(TfsLocalPath(filePath.toString())))
        Assert.assertEquals(emptyList<TfsLocalPath>(), result.deletedPaths)
        Assert.assertEquals(listOf(TfsLocalPath(filePath.toString())), result.notFoundPaths)
        Assert.assertEquals(emptyList<String>(), result.errorMessages)
    }
}
