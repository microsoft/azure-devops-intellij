// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.settings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class JsonHelper {
    private static final Logger logger = LoggerFactory.getLogger(JsonHelper.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static <T> String write(final T object) {
        String json = null;
        if (object != null) {
            try {
                json = mapper.writeValueAsString(object);
            } catch (JsonProcessingException e) {
                logger.warn("Failed to convert to string", e);
            }
        }
        return json;
    }

    public static <T> T read(final String json, final Class<T> valueType) throws IOException {
        T object = null;
        if (json != null) {
            object = mapper.readValue(json, valueType);
        }
        return object;
    }
}
