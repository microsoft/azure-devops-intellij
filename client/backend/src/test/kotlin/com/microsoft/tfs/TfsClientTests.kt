// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.tfs

import com.google.common.io.Files
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet
import com.microsoft.tfs.model.host.TfsLocalPath
import com.microsoft.tfs.tests.*
import org.apache.commons.io.FileUtils.deleteDirectory
import org.apache.log4j.Level
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class TfsClientTests : LifetimedTest() {

    companion object {
        private fun assertNoPendingChanges(client: TfsClient, path: Path) {
            val states = client.status(listOf(TfsLocalPath(path.toString())))
            Assert.assertEquals("There should be no changes in $path", emptyList<PendingSet>(), states)
        }

        private fun createTestFile(filePath: Path) {
            filePath.parent.toFile().mkdirs()

            @Suppress("UnstableApiUsage")
            Files.write("testfile", filePath.toFile(), StandardCharsets.UTF_8)
        }

        private fun ignoreItem(workspace: Path, item: String) {
            val tfIgnore = workspace.resolve(".tfignore")

            @Suppress("UnstableApiUsage")
            Files.append("\n$item", tfIgnore.toFile(), StandardCharsets.UTF_8)
        }
    }

    lateinit var workspacePath: Path

    override fun setUp() {
        Logging.initialize(null, Level.INFO)
        super.setUp()
        IntegrationTestUtils.ensureInitialized()
        workspacePath = cloneTestRepository()
    }

    override fun tearDown() {
        deleteWorkspace(workspacePath)
        deleteDirectory(workspacePath.toFile())
        super.tearDown()
    }

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
