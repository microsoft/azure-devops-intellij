// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

public class ToolEulaNotAcceptedException extends RuntimeException {
    public ToolEulaNotAcceptedException(Throwable throwable) {
        super("EULA not accepted", throwable);
    }
}
