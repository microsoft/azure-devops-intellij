// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.services;

import com.intellij.util.net.ssl.CertificateManager;
import com.microsoft.alm.plugin.services.CertificateService;

import javax.net.ssl.SSLContext;

public class IdeaCertificateService implements CertificateService {
    @Override
    public SSLContext getSSLContext() {
        return CertificateManager.getInstance().getSslContext();
    }
}
