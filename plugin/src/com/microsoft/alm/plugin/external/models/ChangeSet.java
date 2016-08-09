// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChangeSet {
    private final String id;
    private final String owner;
    private final String committer;
    private final String date;
    private final String comment;
    private final List<PendingChange> changes;

    public ChangeSet(final String id, final String owner, final String committer, final String date,
                     final String comment, final List<PendingChange> changes) {
        this.id = id;
        this.owner = owner;
        this.committer = committer;
        this.date = date;
        this.comment = comment;
        this.changes = new ArrayList<PendingChange>(changes);
    }

    public String getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public String getCommitter() {
        return committer;
    }

    public String getDate() {
        return date;
    }

    public String getComment() {
        return comment;
    }

    public List<PendingChange> getChanges() {
        return Collections.unmodifiableList(changes);
    }
}

