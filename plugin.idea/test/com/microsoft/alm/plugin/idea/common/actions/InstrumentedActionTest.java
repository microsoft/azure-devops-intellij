// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.telemetry.TfsTelemetryHelper;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class InstrumentedActionTest extends IdeaAbstractTest {
    @Test
    public void testUpdate() {
        // Verify behavior: calling update does not send an event
        AnActionEvent event = null; // Using null since I can't figure out how to create one of these
        String name = "myAction";
        Map<String, String> properties = new HashMap<String, String>();
        TestAction testAction = new TestAction(name);
        testAction.update(event);
        assertLogged("");

        // Verify behavior: exception in update logs the exception
        name = "myAction2";
        testAction = new TestAction(name);
        testAction.setThrowOnUpdate(true);
        testAction.update(event);
        assertLogged("sendException(throwOnUpdate, {VSO.Plugin.Property.Name=myAction2})");
    }

    @Test
    public void testActionPerformed() throws Exception {
        // Verify behavior: calling actionPerformed sends an event
        AnActionEvent event = null; // Using null since I can't figure out how to create one of these
        String name = "myAction";
        Map<String, String> properties = new HashMap<String, String>();
        TestAction testAction = new TestAction(name);
        testAction.actionPerformed(event);
        assertLogged("sendEvent(myAction, {VSO.Plugin.Property.Name=myAction})");

        // Verify behavior: exception in actionPerformed sends the start event and then logs the exception
        name = "myAction2";
        testAction = new TestAction(name);
        testAction.setThrowOnActionPerformed(true);
        testAction.actionPerformed(event);
        assertLogged("sendEvent(myAction2, {VSO.Plugin.Property.Name=myAction2})sendException(throwOnActionPerformed, {VSO.Plugin.Property.Name=myAction2})");
    }

    // This internal class is used to make sure that all methods are tested
    class TestAction extends InstrumentedAction {
        private final String actionName;
        private Map<String, String> properties;
        private boolean throwOnUpdate = false;
        private boolean throwOnActionPerformed = false;

        public TestAction(String actionName) {
            this.actionName = actionName;
        }

        public void setProperties(Map<String, String> properties) {
            this.properties = properties;
        }

        public void setThrowOnUpdate(boolean throwOnUpdate) {
            this.throwOnUpdate = throwOnUpdate;
        }

        public void setThrowOnActionPerformed(boolean throwOnActionPerformed) {
            this.throwOnActionPerformed = throwOnActionPerformed;
        }

        @Override
        protected String getActionName() {
            return actionName;
        }

        @Override
        protected TfsTelemetryHelper.PropertyMapBuilder getContextProperties() {
            TfsTelemetryHelper.PropertyMapBuilder builder = super.getContextProperties();
            if (this.properties != null && this.properties.size() > 0) {
                for (String key : properties.keySet()) {
                    builder.pair(key, properties.get(key));
                }
            }
            return builder;
        }

        @Override
        public void doUpdate(AnActionEvent anActionEvent) {
            if (throwOnUpdate) {
                throw new RuntimeException("throwOnUpdate");
            }
        }

        @Override
        public void doActionPerformed(AnActionEvent anActionEvent) {
            if (throwOnActionPerformed) {
                throw new RuntimeException("throwOnActionPerformed");
            }
        }
    }
}
