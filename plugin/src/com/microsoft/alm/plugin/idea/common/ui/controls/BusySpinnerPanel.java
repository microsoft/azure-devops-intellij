// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.controls;

import com.intellij.openapi.Disposable;
import com.intellij.ui.JBColor;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * This class draws a spinning ring of lines.
 * You can start and stop the spinning with start() and stop().
 * You should stop the spinning if the control isn't visible.
 */
public class BusySpinnerPanel extends JPanel implements Disposable {
    private int rotationAngle = 0;
    private Timer timer;

    public BusySpinnerPanel() {
        timer = new Timer(40, new TimerListener(this));
        timer.setRepeats(true);
        timer.setInitialDelay(40);
        timer.setDelay(40);
        start(false);
    }

    /**
     * Starts the spinning.
     * The spinning is started by default. So, you may not need to call this method unless you call stop.
     * Set show to true to automatically set visible to true
     */
    public void start(boolean show) {
        if (show) {
            setVisible(true);
        }
        timer.start();
    }

    /**
     * Stops the spinning. This is a good thing to do if your spinner won't be shown all the time. You should stop
     * it while it is not visible.
     * Set hide to true to automatically set visible to false
     */
    public void stop(boolean hide) {
        if (hide) {
            setVisible(false);
        }
        timer.stop();
    }

    @Override
    public void dispose() {
        timer.stop();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        // get width and height
        final int width = getWidth();
        final int height = getHeight();

        // Draw 12 lines around the center point in a circle
        int lineLength = Math.min(width, height) / 5;
        int lineWidth = lineLength / 4;
        int cx = width / 2;
        int cy = height / 2;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setPaint(JBColor.black);
        g2.rotate(Math.PI * rotationAngle / 180, cx, cy);
        for (int i = 0; i < 12; i++) {
            g2.drawLine(cx + lineLength, cy, cx + lineLength * 2, cy);
            g2.rotate(-Math.PI / 6, cx, cy);
            g2.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, ((11 - i) / 12.0f) * (2.0f / 3.0f)));
        }
        g2.dispose();
    }

    private static class TimerListener implements ActionListener {

        private BusySpinnerPanel owner;

        public TimerListener(BusySpinnerPanel owner) {
            this.owner = owner;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            this.owner.rotationAngle += 30;
            if (owner.rotationAngle >= 360) {
                owner.rotationAngle = 0;
            }
            this.owner.repaint();
        }
    }
}
