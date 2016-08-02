// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.common;

import com.intellij.openapi.ui.ValidationInfo;

import java.util.ArrayList;
import java.util.List;

public class ValidationListenerContainer {
    private List<ValidationListener> validationListeners = new ArrayList<ValidationListener>();

    public ValidationInfo doValidate() {
        for (ValidationListener listener : validationListeners) {
            final ValidationInfo info = listener.doValidate();
            if (info != null) {
                return info;
            }
        }

        return null;
    }

    public void add(final ValidationListener listener) {
        validationListeners.add(listener);
    }

    public void remove(final ValidationListener listener) {
        validationListeners.remove(listener);
    }
}
