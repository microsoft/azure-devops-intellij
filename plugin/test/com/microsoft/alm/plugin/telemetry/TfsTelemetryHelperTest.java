// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.telemetry;

import com.microsoft.alm.plugin.AbstractTest;
import com.microsoft.applicationinsights.telemetry.SessionState;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class TfsTelemetryHelperTest extends AbstractTest {

    @Test
    public void testSendMetric() {
        String name = "myMetric";
        double value = 99.9;
        TfsTelemetryHelper.getInstance().sendMetric(name, value);
        assertLogged(String.format("sendMetric(%s, %f)", name, value));
    }

    @Test
    public void testSendEvent() {
        // Verify behavior: name only
        String name = "myEvent";
        Map<String, String> properties = new HashMap<String, String>();
        TfsTelemetryHelper.getInstance().sendEvent(name, null);
        assertLogged(String.format("sendEvent(%s, %s)", name, properties.toString()));

        // Verify behavior: name and properties
        name = "myEvent2";
        properties.put("property1", "value1");
        TfsTelemetryHelper.getInstance().sendEvent(name, properties);
        assertLogged(String.format("sendEvent(%s, %s)", name, properties.toString()));

        // TODO test with connection
    }

    @Test
    public void testSendDialogOpened() {
        // Verify behavior: name only
        String name = "myDialog";
        Map<String, String> properties = new HashMap<String, String>();
        TfsTelemetryHelper.getInstance().sendDialogOpened(name, null);
        assertLogged(String.format("sendDialogOpened(%s, %s)",
                String.format(TfsTelemetryConstants.DIALOG_PAGE_VIEW_NAME_FORMAT, name), properties.toString()));

        // Verify behavior: name and properties
        name = "myDialog2";
        properties.put("property1", "value1");
        TfsTelemetryHelper.getInstance().sendDialogOpened(name, properties);
        assertLogged(String.format("sendDialogOpened(%s, %s)",
                String.format(TfsTelemetryConstants.DIALOG_PAGE_VIEW_NAME_FORMAT, name), properties.toString()));
    }

    @Test
    public void testSendSessionBegins() {
        TfsTelemetryHelper.getInstance().sendSessionBegins();
        assertLogged(String.format("sendSessionState(%s)", SessionState.Start.toString()));
    }

    @Test
    public void testSendSessionEnds() {
        TfsTelemetryHelper.getInstance().sendSessionEnds();
        assertLogged(String.format("sendSessionState(%s)", SessionState.End.toString()));
    }

    @Test
    public void testSendException() {
        Exception ex = new Exception("Bad stuff happened.");
        TfsTelemetryHelper.getInstance().sendException(ex, null);
        assertLogged(String.format("sendException(%s, {})", ex.getMessage()));
    }
}
