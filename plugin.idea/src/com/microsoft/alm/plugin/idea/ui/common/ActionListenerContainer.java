// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class ActionListenerContainer {
    private java.util.List<ActionListener> actionListeners = new ArrayList<ActionListener>();

    public void triggerEvent(final ActionEvent event) {
        for (ActionListener listener : actionListeners) {
            listener.actionPerformed(event);
        }
    }

    public void triggerEvent(final Object sender, final String event) {
        triggerEvent(new ActionEvent(sender, 1, event));
    }

    public void add(final ActionListener listener) {
        actionListeners.add(listener);
    }

    public void remove(final ActionListener listener) {
        actionListeners.remove(listener);
    }
}
