// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common;

import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;

public class ModelValidationInfo {
    public final static ModelValidationInfo NO_ERRORS = null;

    final boolean lookupResource;
    final String validationSource;
    final String validationMessageKey;
    final Object[] validationMessageArgs;

    public static ModelValidationInfo createWithMessage(final String message) {
        return new ModelValidationInfo(false, null, message);
    }

    public static ModelValidationInfo createWithResource(final String resourceKey, final Object... arguments) {
        return new ModelValidationInfo(true, null, resourceKey, arguments);
    }

    public static ModelValidationInfo createWithResource(final String source, final String messageKey, final Object... arguments) {
        return new ModelValidationInfo(true, source, messageKey, arguments);
    }

    protected ModelValidationInfo(final boolean lookupResource, final String source, final String messageKey, final Object... arguments) {
        this.lookupResource = lookupResource;
        validationSource = source;
        validationMessageKey = messageKey;
        if(arguments == null || arguments.length == 0) {
            validationMessageArgs = null;
        }
        else {
            validationMessageArgs = arguments;
        }
    }

    public String getValidationMessage() {
        if(lookupResource) {
            if (validationMessageArgs == null || validationMessageArgs.length == 0) {
                return TfPluginBundle.message(validationMessageKey);
            } else {
                return TfPluginBundle.message(validationMessageKey, validationMessageArgs);
            }
        }
        else {
            return validationMessageKey; //Not a resource string, return the key which is the message
        }
    }

    public String getValidationSource() { return validationSource; }

    //for testing
    public String getValidationMessageKey() { return validationMessageKey; }
}
