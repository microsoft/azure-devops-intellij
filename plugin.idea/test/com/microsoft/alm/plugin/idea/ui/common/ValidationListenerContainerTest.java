// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common;

import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ValidationListenerContainerTest extends IdeaAbstractTest {

    @Test
    public void testConstructor() {
        ValidationListenerContainer container = new ValidationListenerContainer();
    }

    @Test
    public void testDoValidate() {
        ValidationListenerContainer container = new ValidationListenerContainer();
        // make sure validate works when no validation listeners have been added
        assertEquals(null, container.doValidate());

        // Create some validators
        MockValidationListener error = new MockValidationListener("error");
        MockValidationListener success = new MockValidationListener(null);

        // test with one listener
        container.add(error);
        assertEquals(error.error, container.doValidate().message);

        // test with two listeners
        container.add(success);
        assertEquals(error.error, container.doValidate().message);

        // Change the order of the listeners
        ValidationListenerContainer container2 = new ValidationListenerContainer();
        container2.add(success);
        container2.add(error);
        assertEquals(error.error, container2.doValidate().message);
    }

    @Test
    public void testRemove() {
        ValidationListenerContainer container = new ValidationListenerContainer();
        MockValidationListener error1 = new MockValidationListener("error1");
        MockValidationListener error2 = new MockValidationListener("error2");
        MockValidationListener error3 = new MockValidationListener("error3");

        // Add some validators (order is important)
        container.add(error1);
        container.add(error2);
        container.add(error3);
        assertEquals(error1.error, container.doValidate().message);

        // Remove them in order and make sure we get the results we expect
        container.remove(error1);
        assertEquals(error2.error, container.doValidate().message);
        container.remove(error2);
        assertEquals(error3.error, container.doValidate().message);
        container.remove(error3);
        assertEquals(null, container.doValidate());

    }

    class MockValidationListener implements ValidationListener {
        public String error;

        public MockValidationListener(String error) {
            this.error = error;
        }

        @Override
        public ValidationInfo doValidate() {
            if (error != null) {
                return new ValidationInfo(error);
            }
            return null;
        }
    }
}