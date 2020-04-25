// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.tfs.tests

import com.jetbrains.rd.util.info
import com.microsoft.tfs.Logging
import org.junit.Assert
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

private val logger = Logging.getLogger("ClassicClientUtilsKt")

private fun executeClient(directory: Path, vararg arguments: String) {
    logger.info { "Running tf with arguments: [${arguments.joinToString(",")}]" }
    val loginArgument = "-login:${IntegrationTestUtils.user},${IntegrationTestUtils.pass}"
    val process = ProcessBuilder(IntegrationTestUtils.tfExe, loginArgument, *arguments)
        .directory(directory.toFile())
        .apply {
            environment().also {
                it["TF_NOTELEMETRY"] = "TRUE"
                it["TF_ADDITIONAL_JAVA_ARGS"] = "-Duser.country=US -Duser.language=en"
                it["TF_USE_KEYCHAIN"] = "FALSE" // will be stuck on com.microsoft.tfs.jni.internal.keychain.NativeKeychain.nativeFindInternetPassword on macOS otherwise
            }
        }
        .inheritIO()
        .start()
    val exitCode = process.waitFor()
    Assert.assertEquals(0, exitCode)
}

private fun tfCreateWorkspace(workspacePath: Path, name: String) {
    val collectionUrl = IntegrationTestUtils.serverUrl
    executeClient(workspacePath, "workspace", "-new", "-collection:$collectionUrl", name)
}

private fun tfDeleteWorkspace(workspacePath: Path, workspaceName: String) {
    executeClient(workspacePath, "workspace", "-delete", workspaceName)
}

private fun tfCreateMapping(workspacePath: Path, workspaceName: String, serverPath: String, localPath: Path) {
    executeClient(workspacePath, "workfold", "-map", "-workspace:$workspaceName", serverPath, localPath.toString())
}

private fun tfGet(workspacePath: Path) {
    executeClient(workspacePath, "get")
}

fun cloneTestRepository(): Path {
    val workspacePath = Files.createTempDirectory("adi.b.test.").toFile().canonicalFile.toPath()
    val workspaceName = "${workspacePath.fileName}.${IntegrationTestUtils.workspaceNameSuffix}"
    Assert.assertTrue(workspaceName.length <= 64)
    tfCreateWorkspace(workspacePath, workspaceName)
    tfCreateMapping(workspacePath, workspaceName, "$/${IntegrationTestUtils.teamProject}", Paths.get("."))
    tfGet(workspacePath)
    return workspacePath
}

fun deleteWorkspace(path: Path) {
    val workspaceName = "${path.fileName}.${IntegrationTestUtils.workspaceNameSuffix}"
    tfDeleteWorkspace(path, workspaceName)
}
