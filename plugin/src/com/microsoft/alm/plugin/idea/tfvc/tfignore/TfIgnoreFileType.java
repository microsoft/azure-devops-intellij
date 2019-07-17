// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.tfignore;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class TfIgnoreFileType extends LanguageFileType {

    TfIgnoreFileType() {
        super(TfIgnoreLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return ".tfignore";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Ignore file for TFVC";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "tfignore";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return AllIcons.FileTypes.Text;
    }
}
