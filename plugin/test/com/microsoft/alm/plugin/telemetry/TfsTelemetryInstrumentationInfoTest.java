// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.telemetry;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class TfsTelemetryInstrumentationInfoTest {
    @Test
    public void testInitialize_null() {
        TfsTelemetryInstrumentationInfo info = new TfsTelemetryInstrumentationInfo();
        info.initialize(null);
        Assert.assertFalse(info.isDeveloperMode());
        Assert.assertFalse(info.isTestKey());
    }

    @Test
    public void testInitialize_emptyStream() {
        TfsTelemetryInstrumentationInfo info = new TfsTelemetryInstrumentationInfo();

        String input = "";
        InputStream stream = new ByteArrayInputStream(input.getBytes());
        info.initialize(stream);
        Assert.assertFalse(info.isDeveloperMode());
        Assert.assertFalse(info.isTestKey());
    }

    @Test
    public void testInitialize_normal() {
        TfsTelemetryInstrumentationInfo info = new TfsTelemetryInstrumentationInfo();

        String input = "telemetry.instrumentation.is_test_environment=true\n" +
                "telemetry.instrumentation.is_developer_mode=true";
        InputStream stream = new ByteArrayInputStream(input.getBytes());
        info.initialize(stream);
        Assert.assertTrue(info.isDeveloperMode());
        Assert.assertTrue(info.isTestKey());
    }

    @Test
    public void testInitialize_other() {
        TfsTelemetryInstrumentationInfo info = new TfsTelemetryInstrumentationInfo();

        // TestEnv == false implies devMode == false
        String input = "telemetry.instrumentation.is_test_environment=false\n" +
                "telemetry.instrumentation.is_developer_mode=true";
        InputStream stream = new ByteArrayInputStream(input.getBytes());
        info.initialize(stream);
        Assert.assertFalse(info.isDeveloperMode());
        Assert.assertFalse(info.isTestKey());

        // TestEnv == true will allow the devMode to be read
        input = "telemetry.instrumentation.is_test_environment=true\n" +
                "telemetry.instrumentation.is_developer_mode=false";
        stream = new ByteArrayInputStream(input.getBytes());
        info.initialize(stream);
        Assert.assertFalse(info.isDeveloperMode());
        Assert.assertTrue(info.isTestKey());
    }

}
