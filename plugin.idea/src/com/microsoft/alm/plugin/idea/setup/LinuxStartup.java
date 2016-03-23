// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.setup;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/**
 *
 */
public class LinuxStartup {

    /**
     * Setup Linux specific configurations upon plugin launch
     */
    public static void startup() {
        System.out.println("Linux here");
        
    }
}

