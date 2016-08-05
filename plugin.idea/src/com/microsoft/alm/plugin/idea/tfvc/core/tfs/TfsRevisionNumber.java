// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core.tfs;

import com.intellij.openapi.vcs.history.VcsRevisionNumber;

/**
 * TODO: leantk- this number needs to be made unique and it currently isn't
 */
public class TfsRevisionNumber extends VcsRevisionNumber.Int {

    private static final String SEPARATOR = ":";

    private final String myItemId;

    public TfsRevisionNumber(final int value, final String itemId) {
        super(value);
        myItemId = itemId;
    }

    @Override
    public String asString() {
        return String.valueOf(getValue()) + SEPARATOR + myItemId;
    }

    public String getItemId() {
        return myItemId;
    }

    public static VcsRevisionNumber tryParse(final String s) {
        try {
            int i = s.indexOf(SEPARATOR);
            if (i != -1) {
                String revisionNumberString = s.substring(0, i);
                String itemIdString = s.substring(i + 1);
                int revisionNumber = Integer.parseInt(revisionNumberString);
                return new TfsRevisionNumber(revisionNumber, itemIdString);
            } else {
                int revisionNumber = Integer.parseInt(s);
                return new TfsRevisionNumber(revisionNumber, "");
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String getChangesetString() {
        return String.valueOf(getValue());
    }
}
