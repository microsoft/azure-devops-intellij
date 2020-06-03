// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.tfs.tests

import com.google.common.io.Files
import com.microsoft.tfs.Logging
import com.microsoft.tfs.TfsClient
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet
import com.microsoft.tfs.model.host.TfsLocalPath
import org.apache.commons.io.FileUtils
import org.apache.log4j.Level
import org.junit.Assert
import java.nio.charset.StandardCharsets
import java.nio.file.Path

abstract class TfsClientTestFixture : LifetimedTest() {

    companion object {
        fun assertNoPendingChanges(client: TfsClient, path: Path) {
            val states = client.status(listOf(TfsLocalPath(path.toString())))
            Assert.assertEquals("There should be no changes in $path", emptyList<PendingSet>(), states)
        }

        fun createTestFile(filePath: Path) {
            filePath.parent.toFile().mkdirs()

            @Suppress("UnstableApiUsage")
            Files.write("testfile", filePath.toFile(), StandardCharsets.UTF_8)
        }

        fun ignoreItem(workspace: Path, item: String) {
            val tfIgnore = workspace.resolve(".tfignore")

            @Suppress("UnstableApiUsage")
            Files.append("\n$item", tfIgnore.toFile(), StandardCharsets.UTF_8)
        }
    }

    protected lateinit var workspacePath: Path

    abstract fun cloneRepository(): Path

    override fun setUp() {
        Logging.initialize(null, Level.INFO)
        super.setUp()
        IntegrationTestUtils.ensureInitialized()
        workspacePath = cloneRepository()
    }

    override fun tearDown() {
        deleteWorkspace(workspacePath)
        FileUtils.deleteDirectory(workspacePath.toFile())
        super.tearDown()
    }
}