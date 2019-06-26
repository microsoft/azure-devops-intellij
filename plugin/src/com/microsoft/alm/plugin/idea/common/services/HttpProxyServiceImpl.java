// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.services;

import com.intellij.util.net.HttpConfigurable;
import com.microsoft.alm.common.utils.SystemHelper;
import com.microsoft.alm.plugin.idea.common.utils.BackCompatibleUtils;
import com.microsoft.alm.plugin.services.HttpProxyService;
import com.microsoft.alm.plugin.services.PluginServiceProvider;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implementation defaults to using proxy settings from the System properties if they are set.
 * Then it will look into the IntelliJ proxy settings.
 * If you ask for the host and port, it defaults to the Fiddler settings and then looks for other settings.
 * Authentication is only done if the IntelliJ proxy settings are being used.
 */
public class HttpProxyServiceImpl implements HttpProxyService {
    private static final Logger logger = LoggerFactory.getLogger(HttpProxyServiceImpl.class);

    private static final String PROP_PROXY_SET = "proxySet";
    private static final String PROP_PROXY_HOST = "proxyHost";
    private static final String PROP_PROXY_PORT = "proxyPort";
    private static final String PROXY_URL_FORMAT = "http://%s:%d";

    private boolean useHttpProxyViaSystemProperties;
    private boolean useHttpProxyViaIntelliJProperties;

    private void initialize() {
        useHttpProxyViaSystemProperties = StringUtils.equalsIgnoreCase(System.getProperty(PROP_PROXY_SET), "true");
        if (!useHttpProxyViaSystemProperties) {
            useHttpProxyViaIntelliJProperties = PluginServiceProvider.getInstance().isInsideIDE() &&
                    HttpConfigurable.getInstance() != null &&
                    HttpConfigurable.getInstance().USE_HTTP_PROXY;
        } else {
            useHttpProxyViaIntelliJProperties = false;
        }
    }

    @Override
    public boolean useHttpProxy() {
        initialize();
        logger.info("useHttpProxy: " + (useHttpProxyViaSystemProperties || useHttpProxyViaIntelliJProperties));
        return useHttpProxyViaSystemProperties || useHttpProxyViaIntelliJProperties;
    }

    @Override
    public boolean isAuthenticationRequired() {
        initialize();
        // We don't do authentication if we are using the system properties
        final boolean result = !useHttpProxyViaSystemProperties &&
                useHttpProxyViaIntelliJProperties &&
                HttpConfigurable.getInstance().PROXY_AUTHENTICATION;
        logger.info("isAuthenticationRequired: " + result);
        return result;
    }

    @Override
    public String getProxyURL() {
        final String result = String.format(PROXY_URL_FORMAT, getProxyHost(), getProxyPort());
        logger.info("getProxyURL: " + result);
        return result;
    }

    @Override
    public String getProxyHost() {
        initialize();
        // default to Fiddler proxy host
        String proxyHost = "127.0.0.1";
        if (useHttpProxyViaSystemProperties && System.getProperty(PROP_PROXY_HOST) != null) {
            proxyHost = System.getProperty(PROP_PROXY_HOST);
        } else if (useHttpProxyViaIntelliJProperties) {
            proxyHost = HttpConfigurable.getInstance().PROXY_HOST;
        }
        logger.info("getProxyHost: " + proxyHost);
        return proxyHost;
    }

    @Override
    public int getProxyPort() {
        initialize();
        // default to Fiddler proxy port
        int proxyPort = 8888;
        if (useHttpProxyViaSystemProperties && System.getProperty(PROP_PROXY_PORT) != null) {
            proxyPort = SystemHelper.toInt(System.getProperty(PROP_PROXY_PORT), proxyPort);
        } else if (useHttpProxyViaIntelliJProperties) {
            proxyPort = HttpConfigurable.getInstance().PROXY_PORT;
        }
        logger.info("getProxyPort: " + proxyPort);
        return proxyPort;
    }

    @Override
    public String getUserName() {
        initialize();
        final String result;
        if (useHttpProxyViaIntelliJProperties) {
            result = BackCompatibleUtils.getProxyLogin();
        } else {
            result = null;
        }

        logger.info("getUserName: " + result);
        return result;
    }

    @Override
    public String getPassword() {
        logger.info("getPassword called");
        initialize();
        if (useHttpProxyViaIntelliJProperties) {
            return HttpConfigurable.getInstance().getPlainProxyPassword();
        } else {
            return null;
        }
    }
}
