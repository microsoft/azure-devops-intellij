// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.extensions;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.refactoring.rename.RenamePsiElementProcessor;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.IncorrectOperationException;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.operations.RenameFileDirectory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Overrides the renaming of files so it uses the tf commandline
 */
public class TFSRenamePsiElementProcessor extends RenamePsiElementProcessor {
    public static final Logger logger = LoggerFactory.getLogger(TFSRenamePsiElementProcessor.class);

    /**
     * Only process files and directories for rename
     *
     * @param psiElement
     * @return
     */
    public boolean canProcessElement(@NotNull PsiElement psiElement) {
        return (psiElement instanceof PsiFile || psiElement instanceof PsiDirectory);
    }

    public void renameElement(final PsiElement element, final String newName, final UsageInfo[] usages,
                              @Nullable final RefactoringElementListener listener) throws IncorrectOperationException {
        logger.info("TFSRenamePsiElementProcessor: processing file for rename");
        RenameFileDirectory.execute(element, newName, usages, listener);
    }

    /**
     * Returns the extension name since it is protected in the parent class
     *
     * @return
     */
    public static ExtensionPointName getExtensionPointName() {
        return EP_NAME;
    }
}
