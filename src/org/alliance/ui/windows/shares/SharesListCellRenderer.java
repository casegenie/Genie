package org.alliance.ui.windows.shares;

import org.alliance.ui.themes.util.SubstanceThemeHelper;
import org.alliance.ui.themes.AllianceListCellRenderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JList;

/**
 *
 * @author Bastvera
 */
public class SharesListCellRenderer extends AllianceListCellRenderer {

    private static Font EVEN_ROW_FONT;
    private static Font ODD_ROW_FONT;
    private static String GROUP_TEXT;

    public SharesListCellRenderer(String groupText) {
        super(SubstanceThemeHelper.isSubstanceInUse());
        GROUP_TEXT = "     --> " + groupText + " ";
        ODD_ROW_FONT = new Font(renderer.getFont().getFontName(), Font.BOLD, renderer.getFont().getSize());
        EVEN_ROW_FONT = new Font(renderer.getFont().getFontName(), renderer.getFont().getStyle(), renderer.getFont().getSize() - 2);
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        if (index % 2 == 1) {
            value = GROUP_TEXT + value;
        }

        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (index % 2 == 1) {
            renderer.setFont(EVEN_ROW_FONT);
            renderer.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
        } else {
            renderer.setFont(ODD_ROW_FONT);
        }
        return renderer;
    }
}
