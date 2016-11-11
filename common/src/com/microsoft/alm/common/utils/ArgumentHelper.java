// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.common.utils;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.List;

/**
 * Checks for illegal arguments and throws IllegalArgumentException if found
 */
public class ArgumentHelper {
    private static final String EMPTY_ARG_MSG = "%s is empty";
    private static final String NOT_FILE_MSG = "%s expected to be a file";
    private static final String NOT_WRITEABLE_FILE_MSG = "%s must be writable";

    public static void checkNotNull(final Object arg, final String argName) {
        if (arg == null) {
            throw new IllegalArgumentException(argName);
        }
    }

    public static void checkNotNullOrEmpty(final List arg, final String argName) {
        checkNotNull(arg, argName);

        if (arg.isEmpty()) {
            throw new IllegalArgumentException(String.format(EMPTY_ARG_MSG, argName));
        }
    }

    public static void checkNotEmptyString(final String arg, final String argName) {
        if (StringUtils.isEmpty(arg)) {
            throw new IllegalArgumentException(String.format(EMPTY_ARG_MSG, argName));
        }
    }

    public static void checkIfFile(final File file) {
        checkNotNull(file, "file");
        if (file.isDirectory()) {
            throw new IllegalArgumentException(String.format(NOT_FILE_MSG, file.getPath()));
        }
    }

    public static void checkIfFileWriteable(final File file) {
        if (!file.canWrite()) {
            throw new IllegalArgumentException(String.format(NOT_WRITEABLE_FILE_MSG, file.getPath()));
        }
    }
}
