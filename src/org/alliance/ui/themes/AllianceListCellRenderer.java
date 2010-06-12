package org.alliance.ui.themes;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import org.pushingpixels.substance.api.renderers.SubstanceDefaultListCellRenderer;

/**
 *
 * @author Bastvera
 */
public class AllianceListCellRenderer extends SubstanceDefaultListCellRenderer {

    public DefaultListCellRenderer renderer;

    public AllianceListCellRenderer(boolean substanceInUse) {
        if (substanceInUse) {
            renderer = new SubstanceDefaultListCellRenderer();
        } else {
            renderer = new DefaultListCellRenderer();
        }
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        return renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    }
}
