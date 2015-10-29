// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common;

import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import org.junit.Test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ActionListenerContainerTest extends IdeaAbstractTest {

    @Test
    public void testConstructor() {
        ActionListenerContainer container = new ActionListenerContainer();
    }

    @Test
    public void testTriggerEvent() throws Exception {
        ActionListenerContainer container = new ActionListenerContainer();
        MockActionListener listener1 = new MockActionListener();
        MockActionListener listener2 = new MockActionListener();

        // Test with no listeners
        container.triggerEvent(this, null);

        // Test with one listener
        assertFalse(listener1.triggered);
        container.add(listener1);
        container.triggerEvent(this, null);
        assertTrue(listener1.triggered);

        // Test with two listeners
        listener1.triggered = false;
        assertFalse(listener2.triggered);
        container.add(listener2);
        container.triggerEvent(this, null);
        assertTrue(listener1.triggered);
        assertTrue(listener2.triggered);

        // trigger with a specific event
        String testEvent = "testEvent";
        listener1.triggered = false;
        listener2.triggered = false;
        container.triggerEvent(this, testEvent);
        assertTrue(listener1.triggered);
        assertEquals(testEvent, listener1.actionCommand);
        assertTrue(listener2.triggered);
        assertEquals(testEvent, listener2.actionCommand);
    }

    @Test
    public void testRemove() throws Exception {
        ActionListenerContainer container = new ActionListenerContainer();
        MockActionListener listener1 = new MockActionListener();
        MockActionListener listener2 = new MockActionListener();

        // Make sure remove doesn't throw if not found
        container.remove(listener1);

        // Add the listeners
        container.add(listener1);
        container.add(listener2);
        container.triggerEvent(this, null);
        assertTrue(listener1.triggered);
        assertTrue(listener2.triggered);

        // Remove the listeners in order and verify the results
        listener1.triggered = false;
        listener2.triggered = false;
        container.remove(listener1);
        container.triggerEvent(this, null);
        assertFalse(listener1.triggered);
        assertTrue(listener2.triggered);
        listener2.triggered = false;
        container.remove(listener2);
        container.triggerEvent(this, null);
        assertFalse(listener1.triggered);
        assertFalse(listener2.triggered);
    }

    class MockActionListener implements ActionListener {
        public boolean triggered = false;
        public String actionCommand = null;

        @Override
        public void actionPerformed(ActionEvent e) {
            actionCommand = e.getActionCommand();
            triggered = true;
        }
    }
}