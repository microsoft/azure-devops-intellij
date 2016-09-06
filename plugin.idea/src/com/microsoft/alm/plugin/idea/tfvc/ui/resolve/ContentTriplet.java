// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.resolve;

import org.jetbrains.annotations.NotNull;

public class ContentTriplet {
    @NotNull
    public String baseContent;
    @NotNull
    public String serverContent;
    @NotNull
    public String localContent;
}
