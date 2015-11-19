// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common;

import com.intellij.util.ui.JBUI;

import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.AWTKeyStroke;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.util.HashSet;
import java.util.Set;

/**
 * This class is a place for static methods that help with Java Swing components.
 */
public class SwingHelper {

    /**
     * This method sets the FocusTraversalKeys for a component to be the standard keys.
     * Use this on Tables or TextAreas where you want the tab keys to leave the control.
     *
     * @param component the component that you want to fix tab keys for
     */
    public static void fixTabKeys(final JComponent component) {
        final Set<AWTKeyStroke> forward = new HashSet<AWTKeyStroke>(
                component.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        forward.add(KeyStroke.getKeyStroke("TAB"));
        component.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, forward);
        final Set<AWTKeyStroke> backward = new HashSet<AWTKeyStroke>(
                component.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
        backward.add(KeyStroke.getKeyStroke("shift TAB"));
        component.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, backward);
    }

    public static void setPreferredHeight(final JComponent component, final int height) {
        final Dimension size = component.getPreferredSize();
        size.setSize(size.getWidth(), JBUI.scale(height));
        component.setPreferredSize(size);
    }

    public static void copyFontAndMargins(final JTextArea target, final JComponent source) {
        final Insets insets = source.getInsets();
        target.setFont(source.getFont());
        target.setMargin(insets);
    }

    public static void setMargin(final JComponent component, final Insets newMargin) {
        final Border currentBorder = component.getBorder();
        final Border empty = new EmptyBorder(newMargin.top, newMargin.left, newMargin.bottom, newMargin.right);
        if (currentBorder == null || currentBorder instanceof EmptyBorder) {
            component.setBorder(empty);
        } else if (currentBorder instanceof CompoundBorder) {
            final CompoundBorder current = (CompoundBorder) currentBorder;
            final Border insideBorder = current.getInsideBorder();
            component.setBorder(new CompoundBorder(empty, insideBorder));
        } else {
            component.setBorder(new CompoundBorder(empty, currentBorder));
        }
    }

    public static void setMaxCharLimit(final JTextArea textField, final int limit) {
        textField.setDocument(new MaxCharLimitDocument(limit));
    }

    private static class MaxCharLimitDocument extends PlainDocument {
        private final int maxCharLimit;

        MaxCharLimitDocument(final int limit) {
            maxCharLimit = limit;
        }

        @Override
        public void insertString(final int offset, final String str, final AttributeSet attr) throws BadLocationException {
            if (str == null) return;

            if (getLength() + str.length() <= maxCharLimit) {
                super.insertString(offset, str, attr);
            }
        }

    }
}
