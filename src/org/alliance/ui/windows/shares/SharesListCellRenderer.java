package org.alliance.ui.windows.shares;

import org.alliance.ui.themes.util.SubstanceListCellRendererHelper;
import org.alliance.ui.themes.util.SubstanceThemeHelper;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

/**
 *
 * @author Bastvera
 */
public class SharesListCellRenderer {

    private static Font EVEN_ROW_FONT;
    private static Font ODD_ROW_FONT;
    private static String GROUP_TEXT;
    private DefaultListCellRenderer renderer;

    public SharesListCellRenderer(String groupText) {
        GROUP_TEXT = "     --> " + groupText + " ";
        if (SubstanceThemeHelper.isSubstanceInUse()) {
            renderer = new SubstanceListCellRenderer();
        } else {
            renderer = new NormalListCellRenderer();
        }
        ODD_ROW_FONT = new Font(renderer.getFont().getFontName(), Font.BOLD, renderer.getFont().getSize());
        EVEN_ROW_FONT = new Font(renderer.getFont().getFontName(), renderer.getFont().getStyle(), renderer.getFont().getSize() - 2);
    }

    public DefaultListCellRenderer getRenderer() {
        return renderer;
    }

    private class NormalListCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (index % 2 == 1) {
                value = new String(GROUP_TEXT + value);
            }
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (index % 2 == 1) {
                setFont(EVEN_ROW_FONT);
                setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
            } else {
                setFont(ODD_ROW_FONT);
            }
            return this;
        }
    }

    private class SubstanceListCellRenderer extends SubstanceListCellRendererHelper {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (index % 2 == 1) {
                value = new String(GROUP_TEXT + value);
            }
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (index % 2 == 1) {
                setFont(EVEN_ROW_FONT);
                setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
            } else {
                setFont(ODD_ROW_FONT);
            }
            return this;
        }
    }
}
