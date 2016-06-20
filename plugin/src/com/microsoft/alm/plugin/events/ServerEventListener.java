// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.events;

import java.util.EventListener;
import java.util.Map;

public interface ServerEventListener extends EventListener {
    void serverChanged(final ServerEvent event, final Map<String,Object> contextMap);
}
