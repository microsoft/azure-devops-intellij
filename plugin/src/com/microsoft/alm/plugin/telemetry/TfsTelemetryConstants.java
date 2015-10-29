// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.telemetry;

/**
 * This class of constant strings is used by the TfsTelemetryInitializer class.
 */
public class TfsTelemetryConstants {
    public static final String CONTEXT_PROPERTY_USER_ID = "Context.Default.VSO.Core.user.id"; //$NON-NLS-1$

    public static final String CONTEXT_PROPERTY_BUILD_NUMBER = "Context.Default.VSO.Core.BuildNumber"; //$NON-NLS-1$
    public static final String CONTEXT_PROPERTY_MAJOR_VERSION = "Context.Default.VSO.Core.MajorVersion"; //$NON-NLS-1$
    public static final String CONTEXT_PROPERTY_MINOR_VERSION = "Context.Default.VSO.Core.MinorVersion"; //$NON-NLS-1$
    public static final String CONTEXT_PROPERTY_EXE_NAME = "Context.Default.VSO.Core.ExeName"; //$NON-NLS-1$

    public static final String CONTEXT_PROPERTY_PROCESSOR_ARCHITECTURE = "Context.Default.VSO.Core.Machine.Processor.Architecture"; //$NON-NLS-1$
    public static final String CONTEXT_PROPERTY_LOCALE_NAME = "Context.Default.VSO.Core.Locale.SystemLocaleName"; //$NON-NLS-1$
    public static final String CONTEXT_PROPERTY_OS_MAJOR_VERSION = "Context.Default.VSO.Core.OS.MajorVersion"; //$NON-NLS-1$
    public static final String CONTEXT_PROPERTY_OS_MINOR_VERSION = "Context.Default.VSO.Core.OS.MinorVersion"; //$NON-NLS-1$
    public static final String CONTEXT_PROPERTY_OS_NAME = "Context.Default.VSO.Core.OS.Name"; //$NON-NLS-1$
    public static final String CONTEXT_PROPERTY_OS_SHORT_NAME = "Context.Default.VSO.Core.OS.ShortName"; //$NON-NLS-1$
    public static final String CONTEXT_PROPERTY_OS_FULL_NAME = "Context.Default.VSO.Core.OS.FullName"; //$NON-NLS-1$

    public static final String CONTEXT_PROPERTY_JAVA_RUNTIME_NAME = "Context.Default.VSO.Core.Java.Name"; //$NON-NLS-1$
    public static final String CONTEXT_PROPERTY_JAVA_RUNTIME_VERSION = "Context.Default.VSO.Core.Java.Version"; //$NON-NLS-1$

    public static final String SHARED_PROPERTY_IS_HOSTED = "VSO.TeamFoundationServer.IsHostedServer"; //$NON-NLS-1$
    public static final String SHARED_PROPERTY_SERVER_ID = "VSO.TeamFoundationServer.ServerId"; //$NON-NLS-1$
    public static final String SHARED_PROPERTY_COLLECTION_ID = "VSO.TeamFoundationServer.CollectionId"; //$NON-NLS-1$

    public static final String FEEDBACK_PROPERTY_COMMENT = "VSO.Feedback.Comment"; //$NON-NLS-1$
    public static final String FEEDBACK_PROPERTY_EMAIL = "VSO.Feedback.Email"; //$NON-NLS-1$
    public static final String FEEDBACK_PROPERTY_CONTEXT = "VSO.Feedback.Context"; //$NON-NLS-1$

    public static final String PLUGIN_EVENT_PROPERTY_IS_SUCCESS = "VSO.Plugin.Property.Success"; //$NON-NLS-1$
    public static final String PLUGIN_EVENT_PROPERTY_COMMAND_NAME = "VSO.Plugin.Property.Name"; //$NON-NLS-1$
    public static final String PLUGIN_EVENT_PROPERTY_MESSAGE = "VSO.Plugin.Property.Message"; //$NON-NLS-1$
    public static final String PLUGIN_EVENT_PROPERTY_DIALOG = "VSO.Plugin.Property.Dialog"; //$NON-NLS-1$

    public static final String PLUGIN_ACTION_EVENT_NAME_FORMAT = "VSO/Plugin/Action/%s"; //$NON-NLS-1$
    public static final String DIALOG_PAGE_VIEW_NAME_FORMAT = "VSO/Plugin/Dialog/%s"; //$NON-NLS-1$
}
