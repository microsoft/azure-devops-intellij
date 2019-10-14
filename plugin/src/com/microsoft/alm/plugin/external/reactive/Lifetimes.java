// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.reactive;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.jetbrains.rd.util.lifetime.Lifetime;
import com.jetbrains.rd.util.lifetime.LifetimeDefinition;
import com.jetbrains.rd.util.lifetime.LifetimeStatus;

public class Lifetimes {
    public static Lifetime defineNestedLifetime(Disposable disposable) {
        LifetimeDefinition lifetimeDefinition = new LifetimeDefinition();
        Disposer.register(disposable, () -> {
            if (lifetimeDefinition.getStatus() == LifetimeStatus.Alive)
                lifetimeDefinition.terminate(false);
        });
        return lifetimeDefinition;
    }

    public static Disposable toDisposable(Lifetime lifetime) {
        Disposable disposable = () -> { };
        lifetime.onTerminationIfAlive(() -> Disposer.dispose(disposable));
        return disposable;
    }
}
