package org.alliance.ui.windows.options;

import com.stendahls.XUI.XUI;
import com.stendahls.XUI.XUIDialog;
import com.stendahls.ui.JHtmlLabel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import org.alliance.core.LanguageResource;
import org.alliance.ui.UISubsystem;
import org.alliance.ui.themes.util.SubstanceThemeHelper;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

/**
 *
 * @author Bastvera
 */
public class NetworkTab extends XUIDialog implements TabHelper {

    private JPanel tab;
    private JComboBox nicBox;
    private JCheckBox showAll;
    private JCheckBox ipv6;
    private JCheckBox lanMode;
    private JCheckBox dnsMode;
    private JCheckBox rdnsMode;
    private JLabel externalText;
    private JLabel dnsText;
    private JTextField dnsField;
    private JTextField localField;
    private JTextField externalField;
    private UISubsystem ui;
    private final static String[] OPTIONS = new String[]{
        "server.port", "internal.allnic", "server.ipv6", "server.lanmode", "server.bindnic",
        "server.dnsmode", "server.dnsname", "server.staticip"};

    public NetworkTab(String loading) {
        tab = new JPanel();
        tab.add(new JLabel(loading));
        tab.setName(LanguageResource.getLocalizedString(getClass(), "title"));
        tab.setToolTipText(LanguageResource.getLocalizedString(getClass(), "tooltip"));
    }

    public NetworkTab(final UISubsystem ui) throws Exception {
        init(ui.getRl(), ui.getRl().getResourceStream("xui/optionstabs/networktab.xui.xml"));
        this.ui = ui;

        LanguageResource.translateXUIElements(getClass(), xui.getXUIComponents());
        SubstanceThemeHelper.setButtonsToGeneralArea(xui.getXUIComponents());
        tab = (JPanel) xui.getComponent("networktab");
        tab.setName(LanguageResource.getLocalizedString(getClass(), "title"));
        tab.setToolTipText(LanguageResource.getLocalizedString(getClass(), "tooltip"));

        if (ui.getCore().getUpnpManager().isPortForwardSuccedeed()) {
            ((JHtmlLabel) xui.getComponent("portforward")).setText(LanguageResource.getLocalizedString(getClass(), "portupnp"));
        } else {
            ((JHtmlLabel) xui.getComponent("portforward")).setText(LanguageResource.getLocalizedString(getClass(), "xui.portforward",
                    "[a href='.']http://www.portforward.com/[/a]"));
            ((JHtmlLabel) xui.getComponent("portforward")).addHyperlinkListener(new HyperlinkListener() {

                @Override
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        ui.openURL("http://www.portforward.com");
                    }
                }
            });
        }

        showAll = (JCheckBox) xui.getComponent("internal.allnic");
        ipv6 = (JCheckBox) xui.getComponent("server.ipv6");
        lanMode = (JCheckBox) xui.getComponent("server.lanmode");
        dnsMode = (JCheckBox) xui.getComponent("server.dnsmode");
        rdnsMode = (JCheckBox) xui.getComponent("internal.rdnsname");
        nicBox = (JComboBox) xui.getComponent("server.bindnic");

        ActionListener al = new ActionListener() {

            private boolean actionInProgress = false;

            @Override
            public void actionPerformed(final ActionEvent e) {
                if (actionInProgress) {
                    return;
                }
                actionInProgress = true;
                try {
                    if (e != null && e.getSource().equals(showAll)) {
                        fillInterfaces();
                    }
                    fillIp(lanMode.isSelected());
                } catch (Exception ex) {
                }
                actionInProgress = false;
            }
        };

        showAll.addActionListener(al);
        ipv6.addActionListener(al);
        lanMode.addActionListener(al);
        nicBox.addActionListener(al);

        dnsMode.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (dnsMode.isSelected()) {
                    dnsField.setEnabled(true);
                    dnsText.setEnabled(true);
                } else {
                    dnsField.setEnabled(false);
                    dnsText.setEnabled(false);
                }
            }
        });

        localField = (JTextField) xui.getComponent("localfield");
        localField.setEditable(false);
        externalField = (JTextField) xui.getComponent("externalfield");
        externalField.setEditable(false);
        externalText = (JLabel) xui.getComponent("externalip");
        dnsField = (JTextField) xui.getComponent("server.dnsname");
        dnsText = (JLabel) xui.getComponent("dnsname");
        ((JTextField) xui.getComponent("server.port")).setEditable(false);
        if (ui.getCore().getSettings().getInternal().getAllnic() == 1) {
            showAll.setSelected(true);
        }
        fillInterfaces();
    }

    private void fillInterfaces() throws Exception {
        nicBox.removeAllItems();
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface netIf : Collections.list(nets)) {
            if (showAll.isSelected()) {
                nicBox.addItem("(" + netIf.getName() + ") " + netIf.getDisplayName());
            } else if (!netIf.isVirtual() && !netIf.isLoopback() && !netIf.isPointToPoint() && netIf.isUp()) {
                Enumeration<InetAddress> inetAddresses = netIf.getInetAddresses();
                if (inetAddresses.hasMoreElements()) {
                    nicBox.addItem("(" + netIf.getName() + ") " + netIf.getDisplayName());
                }
            }
        }
    }

    private void fillIp(final boolean isLanMode) {
        localField.setText(null);
        externalField.setText(null);
        externalField.setEnabled(!isLanMode);
        externalText.setEnabled(!isLanMode);
        //ipv6.setEnabled(!isLanMode);
        String nicName = nicBox.getSelectedItem().toString();
        nicName = nicName.substring(1, nicName.indexOf(")"));
        try {
            if (ui.getCore().getNetworkManager().getIpDetection().updateLocalIp(nicName, ipv6.isSelected() ? 1 : 0)) {
                localField.setText(ui.getCore().getNetworkManager().getIpDetection().getLastLocalIp());
            }
        } catch (Exception ex) {
            //Skip
        }
        if (localField.getText().isEmpty()) {
            localField.setText(LanguageResource.getLocalizedString(getClass(), "noobtain"));
            externalField.setText(LanguageResource.getLocalizedString(getClass(), "noobtain"));
            return;
        }
        if (isLanMode) {
            //ipv6.setSelected(!isLanMode);
            externalField.setText(LanguageResource.getLocalizedString(getClass(), "unused"));
            return;
        } else {
            try {
                if (!nicName.equals(ui.getCore().getSettings().getServer().getBindnic())) {
                    if (ui.getCore().getNetworkManager().getIpDetection().updateExternalIp(0)) {
                        externalField.setText(ui.getCore().getNetworkManager().getIpDetection().getLastExternalIp());
                    }
                } else {
                    externalField.setText(ui.getCore().getNetworkManager().getIpDetection().getLastExternalIp());
                }
            } catch (Exception ex) {
                //Skip
            }
        }
        if (externalField.getText().isEmpty()) {
            externalField.setText(LanguageResource.getLocalizedString(getClass(), "noobtain"));
        }
    }

    /*    ((JCheckBox) components.get("internal.rdnsname")).addActionListener(new ActionListener() {

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
     */
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
        if (option.equals("server.dnsname")) {
            return true;
        }
        return false;
    }

    @Override
    public String getOverridedSettingValue(String option, String value) {
        if (option.equals("server.bindnic")) {
            for (int i = 0; i < nicBox.getItemCount(); i++) {
                if (nicBox.getItemAt(i).toString().startsWith("(" + value + ")")) {
                    return nicBox.getItemAt(i).toString();
                }
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
        if (option.equals("server.bindnic")) {
            String nic = nicBox.getSelectedItem().toString();
            return nic.substring(1, nic.indexOf(")"));
        }
        return value;
    }

    @Override
    public void postOperation() {
        ipv6.setEnabled(false); // Disabled
        rdnsMode.setEnabled(false); // Disabled
        (showAll.getActionListeners())[0].actionPerformed(null);
        (dnsMode.getActionListeners())[0].actionPerformed(null);
    }
}
