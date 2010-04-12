package org.alliance.ui.windows;

import com.stendahls.XUI.XUIDialog;
import com.stendahls.nif.util.EnumerationIteratorConverter;
import com.stendahls.ui.JHtmlLabel;
import static org.alliance.core.CoreSubsystem.KB;
import org.alliance.core.node.Friend;
import org.alliance.core.settings.My;
import org.alliance.core.settings.Routerule;
import org.alliance.core.settings.SettingClass;
import org.alliance.core.settings.Settings;
import org.alliance.ui.UISubsystem;
import org.alliance.ui.dialogs.OptionDialog;
import org.alliance.ui.themes.util.SubstanceThemeHelper;

import java.io.File;
import java.util.HashMap;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileFilter;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA. User: maciek Date: 2006-mar-20 Time: 22:33:46 To
 * change this template use File | Settings | File Templates.
 */
public class OptionsWindow extends XUIDialog {

    private final static String[] OPTIONS = new String[]{
        "internal.uploadthrottle", "internal.hashspeedinmbpersecond",
        "internal.politehashingwaittimeinminutes",
        "internal.politehashingintervalingigabytes", "my.nickname",
        "server.port", "internal.downloadfolder",
        "internal.alwaysallowfriendstoconnect",
        "internal.alwaysallowfriendsoffriendstoconnecttome",
        "internal.invitationmayonlybeusedonce", "internal.encryption",
        "internal.showpublicchatmessagesintray",
        "internal.showprivatechatmessagesintray",
        "internal.showsystemmessagesintray",
        "internal.rescansharewhenalliancestarts",
        "internal.enablesupportfornonenglishcharacters",
        "internal.alwaysautomaticallyconnecttoallfriendsoffriend",
        "server.lansupport", "internal.automaticallydenyallinvitations",
        "internal.enableiprules", "server.dnsname",
        "internal.disablenewuserpopup",
        "internal.alwaysallowfriendsoftrustedfriendstoconnecttome",
        "internal.alwaysdenyuntrustedinvitations", "internal.rdnsname",
        "internal.pmsound", "internal.downloadsound", "internal.publicsound",
        "internal.guiskin"};
    private UISubsystem ui;
    private HashMap<String, JComponent> components = new HashMap<String, JComponent>();
    private JList ruleList;
    private DefaultListModel ruleListModel;
    private JTextField nickname, downloadFolder;
    private boolean openedWithUndefiniedNickname;
    private int uploadThrottleBefore;

    public OptionsWindow(final UISubsystem ui) throws Exception {
        super(ui.getMainWindow());
        this.ui = ui;

        init(ui.getRl(), ui.getRl().getResourceStream("xui/optionswindow.xui.xml"));
        SubstanceThemeHelper.setButtonsToGeneralArea(xui.getXUIComponents());

        xui.getComponent("server.port").setEnabled(false);

        nickname = (JTextField) xui.getComponent("my.nickname");
        downloadFolder = (JTextField) xui.getComponent("internal.downloadfolder");
     
        ruleList = (JList) xui.getComponent("ruleList");           
        ruleListModel = new DefaultListModel();
      
        for (Routerule rule : ui.getCore().getSettings().getRulelist()) {
            ruleListModel.addElement(rule);
        }
        ruleList.setModel(ruleListModel);

        openedWithUndefiniedNickname = ui.getCore().getSettings().getMy().getNickname().equals(My.UNDEFINED_NICKNAME);

        if (ui.getCore().getUpnpManager().isPortForwardSuccedeed()) {
            ((JHtmlLabel) xui.getComponent("portforward")).setText("Port successfully forwarded in your router using UPnP.");
        } else {
            ((JHtmlLabel) xui.getComponent("portforward")).addHyperlinkListener(new HyperlinkListener() {

                @Override
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        ui.openURL("http://www.portforward.com");
                    }
                }
            });
        }
      
        uploadThrottleBefore = ui.getCore().getSettings().getInternal().getUploadthrottle();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        pack();

        // we set this AFTER we pack the frame - so that the frame isnt made
        // wider because of a long download folder path
        for (String k : OPTIONS) {
            JComponent c = (JComponent) xui.getComponent(k);
            if (c != null) {
                components.put(k, c);
                setComponentValue(c, getSettingValue(k));
            }
        }

        checkCheckBoxStatus();
        configureCheckBoxListeners();

        Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(ss.width / 2 - getWidth() / 2, ss.height / 2 - getHeight() / 2);
        setVisible(true);
    }

    private String getSettingValue(String k) throws Exception {
        String clazz = k.substring(0, k.indexOf('.'));
        k = k.substring(k.indexOf('.') + 1);
        SettingClass setting = getSettingClass(clazz);
        return String.valueOf(setting.getValue(k));
    }

    private SettingClass getSettingClass(String clazz) throws Exception {
        if (clazz.equals("internal")) {
            return ui.getCore().getSettings().getInternal();
        } else if (clazz.equals("my")) {
            return ui.getCore().getSettings().getMy();
        } else if (clazz.equals("server")) {
            return ui.getCore().getSettings().getServer();
        } else {
            throw new Exception("Could not find class type: " + clazz);
        }
    }

    private void setComponentValue(JComponent c, String settingValue) {
        if (c instanceof JTextField) {
            JTextField tf = (JTextField) c;
            tf.setText(settingValue);
        } else if (c instanceof JCheckBox) {
            JCheckBox b = (JCheckBox) c;
            if ("0".equals(settingValue) || "no".equalsIgnoreCase(settingValue) || "false".equalsIgnoreCase(settingValue)) {
                b.setSelected(false);
            } else {
                b.setSelected(true);
            }

        } else if (c instanceof JComboBox) {
            JComboBox b = (JComboBox) c;
            try {
                b.setSelectedIndex(Integer.parseInt(settingValue));
            } catch (NumberFormatException e) {
                b.setSelectedItem(settingValue);
            }
        }
    }

    public void EVENT_apply(ActionEvent a) throws Exception {
        apply();
    }

    private boolean allowEmptyFields(String k) {

        if (k.equalsIgnoreCase("server.dnsname") || k.equalsIgnoreCase("internal.pmsound") || k.equalsIgnoreCase("internal.downloadsound") || k.equalsIgnoreCase("internal.publicsound")) {
            return true;
        } else {
            return false;
        }
    }

    private boolean apply() throws Exception {
        if (!nicknameIsOk()) {
            return false;
        }

        // update primitive values
        for (String k : OPTIONS) {
            JComponent c = (JComponent) xui.getComponent(k);
            if (getComponentValue(c).toString().length() == 0 && !allowEmptyFields(k)) {
                OptionDialog.showErrorDialog(this, "One or more required fields is empty (field id: " + k + ").");
                return false;
            }
            setSettingValue(k, getComponentValue(c));
        }

        Settings settings = ui.getCore().getSettings();
       
        ui.getCore().getFriendManager().getMe().setNickname(nickname.getText());
        if (ui.getNodeTreeModel(false) != null) {
            ui.getNodeTreeModel(false).signalNodeChanged(
                    ui.getCore().getFriendManager().getMe());
        }
        // update rulelist
        settings.getRulelist().clear();
        for (Routerule rule : EnumerationIteratorConverter.iterable(
                ruleListModel.elements(), Routerule.class)) {
            settings.getRulelist().add(rule);
        }

        ui.getCore().getNetworkManager().getUploadThrottle().setRate(settings.getInternal().getUploadthrottle() * KB);
        if (uploadThrottleBefore != settings.getInternal().getUploadthrottle()) {
            ui.getCore().getNetworkManager().getBandwidthOut().resetHighestCPS();
        }

        ui.getCore().saveSettings();
        return true;
    }
     
    private boolean nicknameIsOk() {
        if (nickname.getText().equals(My.UNDEFINED_NICKNAME)) {
            OptionDialog.showErrorDialog(ui.getMainWindow(),
                    "You must enter a nickname before continuing.");
            return false;
        }
        if (nickname.getText().indexOf('<') != -1 || nickname.getText().indexOf('>') != -1) {
            OptionDialog.showErrorDialog(ui.getMainWindow(),
                    "Your nickname may not contain &lt; or &gt;.");
            return false;
        }
        return true;
    }

    public void EVENT_cancel(ActionEvent a) throws Exception {
        if (!nicknameIsOk()) {
            return;
        }
        dispose();
    }

    public void EVENT_ok(ActionEvent a) throws Exception {
        if (apply()) {
            dispose();
            if (openedWithUndefiniedNickname) {
                OptionDialog.showInformationDialog(ui.getMainWindow(),
                        "It is time to connect to other users![p]Press OK to continue.[p]");
                ui.getMainWindow().openWizard();
            }
        }
    }

    private Object getComponentValue(JComponent c) {
        if (c instanceof JTextField) {
            return ((JTextField) c).getText();
        }
        if (c instanceof JCheckBox) {
            return ((JCheckBox) c).isSelected() ? 1 : 0;
        }
        if (c instanceof JComboBox) {
            if (components.get("internal.guiskin").equals(c)) {
                return ((JComboBox) c).getSelectedItem();
            } else {
                return "" + ((JComboBox) c).getSelectedIndex();
            }
        }
        return null;
    }

    private void setSettingValue(String k, Object val) throws Exception {
        String clazz = k.substring(0, k.indexOf('.'));
        k = k.substring(k.indexOf('.') + 1);
        SettingClass setting = getSettingClass(clazz);
        setting.setValue(k, val);
    }   

    public void EVENT_addrule(ActionEvent e) throws Exception {
        AddRuleWindow window = new AddRuleWindow(ui);
        if (window.getHuman() == null) {
            //This is here to take care of the case of a user adding a rule, then hitting cancel
            return;
        }
        ruleListModel.add(ruleListModel.size(), new Routerule(window.getHuman()));
        ruleList.revalidate();
        ruleList.setSelectedIndex(ruleListModel.size() - 1);
    }

    public void EVENT_editrule(ActionEvent e) throws Exception {
        if (ruleList.getSelectedIndex() != -1) {
            Routerule temp = (Routerule) ruleListModel.get(ruleList.getSelectedIndex());
            int ruleIndex = ruleList.getSelectedIndex();
            AddRuleWindow window = new AddRuleWindow(ui, ruleIndex, temp.getHumanreadable());
            if (window.getHuman() != null) {//Kratos
                ruleListModel.remove(ruleList.getSelectedIndex());
                ruleListModel.add(ruleIndex, new Routerule(window.getHuman()));
                ruleList.revalidate();
                ruleList.setSelectedIndex(ruleIndex);
            }
        }
    }

    public void EVENT_moveruleup(ActionEvent e) {
        if (ruleList.getSelectedIndex() != -1) {
            int ruleIndex = ruleList.getSelectedIndex();
            if (ruleIndex <= 0 || ruleIndex > ruleListModel.size()) {
                return;
            }
            Object temp = ruleListModel.get(ruleIndex);
            ruleListModel.remove(ruleList.getSelectedIndex());
            ruleListModel.add(ruleIndex - 1, temp);
            ruleList.revalidate();
            ruleList.setSelectedIndex(ruleIndex - 1);
        }
    }

    public void EVENT_moveruledown(ActionEvent e) {
        if (ruleList.getSelectedIndex() != -1) {
            int ruleIndex = ruleList.getSelectedIndex();
            if (ruleIndex < 0 || ruleIndex >= ruleListModel.size() - 1) {
                return;
            }
            Object temp = ruleListModel.get(ruleIndex);
            ruleListModel.remove(ruleList.getSelectedIndex());
            ruleListModel.add(ruleIndex + 1, temp);
            ruleList.revalidate();
            ruleList.setSelectedIndex(ruleIndex + 1);
        }
    }

    public void EVENT_removerule(ActionEvent e) {
        if (ruleList.getSelectedIndex() != -1) {
            ruleListModel.remove(ruleList.getSelectedIndex());
            ruleList.revalidate();
        }
    }

    public void EVENT_browse(ActionEvent e) {
        JFileChooser fc = new JFileChooser(downloadFolder.getText());
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String path = fc.getSelectedFile().getPath();
            downloadFolder.setText(path);
        }
    }
   
    private void browseSound(String s) {
        JFileChooser fc = new JFileChooser("");
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setAcceptAllFileFilterUsed(false);

        fc.addChoosableFileFilter(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                if (pathname.toString().endsWith("wav") || pathname.isDirectory()) {
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public String getDescription() {
                return ("Wave files");
            }
        });

        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String path = fc.getSelectedFile().getPath();
            ((JTextField) xui.getComponent(s)).setText(path);
        }
    }

    public void EVENT_browsesoundpm(ActionEvent e) {
        browseSound("internal.pmsound");
    }

    public void EVENT_browsesounddownload(ActionEvent e) {
        browseSound("internal.downloadsound");
    }

    public void EVENT_browsesoundpublic(ActionEvent e) {
        browseSound("internal.publicsound");
    }

    public void EVENT_sounddefault(ActionEvent e) {
        ((JTextField) xui.getComponent("internal.pmsound")).setText("sounds/chatpm.wav");
        ((JTextField) xui.getComponent("internal.downloadsound")).setText("sounds/download.wav");
        ((JTextField) xui.getComponent("internal.publicsound")).setText("sounds/chatpublic.wav");
    }

    public void EVENT_soundmute(ActionEvent e) {
        ((JTextField) xui.getComponent("internal.pmsound")).setText("");
        ((JTextField) xui.getComponent("internal.downloadsound")).setText("");
        ((JTextField) xui.getComponent("internal.publicsound")).setText("");
    }
       
    private void checkCheckBoxStatus() {
        if (getComponentValue(components.get("internal.alwaysallowfriendsoffriendstoconnecttome")).toString().equalsIgnoreCase("1")) {
            setComponentValue(components.get("internal.alwaysallowfriendsoftrustedfriendstoconnecttome"), "false");
            components.get("internal.alwaysallowfriendsoftrustedfriendstoconnecttome").setEnabled(false);
            setComponentValue(components.get("internal.automaticallydenyallinvitations"), "false");
            components.get("internal.automaticallydenyallinvitations").setEnabled(false);
            setComponentValue(components.get("internal.alwaysdenyuntrustedinvitations"), "false");
            components.get("internal.alwaysdenyuntrustedinvitations").setEnabled(false);
        } else if (getComponentValue(components.get("internal.automaticallydenyallinvitations")).toString().equalsIgnoreCase("1")) {
            setComponentValue(components.get("internal.alwaysallowfriendsoftrustedfriendstoconnecttome"), "false");
            components.get("internal.alwaysallowfriendsoftrustedfriendstoconnecttome").setEnabled(false);
            setComponentValue(components.get("internal.alwaysdenyuntrustedinvitations"), "false");
            components.get("internal.alwaysdenyuntrustedinvitations").setEnabled(false);
        } else if (getComponentValue(components.get("internal.alwaysallowfriendsoftrustedfriendstoconnecttome")).toString().equalsIgnoreCase("0")) {
            setComponentValue(components.get("internal.alwaysdenyuntrustedinvitations"), "false");
            components.get("internal.alwaysdenyuntrustedinvitations").setEnabled(false);
        }
    }

    private void configureCheckBoxListeners() {
        ((JCheckBox) components.get("internal.alwaysallowfriendsoffriendstoconnecttome")).addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (getComponentValue(components.get("internal.alwaysallowfriendsoffriendstoconnecttome")).toString().equalsIgnoreCase("1")) {
                    setComponentValue(components.get("internal.alwaysallowfriendsoftrustedfriendstoconnecttome"), "false");
                    components.get("internal.alwaysallowfriendsoftrustedfriendstoconnecttome").setEnabled(false);
                    setComponentValue(components.get("internal.automaticallydenyallinvitations"), "false");
                    components.get("internal.automaticallydenyallinvitations").setEnabled(false);
                    setComponentValue(components.get("internal.alwaysdenyuntrustedinvitations"), "false");
                    components.get("internal.alwaysdenyuntrustedinvitations").setEnabled(false);
                } else {
                    components.get("internal.alwaysallowfriendsoftrustedfriendstoconnecttome").setEnabled(true);
                    components.get("internal.automaticallydenyallinvitations").setEnabled(true);
                }
            }
        });

        ((JCheckBox) components.get("internal.automaticallydenyallinvitations")).addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (getComponentValue(components.get("internal.automaticallydenyallinvitations")).toString().equalsIgnoreCase("1")) {
                    setComponentValue(components.get("internal.alwaysallowfriendsoftrustedfriendstoconnecttome"), "false");
                    components.get("internal.alwaysallowfriendsoftrustedfriendstoconnecttome").setEnabled(false);
                    setComponentValue(components.get("internal.alwaysdenyuntrustedinvitations"), "false");
                    components.get("internal.alwaysdenyuntrustedinvitations").setEnabled(false);
                } else {
                    components.get("internal.alwaysallowfriendsoftrustedfriendstoconnecttome").setEnabled(true);
                }
            }
        });

        ((JCheckBox) components.get("internal.alwaysallowfriendsoftrustedfriendstoconnecttome")).addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (getComponentValue(components.get("internal.alwaysallowfriendsoftrustedfriendstoconnecttome")).toString().equalsIgnoreCase("0")) {
                    components.get("internal.alwaysdenyuntrustedinvitations").setEnabled(false);
                    setComponentValue(components.get("internal.alwaysdenyuntrustedinvitations"), "false");
                } else {
                    components.get("internal.alwaysdenyuntrustedinvitations").setEnabled(true);
                }
            }
        });

        ((JCheckBox) components.get("internal.rdnsname")).addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (getComponentValue(components.get("internal.rdnsname")).toString().equalsIgnoreCase("1")) {
                    Boolean confirm = OptionDialog.showQuestionDialog(ui.getMainWindow(), "This option will try to convert all friend's IPs to DNS. Conversion will hang alliance for couple of seconds so use it only when you don't upload/download anything.\nIf DNS names can't be obtained alliance will use IP and try to convert later.");
                    if (confirm) {
                        try {
                            setComponentValue(components.get("server.dnsname"), InetAddress.getByName(ui.getCore().getFriendManager().getMe().getExternalIp(ui.getCore())).getHostName());
                        } catch (UnknownHostException ex) {
                            setComponentValue(components.get("server.dnsname"), "");
                        } catch (IOException ex) {
                            setComponentValue(components.get("server.dnsname"), "");
                        }
                        ui.getCore().getSettings().getInternal().setRdnsname(1);
                        Collection<Friend> friends = ui.getCore().getFriendManager().friends();
                        for (Friend friend : friends.toArray(new Friend[friends.size()])) {
                            friend.setLastKnownHost(friend.rDNSConvert(friend.getLastKnownHost(), ui.getCore().getSettings().getFriend(friend.getGuid())));
                        }
                    } else {
                        setComponentValue(components.get("internal.rdnsname"), "0");
                    }
                } else {
                    Boolean confirm = OptionDialog.showQuestionDialog(ui.getMainWindow(), "Unconverting changes ALL DNS names to IP. Conversion will hang alliance for couple of seconds so use it only when you don't upload/download anything.");
                    if (confirm) {
                        setComponentValue(components.get("server.dnsname"), "");
                        ui.getCore().getSettings().getInternal().setRdnsname(0);
                        Collection<Friend> friends = ui.getCore().getFriendManager().friends();
                        for (Friend friend : friends.toArray(new Friend[friends.size()])) {
                            friend.setLastKnownHost(friend.rDNSConvert(friend.getLastKnownHost(), ui.getCore().getSettings().getFriend(friend.getGuid())));
                        }
                    } else {
                        setComponentValue(components.get("internal.rdnsname"), "1");
                    }
                }
            }
        });
    }
}
