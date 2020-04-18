// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import org.junit.After;

public abstract class DisposableTest extends IdeaAbstractTest {
    protected final Disposable testDisposable = Disposer.newDisposable();

    @After
    public void tearDown() {
        Disposer.dispose(testDisposable);
    }
}
