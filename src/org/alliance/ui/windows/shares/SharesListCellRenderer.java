package org.alliance.ui.windows.shares;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import org.alliance.ui.themes.util.SubstanceListCellRendererHelper;

/**
 *
 * @author Bastvera
 */
public class SharesListCellRenderer extends SubstanceListCellRendererHelper {

    private static Font EVEN_ROW_FONT;

    public SharesListCellRenderer() {
        EVEN_ROW_FONT = new Font(getFont().getFontName(), getFont().getStyle(), getFont().getSize() - 2);
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        if (index % 2 == 1) {
            value = new String("     --> Group: " + value);
        }
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (index % 2 == 1) {
            setFont(EVEN_ROW_FONT);
            setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
        }
        return this;
    }
}
