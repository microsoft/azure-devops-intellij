// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.idea.tfvc.ui.settings.LicenseKind;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ToolEulaNotAcceptedException extends RuntimeException {

    private final @NotNull LicenseKind myLicenseKind;
    public @NotNull LicenseKind getLicenseKind() {
        return myLicenseKind;
    }

    public ToolEulaNotAcceptedException(@NotNull LicenseKind licenseKind, @NotNull Throwable throwable) {
        this(licenseKind, null, throwable);
    }

    public ToolEulaNotAcceptedException(@NotNull LicenseKind licenseKind, @NotNull String message) {
        this(licenseKind, message, null);
    }

    private ToolEulaNotAcceptedException(@NotNull LicenseKind kind, @Nullable String message, @Nullable Throwable cause) {
        super(message == null ? "EULA not accepted" : message, cause);
        myLicenseKind = kind;
    }
}
