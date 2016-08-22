// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.utils;

import java.io.IOException;
import java.util.List;

public class ProcessHelper {
    public static Process startProcess(List<String> arguments) throws IOException {
        final ProcessBuilder pb = new ProcessBuilder(arguments);
        return pb.start();
    }
}
