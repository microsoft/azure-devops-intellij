// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.actions;

import com.microsoft.alm.plugin.external.models.ItemInfo;
import com.microsoft.alm.plugin.idea.tfvc.core.TfvcClient;

import java.util.List;

abstract class SimpleMultipleItemAction extends MultipleItemAction<ItemInfo> {

    public SimpleMultipleItemAction(String title, String message) {
        super(title, message);
    }

    @Override
    protected void loadItemInfoCollection(MultipleItemActionContext context, List<String> localPaths) {
        TfvcClient client = TfvcClient.getInstance();
        client.getLocalItemsInfo(context.project, context.serverContext, localPaths, context.itemInfos::add);
    }
}
