package org.alliance.ui.dialogs;

import com.stendahls.XUI.XUIDialog;
import org.alliance.ui.UISubsystem;
import org.alliance.ui.themes.util.SubstanceThemeHelper;

import java.awt.event.ActionEvent;
import javax.swing.JFrame;
import javax.swing.JTextField;

/**
 *
 * @author Bastvera
 */
public class AddGroupDialog extends XUIDialog {

    private String groupname;

    public AddGroupDialog(UISubsystem ui, JFrame f) throws Exception {
        super(ui.getRl(), ui.getRl().getResourceStream("xui/groupdialog.xui.xml"), ui.getMainWindow(), true);
        SubstanceThemeHelper.setButtonsToGeneralArea(xui.getXUIComponents());
        ((JTextField) xui.getComponent("customtext")).selectAll();
        ((JTextField) xui.getComponent("customtext")).requestFocus();
        display();
        this.requestFocus();
    }

    public String getGroupname() {
        return groupname;
    }

    public void EVENT_confirm(ActionEvent e) {
        groupname = ((JTextField) xui.getComponent("customtext")).getText();
        dispose();
    }
}
