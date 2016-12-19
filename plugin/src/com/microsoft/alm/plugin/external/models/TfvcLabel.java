// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class represents the details of a TFVC label returned by the TF labels command.
 */
public class TfvcLabel {
    private final String name;
    private final String scope;
    private final String user;
    private final String date;
    private final String comment;
    private final List<Item> items = new ArrayList<Item>();

    public TfvcLabel(final String name, final String scope, final String user, final String date,
                     final String comment, final List<Item> items) {
        this.name = name;
        this.scope = scope;
        this.user = user;
        this.date = date;
        this.comment = comment;
        this.items.addAll(items);
    }

    public String getName() {
        return name;
    }

    public String getScope() {
        return scope;
    }

    public String getUser() {
        return user;
    }

    public String getDate() {
        return date;
    }

    public String getComment() {
        return comment;
    }

    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    public static class Item {
        private final String serverItem;
        private final String changeset;

        public Item(final String serverItem, final String changeset) {
            this.serverItem = serverItem;
            this.changeset = changeset;
        }

        public String getServerItem() {
            return serverItem;
        }

        public String getChangeset() {
            return changeset;
        }
    }
}
