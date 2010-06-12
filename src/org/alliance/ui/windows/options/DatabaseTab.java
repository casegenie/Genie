package org.alliance.ui.windows.options;

import com.stendahls.XUI.XUI;
import com.stendahls.XUI.XUIDialog;
import javax.swing.JLabel;
import org.alliance.core.LanguageResource;
import org.alliance.ui.UISubsystem;
import org.alliance.ui.themes.util.SubstanceThemeHelper;

import javax.swing.JPanel;

/**
 *
 * @author Bastvera
 */
public class DatabaseTab extends XUIDialog implements TabHelper {

    private JPanel tab;
    private UISubsystem ui;
    private final static String[] OPTIONS = new String[]{"internal.hashspeedinmbpersecond",
        "internal.politehashingintervalingigabytes", "internal.politehashingwaittimeinminutes",
        "internal.rescansharewhenalliancestarts"};

    public DatabaseTab(String loading) {
        tab = new JPanel();
        tab.add(new JLabel(loading));
        tab.setName(LanguageResource.getLocalizedString(getClass(), "title"));
        tab.setToolTipText(LanguageResource.getLocalizedString(getClass(), "tooltip"));
    }

    public DatabaseTab(final UISubsystem ui) throws Exception {
        init(ui.getRl(), ui.getRl().getResourceStream("xui/optionstabs/databasetab.xui.xml"));
        this.ui = ui;

        LanguageResource.translateXUIElements(getClass(), xui.getXUIComponents());
        SubstanceThemeHelper.setButtonsToGeneralArea(xui.getXUIComponents());
        tab = (JPanel) xui.getComponent("databasetab");
        tab.setName(LanguageResource.getLocalizedString(getClass(), "title"));
        tab.setToolTipText(LanguageResource.getLocalizedString(getClass(), "tooltip"));
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
        return value;
    }

    @Override
    public Object getOverridedComponentValue(String option, Object value) {
        if (value == null || value.toString().isEmpty()) {
            if (!isAllowedEmpty(option)) {
                return null;
            }
        }
        return value;
    }

    @Override
    public void postOperation() {
    }
}
