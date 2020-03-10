// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.exceptions;


import com.microsoft.alm.plugin.exceptions.LocalizedException;

/**
 * This is the base class for exceptions thrown by the com.microsoft.alm.plugin.external package.
 * It implements the LocalizedException interface to make sure the exceptions can be localized.
 */
public class ToolException extends RuntimeException implements LocalizedException {

    private String messageKey;

    @Override
    public String getMessageKey() {
        return this.messageKey;
    }

    @Override
    public String[] getMessageParameters() {
        return new String[0];
    }

    public ToolException(final String key) {
        super(key);
        this.messageKey = key;
    }

    public ToolException(final String key, final Throwable t) {
        super(key, t);
        this.messageKey = key;
    }

    // Keys for tool exception messages
    public static String KEY_TF_DOLLAR_IN_PATH = "KEY_TF_DOLLAR_IN_PATH";
    public static String KEY_TF_HOME_NOT_SET = "KEY_TF_HOME_NOT_SET";
    public static String KEY_TF_EXE_NOT_FOUND = "KEY_TF_EXE_NOT_FOUND";
    public static String KEY_TF_BAD_EXIT_CODE = "KEY_TF_BAD_EXIT_CODE";
    public static String KEY_TF_NO_PENDING_CHANGES_FOUND = "KEY_TF_NO_PENDING_CHANGES_FOUND";
    public static String KEY_TF_PARSE_FAILURE = "KEY_TF_PARSE_FAILURE";
    public static String KEY_TF_MIN_VERSION_WARNING = "KEY_TF_MIN_VERSION_WARNING";
    public static String KEY_TF_WORKSPACE_COULD_NOT_BE_DETERMINED = "KEY_TF_WORKSPACE_COULD_NOT_BE_DETERMINED";
    public static String KEY_TF_WORKSPACE_EXISTS = "KEY_TF_WORKSPACE_EXISTS";
    public static String KEY_TF_BRANCH_EXISTS = "KEY_TF_BRANCH_EXISTS";
    public static String KEY_TF_OOM = "KEY_TF_OOM";
    public static String KEY_TF_AUTH_FAIL = "KEY_TF_AUTH_FAIL";
    public static String KEY_TF_VS_MIN_VERSION_WARNING = "KEY_TF_VS_MIN_VERSION_WARNING";
}
