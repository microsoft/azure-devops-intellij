// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.mocks;

import com.microsoft.alm.plugin.services.HttpProxyService;

public class MockHttpProxyService implements HttpProxyService{

    @Override
    public boolean useHttpProxy() {
        return false;
    }

    @Override
    public boolean isAuthenticationRequired() {
        return false;
    }

    @Override
    public String getProxyURL() {
        return String.format("http://%s:%d", getProxyHost(), getProxyPort());
    }

    @Override
    public String getProxyHost() {
        return "127.0.0.1";
    }

    @Override
    public int getProxyPort() {
        return 8888;
    }

    @Override
    public String getUserName() {
        return null;
    }

    @Override
    public String getPassword() {
        return null;
    }
}
