package org.alliance.ui.util;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.event.*;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2007-feb-18
 * Time: 13:02:04
 * To change this template use File | Settings | File Templates.
 */
public class CutCopyPastePopup extends JPopupMenu implements ActionListener {
	private JTextComponent target;

    public CutCopyPastePopup(JTextComponent target) {
        this.target = target;

        JMenuItem mi = new JMenuItem("Cut");
        mi.addActionListener(this);
        mi.setActionCommand("cut");
        add(mi);

        mi = new JMenuItem("Copy");
        mi.addActionListener(this);
        mi.setActionCommand("copy");
        add(mi);

        mi = new JMenuItem("Paste");
        mi.addActionListener(this);
        mi.setActionCommand("paste");
        add(mi);

        add(new JSeparator());

        mi = new JMenuItem("Select all");
        mi.addActionListener(this);
        mi.setActionCommand("selectall");
        add(mi);

        target.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) show(e.getComponent(), e.getX(), e.getY());
            }
        });
    }

    public void actionPerformed(ActionEvent e) {
        if ("cut".equals(e.getActionCommand())) {
            target.cut();
        } else if ("copy".equals(e.getActionCommand())) {
            target.copy();
        } else if ("paste".equals(e.getActionCommand())) {
            target.paste();
        } else if ("selectall".equals(e.getActionCommand())) {
            target.selectAll();
        }
    }
}
