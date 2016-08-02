// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.common;

import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import org.junit.Assert;
import org.junit.Test;

import java.awt.event.ActionEvent;

public class FeedbackActionTest extends IdeaAbstractTest {

    @Test
    public void testActionPerformed() throws Exception {
        FeedbackAction action = new FeedbackAction(null, "context");

        try {
            action.actionPerformed(null);
            Assert.fail("missing assert");
        } catch (AssertionError error) {
            // This is expected
        }

        // Make sure calling the method doesn't throw if the source is not a component
        action.actionPerformed(new ActionEvent(this, 0, null));
    }
}