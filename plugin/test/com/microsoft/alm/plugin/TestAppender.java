// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin;

import org.apache.log4j.Layout;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Test Appender for log4j that allows us to intercept logging calls for validation
 */
public class TestAppender implements org.apache.log4j.Appender {
    private StringBuilder builder = new StringBuilder();

    public String getAndClearLog() {
        String log = builder.toString();
        builder.setLength(0);
        return log;
    }

    @Override
    public void addFilter(Filter filter) {

    }

    @Override
    public Filter getFilter() {
        return null;
    }

    @Override
    public void clearFilters() {

    }

    @Override
    public void close() {

    }

    @Override
    public void doAppend(LoggingEvent loggingEvent) {
        builder.append(loggingEvent.getMessage());
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {

    }

    @Override
    public ErrorHandler getErrorHandler() {
        return null;
    }

    @Override
    public void setLayout(Layout layout) {

    }

    @Override
    public Layout getLayout() {
        return null;
    }

    @Override
    public void setName(String s) {

    }

    @Override
    public boolean requiresLayout() {
        return false;
    }
}
