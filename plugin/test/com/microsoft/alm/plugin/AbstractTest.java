// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin;

import com.microsoft.alm.plugin.mocks.MockCertificateService;
import com.microsoft.alm.plugin.mocks.MockCredentialsPrompt;
import com.microsoft.alm.plugin.mocks.MockHttpProxyService;
import com.microsoft.alm.plugin.mocks.MockLocalizationService;
import com.microsoft.alm.plugin.mocks.MockPropertyService;
import com.microsoft.alm.plugin.mocks.MockServerContextStore;
import com.microsoft.alm.plugin.services.AsyncService;
import com.microsoft.alm.plugin.services.PluginServiceProvider;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;

public class AbstractTest {
    private static Logger logger;
    private static TestAppender appender;

    @BeforeClass
    public static void setup() {
        // Setup a test appender for log4j logger to capture log messages for verification
        logger = Logger.getLogger(AbstractTest.class);
        appender = new TestAppender();
        logger.addAppender(appender);

        // Attach appropriate test services
        PluginServiceProvider.getInstance().initialize(new MockServerContextStore(), new MockCredentialsPrompt(), null,
                new MockPropertyService(), new MockLocalizationService(), new MockHttpProxyService(),
                new AsyncService() {
                    @Override
                    public void executeOnPooledThread(Runnable runnable) {
                        runnable.run();
                    }
                }, new MockCertificateService(), false);
    }

    public static void assertLogged(final String s) {
        final String s2 = appender.getAndClearLog();
        org.junit.Assert.assertEquals(String.format("'%s' != '%s'", s, s2), s, s2);
    }
}