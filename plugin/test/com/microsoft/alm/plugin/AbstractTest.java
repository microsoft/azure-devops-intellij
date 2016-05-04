// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin;

import com.microsoft.alm.plugin.mocks.MockCredentialsPrompt;
import com.microsoft.alm.plugin.mocks.MockLocalizationService;
import com.microsoft.alm.plugin.mocks.MockPropertyService;
import com.microsoft.alm.plugin.mocks.MockServerContextStore;
import com.microsoft.alm.plugin.services.PluginServiceProvider;
import com.microsoft.alm.plugin.telemetry.TfsTelemetryHelper;
import com.microsoft.applicationinsights.extensibility.ContextInitializer;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;

public class AbstractTest {
    private static Logger logger;
    private static TestAppender appender;

    @BeforeClass
    public static void setup() {

        // Make sure we skip client initialization so telemetry is not sent to azure
        System.setProperty("com.microsoft.alm.plugin.telemetry.skipClientInitialization", "true");

        // To test the telemetry helper singleton class, we need to verify what it logs
        // Setup a test appender for log4j logger to capture log messages for verification
        logger = Logger.getLogger(TfsTelemetryHelper.class);
        appender = new TestAppender();
        logger.addAppender(appender);

        // Attach appropriate test services
        PluginServiceProvider.getInstance().initialize(new MockServerContextStore(), new MockCredentialsPrompt(), null, new ContextInitializer() {
            @Override
            public void initialize(TelemetryContext context) {
            }
        }, new MockPropertyService(), new MockLocalizationService(), false);
    }

    public static void assertLogged(final String s) {
        final String s2 = appender.getAndClearLog();
        org.junit.Assert.assertEquals(String.format("'%s' != '%s'", s, s2), s, s2);
    }
}
