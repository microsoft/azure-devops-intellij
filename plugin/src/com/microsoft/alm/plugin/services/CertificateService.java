package com.microsoft.alm.plugin.services;

import javax.net.ssl.SSLContext;

/**
 * Service that provides system SSL certificate store access.
 */
public interface CertificateService {
    SSLContext getSSLContext();
}
