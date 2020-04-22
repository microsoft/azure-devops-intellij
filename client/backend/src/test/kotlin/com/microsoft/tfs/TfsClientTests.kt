// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.tfs

import com.google.common.io.Files
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet
import com.microsoft.tfs.model.host.TfsDeleteFailure
import com.microsoft.tfs.model.host.TfsLocalPath
import com.microsoft.tfs.tests.*
import org.apache.commons.io.FileUtils.deleteDirectory
import org.apache.log4j.Level
import org.junit.Assert
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class TfsClientTests : LifetimedTest() {

    companion object {
        private fun assertNoPendingChanges(client: TfsClient, path: Path) {
            val states = client.status(listOf(TfsLocalPath(path.toString())))
            Assert.assertEquals("There should be no changes in $path", emptyList<PendingSet>(), states)
        }

        private fun ignoreItem(workspace: Path, item: String) {
            val tfIgnore = workspace.resolve(".tfignore")

            @Suppress("UnstableApiUsage")
            Files.append("\n$item", tfIgnore.toFile(), StandardCharsets.UTF_8)
        }
    }

    override fun setUp() {
        Logging.initialize(null, Level.INFO)
        super.setUp()
        IntegrationTestUtils.ensureInitialized()
    }

    @Test
    fun clientShouldThrowExceptionWhenTryingToDeleteIgnoredItem() {
        val workspace = cloneTestRepository()
        try {
            val client = createClient(testLifetime)
            val fileName = ".idea/libraries/something.xml"
            val filePath = workspace.resolve(fileName)
            filePath.parent.toFile().mkdirs()

            @Suppress("UnstableApiUsage")
            Files.write("testfile", filePath.toFile(), StandardCharsets.UTF_8)

            ignoreItem(workspace, ".idea")
            assertNoPendingChanges(client, filePath)

            val result = client.deletePathsRecursively(listOf(TfsLocalPath(filePath.toString()))) as TfsDeleteFailure
            Assert.assertEquals(listOf(TfsLocalPath(filePath.toString())), result.failedPaths)
        } finally {
            deleteWorkspace(workspace)
            deleteDirectory(workspace.toFile())
        }
    }
}
