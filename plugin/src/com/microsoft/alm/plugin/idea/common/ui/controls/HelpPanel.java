// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.controls;

import com.intellij.ide.IdeTooltip;
import com.intellij.ide.IdeTooltipManager;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.util.ui.JBUI;
import com.microsoft.alm.plugin.idea.common.resources.Icons;
import com.microsoft.alm.plugin.idea.common.ui.common.SwingHelper;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import org.apache.commons.lang.StringUtils;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

public class HelpPanel extends JPanel {
    private static final int INITIAL_DELAY = 1000; // 1 second delay before opening the popup
    private static final int DISMISS_DELAY = 5000; // 5 second delay before closing the popup (once mouse leaves panel)

    private final JLabel helpLabel;
    private final List<HelpPopupCommand> popupCommands = new ArrayList<HelpPopupCommand>();
    private final List<ActionListener> listeners = new ArrayList<ActionListener>();
    private final Timer showTooltipTimer;
    private final Timer hideTooltipTimer;
    private String popupText;
    private HelpIdeTooltip tip;

    public HelpPanel() {
        // Create controls
        helpLabel = new JLabel();
        final IconPanel helpIcon = new IconPanel(Icons.Help);

        // Layout controls
        setLayout(new GridBagLayout());
        SwingHelper.addToGridBag(this, helpLabel, 0, 0, 1, 1, 0, JBUI.scale(4));
        SwingHelper.addToGridBag(this, helpIcon, 1, 0);

        this.addMouseListener(new MouseEventListener());

        showTooltipTimer = new Timer(INITIAL_DELAY, new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                IdeaHelper.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        showToolTip(true);
                    }
                });
            }
        });
        showTooltipTimer.setInitialDelay(INITIAL_DELAY);
        showTooltipTimer.setRepeats(false);

        hideTooltipTimer = new Timer(DISMISS_DELAY, new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                IdeaHelper.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        hideToolTip(true);
                    }
                });
            }
        });
        hideTooltipTimer.setInitialDelay(DISMISS_DELAY);
        hideTooltipTimer.setRepeats(false);
    }

    public void addActionListener(final ActionListener listener) {
        // Hook up all controls that add listeners
        listeners.add(listener);
    }

    public void clearListeners() {
        listeners.clear();
    }

    public String getHelpText() {
        return helpLabel.getText();
    }

    public void setHelpText(final String text) {
        helpLabel.setText(text);
    }

    public String getPopupText() {
        return popupText;
    }

    public void setPopupText(final String popupText) {
        this.popupText = popupText;
    }

    public void addPopupCommand(final String text, final String command) {
        popupCommands.add(new HelpPopupCommand(text, command));
    }

    public void clearPopupCommands() {
        popupCommands.clear();
    }

    private void onPopupCommandEvent(final String command) {
        hideToolTip(true);
        for (final ActionListener listener : listeners) {
            listener.actionPerformed(new ActionEvent(this, 0, command));
        }
    }

    private void toggleToolTip() {
        if (tip == null || !tip.isShowing()) {
            showToolTip(true);
        } else {
            hideToolTip(true);
        }
    }

    private void showToolTip(final boolean immediately) {
        // stop the hiding timer
        hideTooltipTimer.stop();

        if (tip != null && tip.isShowing()) {
            // It is already showing
            return;
        }

        if (tip == null) {
            final HelpToolTip toolTipPanel = new HelpToolTip(popupText, popupCommands, this.getWidth());
            tip = new HelpIdeTooltip(this, new Point(getWidth() / 2, 0), toolTipPanel);
        }
        if (immediately) {
            IdeTooltipManager.getInstance().show(tip, true);
        } else {
            // Start the showTooltipTimer to show the tooltip
            showTooltipTimer.start();
        }
    }

    private void hideToolTip(final boolean immediately) {
        // stop the showing timer
        showTooltipTimer.stop();

        if (tip != null) {
            if (immediately) {
                IdeTooltipManager.getInstance().hide(tip);
            } else {
                // Start the hideTooltipTimer to hide the tooltip
                hideTooltipTimer.start();
            }
        }
    }

    private class HelpPopupCommand {
        private final String text;
        private final String command;

        public HelpPopupCommand(final String text, final String command) {
            this.text = text;
            this.command = command;
        }
    }

    private class HelpIdeTooltip extends IdeTooltip {
        private boolean showing = false;

        public HelpIdeTooltip(final Component component, final Point point, final JComponent tipComponent) {
            super(component, point, tipComponent);
            setCalloutShift(JBUI.scale(5));
            setExplicitClose(true);
            //setOpaque(true);
            setPreferredPosition(Balloon.Position.below);
        }

        @Override
        public boolean canBeDismissedOnTimeout() {
            return false;
        }

        @Override
        protected void onHidden() {
            showing = false;
            super.onHidden();
        }

        @Override
        protected boolean beforeShow() {
            showing = true;
            return super.beforeShow();
        }

        public boolean isShowing() {
            return showing;
        }
    }

    private class HelpToolTip extends JPanel {
        public HelpToolTip(final String wrappingText, final List<HelpPopupCommand> popupCommands, final int preferredWidth) {
            IdeTooltipManager.setColors(this);
            this.setLayout(new GridBagLayout());

            // Unfortunately, we have to calculate the preferredHeight of the wrapping text
            // I am roughly estimating it by getting the font metrics and dividing the full
            // width of the label string by the preferred width passed in.
            // I subtract 20 from the preferred width to help with word wrapping.
            // I add one to the number of lines it gets to provide a blank line before the links
            final JLabel label = new JLabel();
            // We use the HTML tags to get the label to word wrap
            label.setText("<HTML>" + wrappingText + "</HTML>");
            final FontMetrics fm = this.getFontMetrics(label.getFont());
            final int preferredHeight = (int) (fm.getHeight() * (fm.stringWidth(wrappingText) / (double) (preferredWidth - JBUI.scale(20)) + 1));
            label.setPreferredSize(new Dimension(preferredWidth, preferredHeight));
            label.setVerticalAlignment(JLabel.TOP);
            SwingHelper.addToGridBag(this, label, 0, 0);

            int row = 1;
            for (final HelpPopupCommand popupCommand : popupCommands) {
                if (!StringUtils.isEmpty(popupCommand.text)) {
                    final Hyperlink hyperlink = new Hyperlink();
                    hyperlink.setText(popupCommand.text);
                    hyperlink.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(final ActionEvent e) {
                            onPopupCommandEvent(popupCommand.command);
                        }
                    });
                    SwingHelper.addToGridBag(this, hyperlink, 0, row++, 1, 1, JBUI.scale(4), 0);
                }
            }
        }
    }

    private class MouseEventListener implements MouseListener {
        private boolean ignoreNextClick = false;

        @Override
        public void mouseClicked(final MouseEvent e) {
            if (!ignoreNextClick) {
                e.consume();
                toggleToolTip();
                ignoreNextClick = true;
            } else {
                ignoreNextClick = false;
            }
        }

        @Override
        public void mousePressed(final MouseEvent e) {
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
        }

        @Override
        public void mouseEntered(final MouseEvent e) {
            showToolTip(false);
        }

        @Override
        public void mouseExited(final MouseEvent e) {
            hideToolTip(false);
        }
    }
}

