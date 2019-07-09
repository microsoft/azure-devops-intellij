// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.exceptions;

import org.jetbrains.annotations.NotNull;

public class DollarInPathException extends ToolException {
    @NotNull
    private final String myServerFilePath;

    public DollarInPathException(@NotNull String fileName) {
        super(KEY_TF_DOLLAR_IN_PATH);
        this.myServerFilePath = fileName;
    }

    @Override
    public String getMessage() {
        return "'$' was found in path component: " + myServerFilePath;
    }

    @NotNull
    public String getServerFilePath() {
        return myServerFilePath;
    }

    @Override
    public String[] getMessageParameters() {
        return new String[]{myServerFilePath};
    }
}
