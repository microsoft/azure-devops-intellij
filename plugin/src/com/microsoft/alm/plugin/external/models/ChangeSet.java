// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.models;

import com.microsoft.alm.common.utils.SystemHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class represents a TFVC changeset (returned by the History command).
 */
public class ChangeSet {
    private final String id;
    private final String owner;
    private final String committer;
    private final String date;
    private final String comment;
    private final List<CheckedInChange> changes;

    public ChangeSet(final String id, final String owner, final String committer, final String date,
                     final String comment, final List<CheckedInChange> changes) {
        this.id = id;
        this.owner = owner;
        this.committer = committer;
        this.date = date;
        this.comment = comment;
        this.changes = new ArrayList<CheckedInChange>(changes);
    }

    public String getId() {
        return id;
    }

    public int getIdAsInt() {
        return SystemHelper.toInt(id, 0);
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

    public List<CheckedInChange> getChanges() {
        return Collections.unmodifiableList(changes);
    }
}