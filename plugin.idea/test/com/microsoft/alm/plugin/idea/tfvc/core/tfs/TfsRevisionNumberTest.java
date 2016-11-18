// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core.tfs;

import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsRevisionNumber;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TfsRevisionNumberTest extends IdeaAbstractTest {

    @Test
    public void testTryParse_Happy() {
        TfsRevisionNumber actualNumber = new TfsRevisionNumber(1, "fileName", "2016-09-14T16:10:08.487-0400");
        TfsRevisionNumber parsedNumber = TfsRevisionNumber.tryParse(actualNumber.asString());

        assertEquals(actualNumber.getChangesetString(), parsedNumber.getChangesetString());
        assertEquals(actualNumber.getFileName(), parsedNumber.getFileName());
        assertEquals(actualNumber.getModificationDate(), parsedNumber.getModificationDate());
    }

    @Test
    public void testTryParse_MalformedNumber() {
        TfsRevisionNumber parsedNumber = TfsRevisionNumber.tryParse("fileName");
        assertNull(parsedNumber);

        parsedNumber = TfsRevisionNumber.tryParse("1/fileName/");
        assertNull(parsedNumber);

        parsedNumber = TfsRevisionNumber.tryParse("1/fileName//");
        assertNull(parsedNumber);

        parsedNumber = TfsRevisionNumber.tryParse("1/fileName/number/extra");
        assertNull(parsedNumber);
    }
}