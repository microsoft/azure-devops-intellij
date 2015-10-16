// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.telemetry;

import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.services.PluginServiceProvider;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.channel.TelemetryChannel;
import com.microsoft.applicationinsights.extensibility.ContextInitializer;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.logger.InternalLogger.LoggerOutputType;
import com.microsoft.applicationinsights.telemetry.PageViewTelemetry;
import com.microsoft.applicationinsights.telemetry.SessionState;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The TfsTelemetryHelper class is a singleton that allows the plugin to capture
 * telemetry data when the user initiates events.
 */
public class TfsTelemetryHelper {

    // Static members
    private static final Logger logger = LoggerFactory.getLogger(TfsTelemetryHelper.class);

    public static final String UNIQUE_PREFIX = "ai-log"; //$NON-NLS-1$
    public static final String BASE_FOLDER = "AppInsights"; //$NON-NLS-1$

    private static final String UNKNOWN = "unknown"; //$NON-NLS-1$

    // Instance members
    private TelemetryClient telemetryClient;

    // A private static class to allow safe lazy initialization of the singleton
    private static class TfsTelemetryHelperHolder {
        private static final TfsTelemetryHelper INSTANCE = new TfsTelemetryHelper();
    }

    /**
     * The getInstance method returns the singleton instance. Creating it if necessary
     */
    public static TfsTelemetryHelper getInstance() {
        // Using the Initialization-on-demand holder pattern to make sure this is thread-safe
        return TfsTelemetryHelperHolder.INSTANCE;
    }

    // The private constructor keeps the class from being inherited or misused
    private TfsTelemetryHelper() {
        final String skip = System.getProperties().getProperty("com.microsoft.alm.plugin.telemetry.skipClientInitialization");
        if (StringUtils.isNotEmpty(skip) && StringUtils.equalsIgnoreCase(skip, "true")) {
            // this flag is here for testing purposes in which case we do not want to create a telemetry channel
            // or client.
            return;
        }

        // Initialize the internal logger
        final Map<String, String> loggerData = new HashMap<String, String>();
        loggerData.put("Level", InternalLogger.LoggingLevel.ERROR.toString()); //$NON-NLS-1$
        loggerData.put("UniquePrefix", UNIQUE_PREFIX); //$NON-NLS-1$
        loggerData.put("BaseFolder", BASE_FOLDER); //$NON-NLS-1$
        InternalLogger.INSTANCE.initialize(LoggerOutputType.FILE.toString(), loggerData);

        // Initialize the active TelemetryConfiguration
        ContextInitializer initializer = PluginServiceProvider.getInstance().getTelemetryContextInitializer();
        TelemetryConfiguration.getActive().getContextInitializers().add(initializer);

        // Create a channel to AppInsights
        final TelemetryChannel channel = TelemetryConfiguration.getActive().getChannel();
        if (channel != null) {
            channel.setDeveloperMode(TfsTelemetryInstrumentationInfo.isDeveloperMode());
        } else {
            logger.error("Failed to load telemetry channel");
            return;
        }

        // Create the telemetry client and cache it for later use
        logger.debug("AppInsights telemetry initialized"); //$NON-NLS-1$
        logger.debug(MessageFormat.format(
                "    Developer Mode: {0}", TfsTelemetryInstrumentationInfo.isDeveloperMode())); //$NON-NLS-1$
        logger.debug(MessageFormat.format(
                "    Production Environment: {0}", !TfsTelemetryInstrumentationInfo.isTestKey())); //$NON-NLS-1$

        telemetryClient = new TelemetryClient();
        telemetryClient.getContext().getSession().setId(UUID.randomUUID().toString());
    }

    /**
     * Call sendMetric to track the new value of the named metric.
     *
     * @param name  is the name of the metric to be tracked.
     * @param value is the current value of the metric as a double.
     */
    public void sendMetric(final String name, final double value) {
        // Log that the event occurred (this log is used in testing)
        logger.debug(String.format("sendMetric(%s, %f)", name, value));

        if (telemetryClient != null) {
            telemetryClient.trackMetric(name, value);
        }
    }

    /**
     * Call sendDialogOpened when a dialog is opened that you want to track telemetry for
     *
     * @param name       is the name of the dialog to be tracked.
     * @param properties are additional properties to track with the event.
     */
    public void sendDialogOpened(final String name, final Map<String, String> properties) {
        final PropertyMapBuilder builder = new PropertyMapBuilder(properties);
        final String pageName = String.format(TfsTelemetryConstants.DIALOG_PAGE_VIEW_NAME_FORMAT, name);

        // Log that the event occurred (this log is used in testing)
        logger.debug(String.format("sendDialogOpened(%s, %s)", pageName, builder.toString()));

        if (telemetryClient != null) {
            // Create the page view telemetry object to pass into the track page view method
            final PageViewTelemetry telemetry = new PageViewTelemetry(pageName);
            telemetry.getProperties().putAll(builder.build());
            telemetryClient.trackPageView(telemetry);
        }
    }

    /**
     * Call sendEvent to track an occurrence of a named event.
     *
     * @param name       is the name of the event to be tracked.
     * @param properties are additional properties to track with the event.
     */
    public void sendEvent(final String name, final Map<String, String> properties) {
        final String eventName = String.format(TfsTelemetryConstants.PLUGIN_ACTION_EVENT_NAME_FORMAT, name);
        final PropertyMapBuilder builder = new PropertyMapBuilder(properties);

        // Log that the event occurred (this log is used in testing)
        logger.debug(String.format("sendEvent(%s, %s)", name, builder.toString()));

        if (telemetryClient != null) {
            telemetryClient.trackEvent(eventName, builder.build(), null);
        }
    }

    /**
     * Call sendSessionBegins when the plugin is loaded.
     */
    public void sendSessionBegins() {
        sendSessionState(SessionState.Start);
    }

    /**
     * Call sendSessionEnds when the plugin is unloaded.
     */
    public void sendSessionEnds() {
        sendSessionState(SessionState.End);
    }

    /**
     * Call sendException to track an exception that occurred that should be tracked.
     *
     * @param exception is the exception to track.
     */
    public void sendException(final Exception exception, final Map<String, String> properties) {
        final PropertyMapBuilder builder = new PropertyMapBuilder(properties);

        // Log that the event occurred (this log is used in testing)
        logger.debug(String.format("sendException(%s, %s)", exception.getMessage(), builder.toString()));

        if (telemetryClient != null) {
            telemetryClient.trackException(exception, builder.build(), null);
        }
    }

    private void sendSessionState(final SessionState state) {
        // Log that the event occurred (this log is used in testing)
        logger.debug(String.format("sendSessionState(%s)", state.toString()));

        if (telemetryClient != null) {
            telemetryClient.trackSessionState(state);
        }
    }

    public static class PropertyMapBuilder {
        public static final Map<String, String> EMPTY = new PropertyMapBuilder().build();

        private Map<String, String> properties = new HashMap<String, String>();

        public PropertyMapBuilder() {
            this(null);
        }

        public PropertyMapBuilder(final Map<String, String> properties) {
            if (properties != null) {
                this.properties = new HashMap<String, String>(properties);
            } else {
                this.properties = new HashMap<String, String>();
            }
        }

        public Map<String, String> build() {
            // Make a copy and return it
            return new HashMap<String, String>(properties);
        }

        public PropertyMapBuilder serverContext(final ServerContext context) {
            if (context != null) {
                final boolean isHosted = (context.getType() == ServerContext.Type.VSO) ||
                        (context.getType() == ServerContext.Type.VSO_DEPLOYMENT);
                properties.put(TfsTelemetryConstants.SHARED_PROPERTY_IS_HOSTED, Boolean.toString(isHosted));
                properties.put(TfsTelemetryConstants.SHARED_PROPERTY_SERVER_ID, getServerId(context));
                properties.put(TfsTelemetryConstants.SHARED_PROPERTY_COLLECTION_ID, getCollectionId(context));
            }
            return this;
        }

        public PropertyMapBuilder activeServerContext() {
            if (ServerContextManager.getInstance().getActiveContext() != ServerContext.NO_CONTEXT) {
                return serverContext(ServerContextManager.getInstance().getActiveContext());
            }
            return this;
        }

        public PropertyMapBuilder currentOrActiveContext(final ServerContext context) {
            if (context != null) {
                return serverContext(context);
            } else {
                return activeServerContext();
            }
        }

        public PropertyMapBuilder actionName(final String actionName) {
            if (!StringUtils.isEmpty(actionName)) {
                properties.put(TfsTelemetryConstants.PLUGIN_EVENT_PROPERTY_COMMAND_NAME, actionName);
            }
            return this;
        }

        public PropertyMapBuilder success(final Boolean success) {
            if (success != null) {
                properties.put(TfsTelemetryConstants.PLUGIN_EVENT_PROPERTY_IS_SUCCESS, success.toString());
            }
            return this;
        }

        public PropertyMapBuilder message(final String message) {
            if (!StringUtils.isEmpty(message)) {
                properties.put(TfsTelemetryConstants.PLUGIN_EVENT_PROPERTY_MESSAGE, message);
            }
            return this;
        }

        public PropertyMapBuilder pair(final String key, final String value) {
            if (!StringUtils.isEmpty(key) && !StringUtils.isEmpty(value)) {
                properties.put(key, value);
            }
            return this;
        }

        private String getServerId(final ServerContext context) {
            if (context != null) {
                if (context.getAccount() != null && context.getAccount().getAccountId() != null) {
                    return context.getAccount().getAccountId().toString();
                } else if (context.getUri() != null) {
                    return context.getUri().getHost();
                }
            }

            return UNKNOWN;
        }

        private String getCollectionId(final ServerContext context) {
            if (context != null &&
                    context.getTeamProjectCollectionReference() != null &&
                    context.getTeamProjectCollectionReference().getId() != null) {
                return context.getTeamProjectCollectionReference().getId().toString();
            }

            return UNKNOWN;
        }

        @Override
        public String toString() {
            return properties.toString();
        }
    }
}
