// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.starters;

import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SimpleCheckoutStarterTest extends IdeaAbstractTest {
    public static final String VALID_GIT_URL = "https://account.visualstudio.com/DefaultCollection/_git/Test  Project!!#";

    @Test
    public void testCreateWithGitUrlHappy() {
        SimpleCheckoutStarter starter = SimpleCheckoutStarter.createWithGitUrl(VALID_GIT_URL, StringUtils.EMPTY);
        Assert.assertEquals(VALID_GIT_URL, starter.getGitUrl());
    }

    @Test
    public void testCreateWithGitUrlHappyLimitedRefs() {
        SimpleCheckoutStarter starter = SimpleCheckoutStarter.createWithGitUrl(VALID_GIT_URL.replace("_git/", "_git/_optimized/"), StringUtils.EMPTY);
        Assert.assertEquals(VALID_GIT_URL.replace("_git/", "_git/_optimized/"), starter.getGitUrl());
    }

    @Test(expected = RuntimeException.class)
    public void testCreateWithGitUrlBadUrl() {
        SimpleCheckoutStarter.createWithGitUrl(VALID_GIT_URL.replace("_git/", ""), StringUtils.EMPTY);
    }

    @Test
    public void testCreateWithCommandLineArgsHappy() {
        SimpleCheckoutStarter starter = SimpleCheckoutStarter.createWithCommandLineArgs(Arrays.asList(VALID_GIT_URL));
        Assert.assertEquals(VALID_GIT_URL, starter.getGitUrl());
    }

    @Test
    public void testCreateWithUriAttributesHappyEncodedUri() throws Exception {
        Map<String, String> uriMap = new HashMap<String, String>() {
            {
                put("url", URLEncoder.encode(VALID_GIT_URL, "UTF8"));
                put("EncFormat", "UTF8");
            }
        };

        SimpleCheckoutStarter starter = SimpleCheckoutStarter.createWithUriAttributes(uriMap);
        Assert.assertEquals(VALID_GIT_URL, starter.getGitUrl());
    }

    @Test
    public void testCreateWithUriAttributesHappyUnEncodedUri() throws Exception {
        Map<String, String> uriMap = new HashMap<String, String>() {
            {
                put("url", VALID_GIT_URL);
            }
        };

        SimpleCheckoutStarter starter = SimpleCheckoutStarter.createWithUriAttributes(uriMap);
        Assert.assertEquals(VALID_GIT_URL, starter.getGitUrl());
    }

    @Test(expected = RuntimeException.class)
    public void testCreateWithUriAttributesNoUrl() throws Exception {
        Map<String, String> uriMap = new HashMap<String, String>() {
            {
                put("arg1", "arg1");
                put("EncFormat", "UTF8");
            }
        };

        SimpleCheckoutStarter.createWithUriAttributes(uriMap);
    }

    @Test(expected = UnsupportedEncodingException.class)
    public void testCreateWithUriAttributesBadEncoding() throws Exception {
        Map<String, String> uriMap = new HashMap<String, String>() {
            {
                put("url", URLEncoder.encode(VALID_GIT_URL, "UTF8"));
                put("EncFormat", "UTF8888");
            }
        };

        SimpleCheckoutStarter.createWithUriAttributes(uriMap);
    }
}
