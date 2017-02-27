// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.servertree;

import com.intellij.openapi.util.Condition;
import com.microsoft.alm.core.webapi.model.TeamProjectReference;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.rest.TfvcHttpClientEx;
import com.microsoft.alm.plugin.context.rest.VersionControlRecursionTypeCaseSensitive;
import com.microsoft.alm.plugin.idea.tfvc.exceptions.TfsException;
import com.microsoft.alm.sourcecontrol.webapi.model.TfvcItem;
import com.microsoft.alm.sourcecontrol.webapi.model.TfvcVersionDescriptor;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class TfsTreeContextTest {
    private TfsTreeContext tfsTreeContext;

    private TfvcItem item1 = new TfvcItem();
    private TfvcItem item2 = new TfvcItem();
    private TfvcItem item3 = new TfvcItem();
    private TfvcItem item4 = new TfvcItem();
    private TfvcItem item5 = new TfvcItem();

    @Mock
    private ServerContext mockServerContext;
    @Mock
    private TeamProjectReference mockTeamProjectReference;
    @Mock
    private TfvcHttpClientEx mockTfvcHttpClientEx;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testIsAccepted_NoFilter() {
        tfsTreeContext = new TfsTreeContext(mockServerContext, true, null);
        assertTrue(tfsTreeContext.isAccepted("path"));
    }

    @Test
    public void testIsAccepted_True() {
        tfsTreeContext = new TfsTreeContext(mockServerContext, true, new Condition<String>() {
            @Override
            public boolean value(String s) {
                return StringUtils.isNotEmpty(s);
            }
        });
        assertTrue(tfsTreeContext.isAccepted("not empty"));
    }

    @Test
    public void testIsAccepted_False() {
        tfsTreeContext = new TfsTreeContext(mockServerContext, true, new Condition<String>() {
            @Override
            public boolean value(String s) {
                return StringUtils.isNotEmpty(s);
            }
        });
        assertFalse(tfsTreeContext.isAccepted(StringUtils.EMPTY));
    }

    @Test(expected = TfsException.class)
    public void testGetChildItems_ContextNull() throws Exception {
        tfsTreeContext = new TfsTreeContext(null, true, null);
        tfsTreeContext.getChildItems("path");
    }

    @Test(expected = TfsException.class)
    public void testGetChildItems_TeamRefNull() throws Exception {
        when(mockServerContext.getTeamProjectReference()).thenReturn(null);
        tfsTreeContext = new TfsTreeContext(null, true, null);
        tfsTreeContext.getChildItems("path");
    }

    @Test
    public void testGetChildItems_AllChildren() throws Exception {
        setChildItemsTest();
        tfsTreeContext = new TfsTreeContext(mockServerContext, false, null);
        List<TfvcItem> returnedItems = tfsTreeContext.getChildItems("$/root");
        assertEquals(4, returnedItems.size());
        assertEquals(item1.getPath(), returnedItems.get(0).getPath());
        assertEquals(item2.getPath(), returnedItems.get(1).getPath());
        assertEquals(item4.getPath(), returnedItems.get(2).getPath());
        assertEquals(item5.getPath(), returnedItems.get(3).getPath());
    }

    @Test
    public void testGetChildItems_FoldersOnly() throws Exception {
        setChildItemsTest();
        tfsTreeContext = new TfsTreeContext(mockServerContext, true, null);
        List<TfvcItem> returnedItems = tfsTreeContext.getChildItems("$/root");
        assertEquals(2, returnedItems.size());
        assertEquals(item1.getPath(), returnedItems.get(0).getPath());
        assertEquals(item4.getPath(), returnedItems.get(1).getPath());
    }

    @Test(expected = TfsException.class)
    public void testGetChildItems_BadRoot() throws Exception {
        setChildItemsTest();
        when(mockTfvcHttpClientEx.getItems(any(UUID.class), eq("$/badRoot"), eq(VersionControlRecursionTypeCaseSensitive.ONE_LEVEL),
                any(TfvcVersionDescriptor.class))).thenThrow(AssertionError.class);


        tfsTreeContext = new TfsTreeContext(mockServerContext, true, null);
        tfsTreeContext.getChildItems("$/badRoot");
    }

    @Test(expected = TfsException.class)
    public void testGetChildItems_RuntimeException() throws Exception {
        setChildItemsTest();
        when(mockTfvcHttpClientEx.getItems(any(UUID.class), eq("$/root"), eq(VersionControlRecursionTypeCaseSensitive.ONE_LEVEL),
                any(TfvcVersionDescriptor.class))).thenThrow(RuntimeException.class);

        tfsTreeContext = new TfsTreeContext(mockServerContext, true, null);
        tfsTreeContext.getChildItems("$/root");
    }

    private void setChildItemsTest() {
        item1.setPath("$/root/directory1");
        item1.setFolder(true);
        item2.setPath("$/root/file2.txt");
        item2.setFolder(false);
        item3.setPath("$/root");
        item3.setFolder(true);
        item4.setPath("$/root/directory4");
        item4.setFolder(true);
        item5.setPath("$/root/file5.txt");
        item5.setFolder(false);

        List<TfvcItem> items = new ArrayList<TfvcItem>(Arrays.asList(item1, item2, item3, item4, item5));
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000000");
        when(mockTfvcHttpClientEx.getItems(eq(id), eq("$/root"), eq(VersionControlRecursionTypeCaseSensitive.ONE_LEVEL),
                any(TfvcVersionDescriptor.class))).thenReturn(items);
        when(mockTeamProjectReference.getId()).thenReturn(id);
        when(mockServerContext.getTeamProjectReference()).thenReturn(mockTeamProjectReference);
        when(mockServerContext.getTfvcHttpClient()).thenReturn(mockTfvcHttpClientEx);
    }
}
