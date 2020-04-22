// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.tfs.tests

import org.junit.Assert

object IntegrationTestUtils {
    val workspaceNameSuffix: String = System.getenv("MSVSTS_INTELLIJ_VSO_WORKSPACE_SUFFIX") ?: "default"
    val serverUrl: String = System.getenv("MSVSTS_INTELLIJ_VSO_SERVER_URL")
    val teamProject: String = System.getenv("MSVSTS_INTELLIJ_VSO_TEAM_PROJECT")
    val user: String = System.getenv("MSVSTS_INTELLIJ_VSO_USER")
    val pass: String = System.getenv("MSVSTS_INTELLIJ_VSO_PASS")
    val tfExe: String = System.getenv("MSVSTS_INTELLIJ_TF_EXE")

    fun ensureInitialized() {
        val message = "You must provide %s for the L2 tests through the following environment variable: %s"
        fun assertNotEmpty(name: String, env: String, value: String) {
            Assert.assertFalse(String.format(message, name, env), value.isEmpty())
        }

        assertNotEmpty("user", "MSVSTS_INTELLIJ_VSO_USER", user)
        assertNotEmpty("pass", "MSVSTS_INTELLIJ_VSO_PASS", pass)
        assertNotEmpty("serverUrl", "MSVSTS_INTELLIJ_VSO_SERVER_URL", serverUrl)
        assertNotEmpty("teamProject", "MSVSTS_INTELLIJ_VSO_TEAM_PROJECT", teamProject)
        assertNotEmpty("tfExe", "MSVSTS_INTELLIJ_TF_EXE", tfExe)
    }
}
