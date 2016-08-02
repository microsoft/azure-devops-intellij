// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PageModelImpl extends AbstractModel implements PageModel {
    private final List<ModelValidationInfo> errors = new ArrayList<ModelValidationInfo>();

    @Override
    public void clearErrors() {
        if (hasErrors()) {
            errors.clear();
            super.setChangedAndNotify(PROP_ERRORS);
        }
    }

    @Override
    public void addError(final ModelValidationInfo error) {
        errors.add(error);
        super.setChangedAndNotify(PROP_ERRORS);
    }

    @Override
    public List<ModelValidationInfo> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    @Override
    public boolean hasErrors() {
        return errors.size() > 0;
    }

    @Override
    public ModelValidationInfo validate() {
        return ModelValidationInfo.NO_ERRORS;
    }
}
