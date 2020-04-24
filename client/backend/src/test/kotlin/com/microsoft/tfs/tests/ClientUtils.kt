// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.tfs.tests

import com.jetbrains.rd.util.lifetime.Lifetime
import com.microsoft.tfs.TfsClient
import com.microsoft.tfs.core.clients.versioncontrol.Workstation
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials
import java.net.URI

fun createClient(lifetime: Lifetime): TfsClient {
    val serverUri = URI(IntegrationTestUtils.serverUrl)
    val credentials = UsernamePasswordCredentials(IntegrationTestUtils.user, IntegrationTestUtils.pass)
    val client = TfsClient(lifetime, serverUri, credentials)
    val workstation = Workstation.getCurrent(client.client.connection.persistenceStoreProvider)
    workstation.reloadCache()
    return client
}