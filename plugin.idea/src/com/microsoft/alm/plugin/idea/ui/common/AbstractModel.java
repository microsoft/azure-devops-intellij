// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common;

import java.util.Observable;

/**
 * This class is a simple base class for all UI Models.
 * It provides one additional method on to of Observable that combines setChanged and Notify.
 */
public class AbstractModel extends Observable {

    protected void setChangedAndNotify(final String propertyName) {
        super.setChanged();
        super.notifyObservers(propertyName);
    }

}
