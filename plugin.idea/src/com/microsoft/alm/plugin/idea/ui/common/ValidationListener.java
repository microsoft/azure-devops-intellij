// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common;

import com.intellij.openapi.ui.ValidationInfo;

public interface ValidationListener {
    ValidationInfo doValidate();
}
