// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.starters;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.git.starters.SimpleCheckoutStarter;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SimpleCheckoutStarter.class})
public class VstsStarterTest extends IdeaAbstractTest {
    public static final String VALID_GIT_URL = "https://organization.visualstudio.com/DefaultCollection/_git/TestProject";
    public static final String VALID_URI = SimpleCheckoutStarter.SUB_COMMAND_NAME + "/?url=" + VALID_GIT_URL + "&EncFormat=UTF8";
    public ApplicationStarterBase vstsStarter;

    @Mock
    public SimpleCheckoutStarter mockSimpleCheckoutStarter;

    @Before
    public void setupLocal() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(SimpleCheckoutStarter.class);
        vstsStarter = new VstsStarter();
    }

    @Test
    public void testProcessCommandHappy() {
        List<String> args = new ArrayList<String>(Arrays.asList(SimpleCheckoutStarter.SUB_COMMAND_NAME, VALID_GIT_URL));
        when(SimpleCheckoutStarter.createWithCommandLineArgs(Arrays.asList(VALID_GIT_URL))).thenReturn(mockSimpleCheckoutStarter);
        vstsStarter.processCommand(args);

        verify(mockSimpleCheckoutStarter).processCommand();
    }

    @Test(expected = RuntimeException.class)
    public void testProcessCommandBadCommand() {
        List<String> args = new ArrayList<String>(Arrays.asList("fakeCommand", VALID_GIT_URL));
        vstsStarter.processCommand(args);

        PowerMockito.verifyStatic();
        verify(mockSimpleCheckoutStarter, never()).processCommand();
    }

    @Test
    public void testProcessUriHappy() throws Exception {
        Map<String, String> uriMap = new HashMap<String, String>() {
            {
                put("url", VALID_GIT_URL);
                put("EncFormat", "UTF8");
            }
        };
        when(SimpleCheckoutStarter.createWithUriAttributes(uriMap)).thenReturn(mockSimpleCheckoutStarter);
        vstsStarter.processUri(VALID_URI);

        //with limited refs _optimized
        uriMap = new HashMap<String, String>() {
            {
                put("url", VALID_GIT_URL.replace("_git/", "_git/_optimized/"));
                put("EncFormat", "UTF8");
            }
        };
        when(SimpleCheckoutStarter.createWithUriAttributes(uriMap)).thenReturn(mockSimpleCheckoutStarter);
        vstsStarter.processUri(VALID_URI.replace("_git/", "_git/_optimized/"));

        verify(mockSimpleCheckoutStarter, times(2)).processCommand();
    }

    @Test(expected = RuntimeException.class)
    public void testProcessUriBadCommand() throws Exception {
        vstsStarter.processUri(VALID_URI.replace(SimpleCheckoutStarter.SUB_COMMAND_NAME, "fakeCommand"));

        PowerMockito.verifyStatic();
        verify(mockSimpleCheckoutStarter, never()).processCommand();
    }

    @Test(expected = RuntimeException.class)
    public void testProcessUriBadUri() throws Exception {
        String uri = "checkouturl=https://laa018-test.visualstudio.com/DefaultCollection/_git/TestProject&EncFormat=UTF8";
        vstsStarter.processUri(uri);

        PowerMockito.verifyStatic();
        verify(mockSimpleCheckoutStarter, never()).processCommand();
    }

    @Test
    public void testParseUri() {
        VstsStarter starter = new VstsStarter();

        //standard URI to parse
        Map<String, String> expectedUriMap = new HashMap<String, String>() {
            {
                put("url", VALID_GIT_URL);
                put("EncFormat", "UTF8");
            }
        };
        Map<String,String> actualUriAttributes = starter.parseUri("url=" + VALID_GIT_URL + "&EncFormat=UTF8");
        Assert.assertEquals(expectedUriMap.entrySet() , actualUriAttributes.entrySet());

        //URI with limited ref git url
        expectedUriMap = new HashMap<String, String>() {
            {
                put("url", VALID_GIT_URL.replace("_git/", "_git/_optimized/"));
                put("EncFormat", "UTF8");
            }
        };

        actualUriAttributes = starter.parseUri("url=" + VALID_GIT_URL.replace("_git/", "_git/_optimized/") + "&EncFormat=UTF8");
        Assert.assertEquals(expectedUriMap.entrySet() , actualUriAttributes.entrySet());

        //URI with empty attribute value
        expectedUriMap = new HashMap<String, String>() {
            {
                put("url", StringUtils.EMPTY);
                put("EncFormat", "UTF8");
            }
        };

        actualUriAttributes = starter.parseUri("url=&EncFormat=UTF8");
        Assert.assertEquals(expectedUriMap.entrySet() , actualUriAttributes.entrySet());

        //URI with empty attribute key
        expectedUriMap = new HashMap<String, String>() {
            {
                put(StringUtils.EMPTY, "url" );
                put("EncFormat", "UTF8");
            }
        };

        actualUriAttributes = starter.parseUri("=url&EncFormat=UTF8");
        Assert.assertEquals(expectedUriMap.entrySet() , actualUriAttributes.entrySet());

        //URI with missing = for key value pair
        expectedUriMap = new HashMap<String, String>() {
            {
                put("EncFormat", "UTF8");
            }
        };

        actualUriAttributes = starter.parseUri("url " + VALID_GIT_URL + "&EncFormat=UTF8");
        Assert.assertEquals(expectedUriMap.entrySet() , actualUriAttributes.entrySet());
    }
}
