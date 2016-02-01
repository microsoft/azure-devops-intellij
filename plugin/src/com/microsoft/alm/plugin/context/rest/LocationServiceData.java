// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LocationServiceData {
    private String defaultAccessMappingMoniker;
    private int lastChangeId;
    private ServiceDefinition[] serviceDefinitions;

    public String getDefaultAccessMappingMoniker() {
        return defaultAccessMappingMoniker;
    }

    public void setDefaultAccessMappingMoniker(String defaultAccessMappingMoniker) {
        this.defaultAccessMappingMoniker = defaultAccessMappingMoniker;
    }

    public int getLastChangeId() {
        return lastChangeId;
    }

    public void setLastChangeId(int lastChangeId) {
        this.lastChangeId = lastChangeId;
    }

    public ServiceDefinition[] getServiceDefinitions() {
        return serviceDefinitions;
    }

    public void setServiceDefinitions(ServiceDefinition[] serviceDefinitions) {
        this.serviceDefinitions = serviceDefinitions;
    }
}
