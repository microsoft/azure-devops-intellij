package com.microsoft.tfs

import com.microsoft.tfs.jni.FileSystemUtils
import com.microsoft.tfs.model.host.TfsLocalPath
import com.microsoft.tfs.tests.TfsClientTestFixture
import com.microsoft.tfs.tests.cloneTestRepository
import com.microsoft.tfs.tests.createClient
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun readOnlyFlagShouldBeClearedOnCheckout() {
        val client = createClient(testLifetime)
        val filePath = workspacePath.resolve("readme.txt")
        client.checkoutFilesForEdit(listOf(TfsLocalPath(filePath.toString())), false)
        assertFalse(isReadOnly(filePath))
    }
}