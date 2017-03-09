// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.utils;

import com.intellij.ide.ui.laf.darcula.DarculaUIUtil;
import com.intellij.util.net.HttpConfigurable;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * All backwards compatible logic is contained in this class to keep it in a central place to track
 */
public class BackCompatibleUtils {
    private static final Logger logger = LoggerFactory.getLogger(BackCompatibleUtils.class);

    private static final String PROXY_LOGIN_METHOD = "getProxyLogin";
    private static final String PROXY_LOGIN_FIELD = "PROXY_LOGIN";

    /**
     * Find the proxy login username
     *
     * @return
     */
    public static String getProxyLogin() {
        try {
            // try to get login name using method existing in IDEA release 163.1188 and above
            final Method proxyLoginMethod = HttpConfigurable.getInstance().getClass().getDeclaredMethod(PROXY_LOGIN_METHOD);
            return (String) proxyLoginMethod.invoke(HttpConfigurable.getInstance(), null);
        } catch (Exception newImplementationException) {
            try {
                logger.warn("Failed to get proxy login using getProxyLogin() so attempting old way", newImplementationException);
                // try to get login name using global variable existing before IDEA release 163.1188
                final Field proxyLoginField = HttpConfigurable.getInstance().getClass().getDeclaredField(PROXY_LOGIN_FIELD);
                return (String) proxyLoginField.get(HttpConfigurable.getInstance());
            } catch (Exception oldImplementationException) {
                logger.warn("Failed to get proxy login using PROXY_LOGIN field", oldImplementationException);
                return StringUtils.EMPTY;
            }
        }
    }

    /**
     * Executing the DarculaUIUtil.paintFocusRing() method independent of the IDEA version we are on
     *
     * @param g
     * @param x
     * @param y
     * @param width
     * @param height
     */
    public static void paintFocusRing(final Graphics g, final int x, final int y, final int width, final int height) {
        try {
            // trying to paint focus using the IDEA 2017 method
            final Method paintFocusRingMethodNew = DarculaUIUtil.class.getDeclaredMethod("paintFocusRing", Graphics.class, Rectangle.class);
            paintFocusRingMethodNew.invoke(null, g, new Rectangle(x, y, width, height));
        } catch (Exception newImplementationException) {
            try {
                logger.warn("Failed to get DarculaUIUtil.paintFocusRing() new implementation so attempting old way", newImplementationException);
                final Method paintFocusRingMethodOld = DarculaUIUtil.class.getDeclaredMethod("paintFocusRing", Graphics.class,
                        Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE);
                paintFocusRingMethodOld.invoke(null, g, x, y, width, height);
            } catch (Exception oldImplementationException) {
                logger.warn("Failed to find DarculaUIUtil.paintFocusRing() method", oldImplementationException);
            }
        }
    }
}
