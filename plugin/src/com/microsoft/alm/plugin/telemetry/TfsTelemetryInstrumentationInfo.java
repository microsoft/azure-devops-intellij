// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.telemetry;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Properties;

/**
 * Get Telemetry information
 * <p/>
 * This class is for internal use only.
 *
 * @threadsafety thread-safe
 */
public abstract class TfsTelemetryInstrumentationInfo {

    private final static Logger logger = LoggerFactory.getLogger(TfsTelemetryInstrumentationInfo.class);

    /**
     * This resource should contain all telemetry information.
     * <p/>
     * There are two properties: telemetry.instrumentation.is_test_environment
     * telemetry.instrumentation.is_developer_mode
     * <p/>
     * If "is_test_environment" resolves to false, "is_developer_mode" will be
     * ignored
     * <p/>
     * Default to "is_test_environment" to false in case this file does not
     * exist
     */
    public static final String TELEMETRY_INSTRUMENTATION_PROPERTIES_RESOURCE =
            "/telemetry/com.microsoft.alm.plugin-telemetry.properties"; //$NON-NLS-1$

    private static final String VSO_INTELLIJ_PROD_KEY = "132bb2e2-06d4-4908-a34b-87be041cc31c"; //$NON-NLS-1$
    private static final String VSO_INTELLIJ_TEST_KEY = "07da2cb6-eed8-4361-b364-ecf1a1559ae7"; //$NON-NLS-1$

    private static boolean isTestEnv;
    private static boolean isDeveloperMode;

    static {
        final InputStream in =
                TfsTelemetryInstrumentationInfo.class.getResourceAsStream(TELEMETRY_INSTRUMENTATION_PROPERTIES_RESOURCE);
        initialize(in);
    }

    static void initialize(final InputStream in) {
        // Default to production environment with batch uploading
        isTestEnv = false;
        isDeveloperMode = false;

        if (in != null) {
            try {
                final Properties props = new Properties();
                props.load(in);
                final String isTestEnvProperty =
                        props.getProperty("telemetry.instrumentation.is_test_environment"); //$NON-NLS-1$
                final String isDeveloperModeProperty =
                        props.getProperty("telemetry.instrumentation.is_developer_mode"); //$NON-NLS-1$

                // Default to production environment, all invalid inputs
                // will be resolved as "false"
                if (StringUtils.isNotEmpty(isTestEnvProperty) && Boolean.parseBoolean(isTestEnvProperty)) {
                    isTestEnv = true;
                    if (StringUtils.isNotEmpty(isDeveloperModeProperty)
                            && Boolean.parseBoolean(isDeveloperModeProperty)) {
                        isDeveloperMode = true;
                    }
                }
            } catch (IOException e) {
                logger.warn(MessageFormat.format("Unable to load property resource {0} with exception {1}", //$NON-NLS-1$
                        TELEMETRY_INSTRUMENTATION_PROPERTIES_RESOURCE,
                        e));
                // suppressing exception
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.warn(MessageFormat.format("Unable to dispose property resource {0} with exception {1}", //$NON-NLS-1$
                            TELEMETRY_INSTRUMENTATION_PROPERTIES_RESOURCE,
                            e));
                    // suppressing exception
                }
            }
        } else {
            logger.warn(MessageFormat.format("Unable to locate property resource {0}", //$NON-NLS-1$
                    TELEMETRY_INSTRUMENTATION_PROPERTIES_RESOURCE));
        }
    }

    public static boolean isDeveloperMode() {
        return isDeveloperMode;
    }

    public static boolean isTestKey() {
        return isTestEnv;
    }

    public static String getInstrumentationKey() {
        return isTestKey() ? VSO_INTELLIJ_TEST_KEY : VSO_INTELLIJ_PROD_KEY;
    }
}
