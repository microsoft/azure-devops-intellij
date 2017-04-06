// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.services;

import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import com.microsoft.alm.plugin.services.AsyncService;

/**
 * Async interactions that are specifically handled by IntelliJ
 */
public class IdeaAsyncService implements AsyncService {

    @Override
    public void executeOnPooledThread(final Runnable runnable) {
        IdeaHelper.executeOnPooledThread(runnable);
    }
}