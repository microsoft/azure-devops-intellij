// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common.mocks;

import org.junit.Assert;

import java.util.Observable;
import java.util.Observer;

public class MockObserver implements Observer {
    private Observable lastObservable = null;
    private Object lastArg = null;

    public MockObserver(Observable model) {
        model.addObserver(this);
    }

    public Observable getLastObservable() {
        return lastObservable;
    }

    public Object getLastArg() {
        return lastArg;
    }

    public void clearLastObservation() {
        lastObservable = null;
        lastArg = null;
    }

    public void assertAndClearLastUpdate(Observable expectedLastObservable, Object expectedLastArg) {
        Assert.assertEquals(expectedLastObservable, getLastObservable());
        Assert.assertEquals(expectedLastArg, getLastArg());
        clearLastObservation();
    }

    @Override
    public void update(Observable o, Object arg) {
        lastObservable = o;
        lastArg = arg;
    }
}
