// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.settings;

public class PropertyState {
    public PropertyState() {
    }

    public PropertyState(final String name, final String value) {
        this.name = name;
        this.value = value;
    }

    public String name;
    public String value;
}
