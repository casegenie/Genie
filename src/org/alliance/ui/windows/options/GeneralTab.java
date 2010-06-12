package org.alliance.ui.windows.options;

import com.stendahls.XUI.XUI;
import com.stendahls.XUI.XUIDialog;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.util.Locale;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import org.alliance.core.LanguageResource;
import org.alliance.ui.UISubsystem;
import org.alliance.ui.themes.util.SubstanceThemeHelper;

import javax.swing.JPanel;

/**
 *
 * @author Bastvera
 */
public class GeneralTab extends XUIDialog implements TabHelper {

    private JPanel tab;
    private UISubsystem ui;
    private final static String FONT_SIZES[] = {"9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "20",
        "22", "24", "26", "28", "30", "32", "34", "36", "38", "40", "44", "48", "56", "64", "72"};
    private final static String[] OPTIONS = {
        "my.nickname", "internal.guiskin", "internal.language",
        "internal.enablesupportfornonenglishcharacters", "internal.showpublicchatmessagesintray",
        "internal.showprivatechatmessagesintray", "internal.showsystemmessagesintray",
        "internal.globalfont", "internal.chatfont", "internal.globalsize", "internal.chatsize"};

    public GeneralTab(String loading) {
        tab = new JPanel();
        tab.add(new JLabel(loading));
        tab.setName(LanguageResource.getLocalizedString(getClass(), "title"));
        tab.setToolTipText(LanguageResource.getLocalizedString(getClass(), "tooltip"));
    }

    public GeneralTab(final UISubsystem ui) throws Exception {
        init(ui.getRl(), ui.getRl().getResourceStream("xui/optionstabs/generaltab.xui.xml"));
        this.ui = ui;

        LanguageResource.translateXUIElements(getClass(), xui.getXUIComponents());
        SubstanceThemeHelper.setButtonsToGeneralArea(xui.getXUIComponents());
        tab = (JPanel) xui.getComponent("generaltab");
        tab.setName(LanguageResource.getLocalizedString(getClass(), "title"));
        tab.setToolTipText(LanguageResource.getLocalizedString(getClass(), "tooltip"));

        fillLanguage();
        fillFonts();
    }

    private void fillLanguage() {
        JComboBox language = (JComboBox) xui.getComponent("internal.language");
        File languageDir = new File(LanguageResource.LANGUAGE_PATH);
        for (String filename : languageDir.list()) {
            if (filename.startsWith("alliance_")) {
                String id = filename.substring(filename.indexOf("_") + 1, filename.lastIndexOf("."));
                Locale l = new Locale(id);
                if (!l.getDisplayLanguage().equalsIgnoreCase(id)) {
                    language.addItem(l.getDisplayLanguage() + " - " + id);
                    if (id.equals("en")) {
                        language.setSelectedItem(l.getDisplayLanguage() + " - " + id);
                    }
                }
            }
        }
    }

    private void fillFonts() {
        JComboBox globalsize = (JComboBox) xui.getComponent("internal.globalsize");
        JComboBox chatsize = (JComboBox) xui.getComponent("internal.chatsize");
        JComboBox globalfont = (JComboBox) xui.getComponent("internal.globalfont");
        JComboBox chatfont = (JComboBox) xui.getComponent("internal.chatfont");
        for (int i = 0; i < FONT_SIZES.length; i++) {
            globalsize.addItem(FONT_SIZES[i]);
            chatsize.addItem(FONT_SIZES[i]);
        }
        GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Font[] fonts = e.getAllFonts();
        for (Font font : fonts) {
            globalfont.addItem(font.getFontName());
            chatfont.addItem(font.getFontName());
        }
    }

    @Override
    public JPanel getTab() {
        return tab;
    }

    @Override
    public String[] getOptions() {
        return OPTIONS;
    }

    @Override
    public XUI getXUI() {
        return super.getXUI();
    }

    @Override
    public boolean isAllowedEmpty(String option) {
        return false;
    }

    @Override
    public String getOverridedSettingValue(String option, String value) {
        if (option.equals("internal.globalfont") || option.equals("internal.chatfont")) {
            if (value.isEmpty()) {
                return tab.getFont().getFontName();
            }
        }
        if (option.equals("internal.globalsize") || option.equals("internal.chatsize")) {
            int i = 0;
            while (i < FONT_SIZES.length) {
                if (FONT_SIZES[i].equals(value)) {
                    return String.valueOf(i);
                }
                i++;
            }
        }
        return value;
    }

    @Override
    public Object getOverridedComponentValue(String option, Object value) {
        if (value == null || value.toString().isEmpty()) {
            if (!isAllowedEmpty(option)) {
                return null;
            }
        }
        if (option.equals("internal.guiskin") || option.equals("internal.language")
                || option.equals("internal.globalfont") || option.equals("internal.chatfont")) {
            return (((JComboBox) xui.getXUIComponent(option).getComponent()).getSelectedItem());
        }
        if (option.equals("internal.globalsize") || option.equals("internal.chatsize")) {
            return FONT_SIZES[Integer.parseInt(value.toString())];
        }
        if (option.equals("my.nickname")) {
            String nickname = value.toString();
            nickname = nickname.replace("<", "").replace(">", "");
            if (nickname.trim().isEmpty()) {
                return null;
            }
            ui.getCore().getFriendManager().getMe().setNickname(nickname);
            if (ui.getNodeTreeModel(false) != null) {
                ui.getNodeTreeModel(false).signalNodeChanged(
                        ui.getCore().getFriendManager().getMe());
            }
        }
        return value;
    }

    @Override
    public void postOperation() {
    }
}
