package org.alliance.ui;

import org.alliance.core.CoreSubsystem;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2007-feb-15
 * Time: 16:49:43
 */
public class JSpeedDiagram extends JComponent {
	private double scale = 400;
    private double diagramr[] = new double[2048];
    private double diagramw[] = new double[2048];

    public JSpeedDiagram() {
        setOpaque(false);
    }

    public Dimension getPreferredSize() {
        return new Dimension(400,30);
    }

    public synchronized void paint(Graphics g) {
        int xlen = getSize().width;
        int ylen = getSize().height;

        scale = getNewScale();

     /*   g.setColor(new Color(82/2,199/2,156/2));
        g.fillRect(0,0,xlen,ylen);*/

        for(int x = 0;x<xlen;x++) {
            int l = (int)((diagramr[x+(diagramr.length-xlen)]/scale)*ylen);
            if (x <= 50) {
                g.setColor(new Color(0,0,0,x));
            }
            g.drawLine(x,ylen-l,x,ylen);
        }

        for(int x = 0;x<xlen;x++) {
            int l = (int)((diagramw[x+(diagramw.length-xlen)]/scale)*ylen);
            if (x <= 50) {
                g.setColor(new Color(0,0,0,x));
            }
            g.drawLine(x,ylen-l,x,ylen);
        }

//        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//        g.setColor(new Color(0,0,0,100));
//        g.setFont(new Font("Arial Black, Arial", 0, 10));
//        g.drawString((int)scale+"KiB/s",2,10);
//        g.drawString("0KiB/s",2,ylen-3);
//        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    private double getNewScale() {
        double max=0;
        for(int i=0;i<diagramr.length;i++)
            if (max < diagramr[i]) max = diagramr[i];

        for(int i=0;i<diagramw.length;i++)
            if (max < diagramw[i]) max = diagramw[i];

        int t = (int)(max/100);
        t = (t+1)*100;
        return t;
    }

    private int counter=0;
    public synchronized void update(CoreSubsystem core) {
        counter++;
        if (counter % 10 == 0) {
            System.arraycopy(diagramr, 1, diagramr, 0, diagramr.length - 1);
            diagramr[diagramr.length-1] = core.getNetworkManager().getBandwidthIn().getCPS()/1024;

            System.arraycopy(diagramw, 1, diagramw, 0, diagramw.length - 1);
            diagramw[diagramw.length-1] = core.getNetworkManager().getBandwidthOut().getCPS()/1024;
            repaint();
        }
    }
}
