// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.tfs.tests

import com.jetbrains.rd.util.lifetime.Lifetime
import com.microsoft.tfs.TfsClient
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials
import java.net.URI

fun createClient(lifetime: Lifetime): TfsClient {
    val serverUri = URI(IntegrationTestUtils.serverUrl)
    val credentials = UsernamePasswordCredentials(IntegrationTestUtils.user, IntegrationTestUtils.pass)
    return TfsClient(lifetime, serverUri, credentials)
}