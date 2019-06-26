// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.common;

import java.util.List;

public interface PageModel {
    String PROP_ERRORS = "errors";

    void addError(final ModelValidationInfo error);

    void clearErrors();

    List<ModelValidationInfo> getErrors();

    boolean hasErrors();

    ModelValidationInfo validate();
}
