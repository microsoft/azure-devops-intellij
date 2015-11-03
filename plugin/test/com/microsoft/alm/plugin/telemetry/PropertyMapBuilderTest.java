// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.telemetry;

import com.microsoft.alm.plugin.AbstractTest;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextBuilder;
import com.microsoft.alm.plugin.context.ServerContextManager;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class PropertyMapBuilderTest extends AbstractTest {
    @Test
    public void constructor() {
        // Empty ctor
        final TfsTelemetryHelper.PropertyMapBuilder builder = new TfsTelemetryHelper.PropertyMapBuilder();
        final Map<String, String> map = builder.build();
        Assert.assertEquals(0, map.size());

        // properties passed in
        final Map<String, String> inputMap = new HashMap<String, String>();
        inputMap.put("one", "1");
        inputMap.put("two", "2");
        final TfsTelemetryHelper.PropertyMapBuilder builder2 = new TfsTelemetryHelper.PropertyMapBuilder(inputMap);
        final Map<String, String> map2 = builder2.build();
        Assert.assertEquals(2, map2.size());
        Assert.assertEquals("1", map2.get("one"));
        Assert.assertEquals("2", map2.get("two"));
    }

    @Test
    public void serverContext() {
        // Set the active context
        final URI uri = URI.create("http://server/path");
        final ServerContext context = new ServerContextBuilder().type(ServerContext.Type.TFS).uri(uri).build();

        // Add that to our property map
        final TfsTelemetryHelper.PropertyMapBuilder builder = new TfsTelemetryHelper.PropertyMapBuilder();
        final Map<String, String> map2 = builder.serverContext(context).build();
        Assert.assertEquals(3, map2.size());
        Assert.assertEquals("false", map2.get("VSO.TeamFoundationServer.IsHostedServer"));
        Assert.assertEquals("server", map2.get("VSO.TeamFoundationServer.ServerId"));
        Assert.assertEquals("unknown", map2.get("VSO.TeamFoundationServer.CollectionId"));
    }

    @Test
    public void activeContext() {
        // Set the active context
        final URI uri = URI.create("http://server/path");
        final ServerContext context = new ServerContextBuilder().type(ServerContext.Type.TFS).uri(uri).build();
        ServerContextManager.getInstance().setActiveContext(context);

        // Add that to our property map
        final TfsTelemetryHelper.PropertyMapBuilder builder = new TfsTelemetryHelper.PropertyMapBuilder();
        final Map<String, String> map2 = builder.activeServerContext().build();
        Assert.assertEquals(3, map2.size());
        Assert.assertEquals("false", map2.get("VSO.TeamFoundationServer.IsHostedServer"));
        Assert.assertEquals("server", map2.get("VSO.TeamFoundationServer.ServerId"));
        Assert.assertEquals("unknown", map2.get("VSO.TeamFoundationServer.CollectionId"));
    }

    @Test
    public void currentOrActiveContext() {
        // Set the active context
        final URI uri = URI.create("http://server/path");
        final URI uri2 = URI.create("http://server2/path");
        final ServerContext context = new ServerContextBuilder().type(ServerContext.Type.TFS).uri(uri).build();
        final ServerContext context2 = new ServerContextBuilder().type(ServerContext.Type.TFS).uri(uri2).build();

        // set active context
        ServerContextManager.getInstance().setActiveContext(context);

        // try to use active context
        final TfsTelemetryHelper.PropertyMapBuilder builder = new TfsTelemetryHelper.PropertyMapBuilder();
        final Map<String, String> map = builder.currentOrActiveContext(null).build();
        Assert.assertEquals("server", map.get("VSO.TeamFoundationServer.ServerId"));

        // try to use current context
        final TfsTelemetryHelper.PropertyMapBuilder builder2 = new TfsTelemetryHelper.PropertyMapBuilder();
        final Map<String, String> map2 = builder2.currentOrActiveContext(context2).build();
        Assert.assertEquals("server2", map2.get("VSO.TeamFoundationServer.ServerId"));
    }

    @Test
    public void otherMethods() {
        final TfsTelemetryHelper.PropertyMapBuilder builder = new TfsTelemetryHelper.PropertyMapBuilder();
        final Map<String, String> map =
                builder.actionName("action")
                        .message("message")
                        .pair("key", "value")
                        .success(true)
                        .build();
        Assert.assertEquals(4, map.size());
        Assert.assertEquals("action", map.get(TfsTelemetryConstants.PLUGIN_EVENT_PROPERTY_COMMAND_NAME));
        Assert.assertEquals("message", map.get(TfsTelemetryConstants.PLUGIN_EVENT_PROPERTY_MESSAGE));
        Assert.assertEquals("value", map.get("key"));
        Assert.assertEquals("true", map.get(TfsTelemetryConstants.PLUGIN_EVENT_PROPERTY_IS_SUCCESS));
    }
}
