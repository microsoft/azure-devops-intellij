// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.services;

/**
 * This is for async interactions that are taken care of by the specific IDE
 */
public interface AsyncService {

    void executeOnPooledThread(final Runnable runnable);
}
