// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConnectionData {
    private VstsUserInfo authenticatedUser;
    private VstsUserInfo  authorizedUser;
    private UUID instanceId;
    private LocationServiceData locationServiceData;

    public VstsUserInfo getAuthenticatedUser() {
        return authenticatedUser;
    }

    public void setAuthenticatedUser(VstsUserInfo authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
    }

    public LocationServiceData getLocationServiceData() {
        return locationServiceData;
    }

    public void setLocationServiceData(LocationServiceData locationServiceData) {
        this.locationServiceData = locationServiceData;
    }

    public UUID getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(UUID instanceId) {
        this.instanceId = instanceId;
    }

    public VstsUserInfo getAuthorizedUser() {
        return authorizedUser;
    }

    public void setAuthorizedUser(VstsUserInfo authorizedUser) {
        this.authorizedUser = authorizedUser;
    }
}

