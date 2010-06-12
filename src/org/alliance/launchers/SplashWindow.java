package org.alliance.launchers;

import org.alliance.Version;
import org.alliance.core.ResourceSingelton;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;

/**
 * User: maciek
 * Date: 2004-sep-14
 * Time: 10:49:44
 */
public class SplashWindow extends Window implements Runnable, StartupProgressListener {

    private Image image;
    private String statusMessage = "";    

    public SplashWindow() throws Exception {
        super(new Frame());
        image = Toolkit.getDefaultToolkit().getImage(ResourceSingelton.getRl().getResource("gfx/splash.jpg"));
        MediaTracker mt = new MediaTracker(SplashWindow.this);
        mt.addImage(image, 0);
        try {
            mt.waitForAll();
        } catch (InterruptedException e) {
        }
        Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();

        setLocation(ss.width / 2 - image.getWidth(null) / 2,
                ss.height / 2 - image.getHeight(null) / 2);
        setSize(new Dimension(image.getWidth(null), image.getHeight(null)));

        setVisible(true);
        toFront();
        requestFocus();
    }

    @Override
    public void paint(Graphics frontG) {
        Graphics2D g = (Graphics2D) frontG;

        g.drawImage(image, 0, 0, null);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setFont(new Font("Arial Black, Arial", 0, 10));

        g.setColor(Color.white);
        int texty = image.getHeight(null) - 10;
        g.drawString(statusMessage, 10, texty);
        String s = "Version " + Version.VERSION + " build (" + Version.BUILD_NUMBER + ")";
        g.drawString(s, image.getWidth(null) - 10 - g.getFontMetrics().stringWidth(s), texty);     
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    @Override
    public void update(Graphics g) {
        paint(g);
    }

    @Override
    public void run() {
        dispose();
    }

    @Override
    public void updateProgress(String message) {
        this.statusMessage = message + "...";
        if (getGraphics() != null) {
            paint(getGraphics());
        }
    }
}
