// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.common;

import com.microsoft.alm.plugin.AbstractTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

public class LoginPageModelTest extends AbstractTest {
    LoginPageModelImpl underTest;
    PageModel mockPageModel;

    @Before
    public void initTest() {
        mockPageModel = Mockito.mock(PageModel.class);
        underTest = new LoginPageModelImpl(mockPageModel) {
            @Override
            public void dispose() {

            }
        };
    }

    @Test
    public void setServerNameTest() {
        underTest.setConnected(true);

        //TFS server name
        underTest.setServerName("myserver");
        assertEquals("http://myserver:8080/tfs", underTest.getServerName());

        //VSTS account
        underTest.setServerName("myorganization.visualstudio.com");
        assertEquals("https://myorganization.visualstudio.com", underTest.getServerName());

        //VSTS test account
        underTest.setServerName("mytestaccount.tfsallin.net");
        assertEquals("https://mytestaccount.tfsallin.net", underTest.getServerName());

        //full url
        underTest.setServerName("https://myserver:8080/tfs/virtualpath");
        assertEquals("https://myserver:8080/tfs/virtualpath", underTest.getServerName());

        //Azure url
        underTest.setServerName("dev.azure.com/organization");
        assertEquals("https://dev.azure.com/organization", underTest.getServerName());

        //Azure full url
        underTest.setServerName("https://dev.azure.com/organization");
        assertEquals("https://dev.azure.com/organization", underTest.getServerName());
    }
}
