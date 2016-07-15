// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.services;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ex.ApplicationInfoEx;
import com.intellij.openapi.extensions.PluginId;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.telemetry.TfsTelemetryConstants;
import com.microsoft.alm.plugin.telemetry.TfsTelemetryHelper;
import com.microsoft.alm.plugin.telemetry.TfsTelemetryInstrumentationInfo;
import com.microsoft.applicationinsights.extensibility.ContextInitializer;
import com.microsoft.applicationinsights.extensibility.context.ComponentContext;
import com.microsoft.applicationinsights.extensibility.context.ContextTagKeys;
import com.microsoft.applicationinsights.extensibility.context.DeviceContext;
import com.microsoft.applicationinsights.extensibility.context.UserContext;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;


/**
 * This class is provided as a ContextInitializer for the application insights TelemetryClient.
 */
public class TelemetryContextInitializer implements ContextInitializer {
    private static final Logger logger = LoggerFactory.getLogger(TelemetryContextInitializer.class);

    private static final String DEFAULT_VERSION = "0";
    private static final String LINUX_DISTRIBUTION_NAME = "NAME=";
    private static final String LINUX_DISTRIBUTION_VERSION = "VERSION=";
    private static final String LINUX_OS_NAME = "Linux";
    private static final String SYS_PROP_OS_NAME = "os.name";
    private static final String SYS_PROP_OS_VERSION = "os.version";
    private static final String SYS_PROP_OS_ARCH = "os.arch";
    private static final String SYS_PROP_USER_NAME = "user.name";
    private static final String SYS_PROP_JAVA_RUNTIME = "java.runtime.name";
    private static final String SYS_PROP_JAVA_VERSION = "java.version";

    private String linuxDistribution = StringUtils.EMPTY;

    @Override
    public void initialize(final TelemetryContext context) {
        initializeInstrumentationKey(context);
        initializeProperties(context.getProperties());
        initializeUser(context.getUser());
        initializeComponent(context.getComponent());
        initializeDevice(context.getDevice());
        initializeTags(context.getTags());
    }

    private void initializeDevice(final DeviceContext device) {
        device.setOperatingSystem(getPlatformName());
        device.setOperatingSystemVersion(getPlatformVersion());
    }

    private void initializeInstrumentationKey(final TelemetryContext context) {
        context.setInstrumentationKey(TfsTelemetryInstrumentationInfo.getInstance().getInstrumentationKey());
    }

    private void initializeUser(final UserContext user) {
        user.setId(getUserId());
        user.setUserAgent(TfPluginBundle.BUNDLE_NAME);
    }

    private String getUserId() {
        final String computerName = getComputerName();
        final String userName = getSystemProperty(SYS_PROP_USER_NAME);
        final String fakeUserId = MessageFormat.format("{0}@{1}", userName, computerName);

        return DigestUtils.sha1Hex(fakeUserId);
    }

    private String getComputerName() {
        String hostname = TfsTelemetryHelper.UNKNOWN;

        try {
            final InetAddress address = InetAddress.getLocalHost();
            hostname = address.getHostName();
        } catch (UnknownHostException ex) {
            // This case is covered by the initial value of hostname above
        }

        return hostname;
    }

    private void initializeComponent(final ComponentContext component) {
        component.setVersion(getPluginVersion());
    }

    private void initializeTags(final Map<String, String> tags) {
        tags.put(ContextTagKeys.getKeys().getApplicationId(), TfPluginBundle.BUNDLE_NAME);
        tags.put(ContextTagKeys.getKeys().getDeviceOS(), getPlatformName());
        tags.put(ContextTagKeys.getKeys().getDeviceOSVersion(), getPlatformVersion());
    }

    private void initializeProperties(final Map<String, String> properties) {
        properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_USER_ID, getUserId());

        // Get IntelliJ IDEA version info
        ApplicationInfoEx appInfo = (ApplicationInfoEx) ApplicationInfo.getInstance();
        properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_MAJOR_VERSION, appInfo.getMajorVersion());
        properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_MINOR_VERSION, appInfo.getMinorVersion());
        properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_BUILD_NUMBER, appInfo.getBuild().asString());
        properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_EXE_NAME, appInfo.getFullApplicationName());

        // Get our plugin version
        properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_PLUGIN_VERSION, getPluginVersion());

        properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_PROCESSOR_ARCHITECTURE, getProcessorArchitecture());
        properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_LOCALE_NAME, getLocaleName());

        properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_OS_MAJOR_VERSION, getPlatformMajorVersion());
        properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_OS_MINOR_VERSION, getPlatformMinorVersion());
        properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_OS_NAME, getPlatformName());
        properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_OS_SHORT_NAME, getPlatformShortName());
        properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_OS_FULL_NAME, getPlatformFullName());

        properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_JAVA_RUNTIME_NAME, getJavaName());
        properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_JAVA_RUNTIME_VERSION, getJavaVersion());

        // TODO do we need this information (Eclipse plugin provides it)
        //properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_FRAMEWORK_NAME, getFrameworkName());
        //properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_FRAMEWORK_VERSION, getFrameworkVersion());
    }

    private String getSystemProperty(final String propertyName) {
        return System.getProperty(propertyName, StringUtils.EMPTY);
    }

    private String getPlatformName() {
        return getSystemProperty(SYS_PROP_OS_NAME);
    }

    private String getPlatformShortName() {
        final String osName = getSystemProperty(SYS_PROP_OS_NAME);
        final String shortName;

        if (StringUtils.isEmpty(osName)) {
            shortName = StringUtils.EMPTY;
        } else {
            final String[] nameParts = osName.trim().split(" ", 2);
            shortName = nameParts[0];
        }

        return shortName;
    }

    private String getPlatformVersion() {
        return getSystemProperty(SYS_PROP_OS_VERSION);
    }

    private String getPlatformMajorVersion() {
        final String osVersion = getSystemProperty(SYS_PROP_OS_VERSION);

        if (osVersion.indexOf('.') < 0) {
            return osVersion;
        } else {
            return osVersion.split("\\.", 2)[0];
        }
    }

    private String getPlatformMinorVersion() {
        final String osVersion = getSystemProperty(SYS_PROP_OS_VERSION);

        if (osVersion.indexOf('.') < 0) {
            return StringUtils.EMPTY;
        } else {
            return osVersion.split("\\.", 2)[1];
        }
    }

    private String getPlatformFullName() {
        return MessageFormat.format("{0} ({1})",
                getSystemProperty(SYS_PROP_OS_NAME),
                LINUX_OS_NAME.equalsIgnoreCase(getPlatformName()) ?
                        getLinuxDistribution() : getSystemProperty(SYS_PROP_OS_VERSION));
    }

    private String getLinuxDistribution() {
        if (StringUtils.isEmpty(linuxDistribution)) {
            linuxDistribution = findLinuxDistribution();
        }
        return linuxDistribution;
    }

    /**
     * Looks for Linux distribution in the *-release file which is present on most (if not all) Linux distributions
     * If the file doesn't exist we will default to report an unknown Linux distro
     *
     * @return linux distribution name and version
     */
    private String findLinuxDistribution() {
        final String[] commands = {"/bin/sh", "-c", "cat /etc/*-release"};
        String distribution = TfsTelemetryHelper.UNKNOWN;
        String version = TfsTelemetryHelper.UNKNOWN;
        BufferedReader bufferedReader = null;

        try {
            final Process process = Runtime.getRuntime().exec(commands);
            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.startsWith(LINUX_DISTRIBUTION_NAME)) {
                    distribution = line.replaceFirst(LINUX_DISTRIBUTION_NAME, StringUtils.EMPTY).replace("\"", StringUtils.EMPTY);
                }
                if (line.startsWith(LINUX_DISTRIBUTION_VERSION)) {
                    version = line.replaceFirst(LINUX_DISTRIBUTION_VERSION, StringUtils.EMPTY).replace("\"", StringUtils.EMPTY);
                }
            }
        } catch (Exception e) {
            logger.warn("Could not find Linux distribution due to error", e);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    logger.warn("Could not close buffered reader", e);
                }
            }
            return MessageFormat.format("{0} - {1}", distribution, version);
        }
    }

    private String getProcessorArchitecture() {
        return getSystemProperty(SYS_PROP_OS_ARCH).toUpperCase();
    }

    private String getLocaleName() {
        return Locale.getDefault().getDisplayName();
    }

    private String getJavaName() {
        return getSystemProperty(SYS_PROP_JAVA_RUNTIME);
    }

    private String getJavaVersion() {
        return getSystemProperty(SYS_PROP_JAVA_VERSION);
    }

    // Get version info of our Plugin
    private String getPluginVersion() {
        final IdeaPluginDescriptor plugin = PluginManager.getPlugin(PluginId.getId("com.microsoft.vso.idea"));
        final String v = plugin != null ? plugin.getVersion() : DEFAULT_VERSION;
        return StringUtils.isNotEmpty(v) ? v : DEFAULT_VERSION;
    }
}
