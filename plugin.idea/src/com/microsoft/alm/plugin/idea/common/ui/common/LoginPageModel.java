// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.common;

import java.util.Observer;

/**
 * This interface represents the model for either the VSO or TFS "checkout from version control" pages.
 * There is a base class implementation of this interface thru CheckoutPageModelImpl.
 */
public interface LoginPageModel extends PageModel {
    String PROP_CONNECTED = "connected";
    String PROP_AUTHENTICATING = "authenticating";
    String PROP_SERVER_NAME = "serverName";
    String PROP_USER_NAME = "userName";

    String URL_CREATE_ACCOUNT = "https://go.microsoft.com/fwlink/?LinkId=307137&wt.mc_id=o~msft~java~intellij";
    String URL_VSO_JAVA = "http://java.visualstudio.com";
    String DEFAULT_SERVER_FORMAT = "http://%s:8080/tfs";
    String DEFAULT_VSTS_ACCOUNT_FORMAT = "https://%s";

    void addObserver(Observer o);

    boolean isConnected();

    void setConnected(boolean connected);

    void signOut();

    String getUserName();

    void setUserName(String userName);

    boolean isAuthenticating();

    void setAuthenticating(boolean authenticating);

    String getServerName();

    void setServerName(String serverName);

    void gotoLink(String url);

    void dispose();
}
