// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.services;

import javax.net.ssl.SSLContext;

/**
 * Service that provides system SSL certificate store access.
 */
public interface CertificateService {
    SSLContext getSSLContext();
}
