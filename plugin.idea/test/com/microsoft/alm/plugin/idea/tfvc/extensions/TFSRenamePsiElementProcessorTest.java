// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.extensions;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class TFSRenamePsiElementProcessorTest extends IdeaAbstractTest {
    private TFSRenamePsiElementProcessor tfsRenamePsiElementProcessor;

    @Before
    public void setUp() {
        tfsRenamePsiElementProcessor = new TFSRenamePsiElementProcessor();
    }

    @Test
    public void testCanProcessElement_File() {
        PsiFile mockFile = mock(PsiFile.class);
        assertTrue(tfsRenamePsiElementProcessor.canProcessElement(mockFile));
    }

    @Test
    public void testCanProcessElement_Directory() {
        PsiDirectory mockDirectory = mock(PsiDirectory.class);
        assertTrue(tfsRenamePsiElementProcessor.canProcessElement(mockDirectory));
    }

    @Test
    public void testCanProcessElement_False() {
        PsiClass mockClass = mock(PsiClass.class);
        assertFalse(tfsRenamePsiElementProcessor.canProcessElement(mockClass));

        PsiMethod mockmethod = mock(PsiMethod.class);
        assertFalse(tfsRenamePsiElementProcessor.canProcessElement(mockmethod));

        PsiParameter mockParameter = mock(PsiParameter.class);
        assertFalse(tfsRenamePsiElementProcessor.canProcessElement(mockParameter));
    }
}
