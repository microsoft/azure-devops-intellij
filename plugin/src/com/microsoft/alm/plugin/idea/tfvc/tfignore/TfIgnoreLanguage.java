// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.tfignore;

import com.intellij.lang.Language;

public class TfIgnoreLanguage extends Language { // TODO: Inherit from IgnoreLanguage if we target IDEA 2019.*
    public static TfIgnoreLanguage INSTANCE = new TfIgnoreLanguage();
    private TfIgnoreLanguage() {
        super("TFIgnore");
    }
}
