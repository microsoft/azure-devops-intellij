// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core.tfs;

import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create a unique revision number from the file name, revision number, and modification date
 */
public class TfsRevisionNumber extends VcsRevisionNumber.Int {
    private static final Logger logger = LoggerFactory.getLogger(TfsRevisionNumber.class);

    private static final String SEPARATOR = "/";

    private final String fileName;
    private final String modificationDate;

    public TfsRevisionNumber(final int value, final String fileName, final String modificationDate) {
        super(value);
        this.fileName = fileName;
        this.modificationDate = modificationDate;
    }

    @Override
    public String asString() {
        return String.valueOf(getValue()) + SEPARATOR + fileName + SEPARATOR + modificationDate;
    }

    public static VcsRevisionNumber tryParse(final String s) {
        try {
            final String[] sections = StringUtils.split(s, SEPARATOR);
            if (sections == null || sections.length < 3) {
                return null;
            }
            return new TfsRevisionNumber(Integer.parseInt(sections[0]), sections[1], sections[2]);
        } catch (NumberFormatException e) {
            logger.error("Could not parse revision number: " + s, e);
            return null;
        }
    }

    public String getChangesetString() {
        return String.valueOf(getValue());
    }
}
