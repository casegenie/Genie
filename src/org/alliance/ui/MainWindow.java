package org.alliance.ui;

import com.stendahls.XUI.MenuItemDescriptionListener;
import com.stendahls.XUI.XUIFrame;
import com.stendahls.nif.ui.OptionDialog;
import com.stendahls.nif.ui.mdi.MDIManager;
import com.stendahls.nif.ui.mdi.MDIManagerEventListener;
import com.stendahls.nif.ui.mdi.MDIWindow;
import com.stendahls.nif.ui.mdi.infonodemdi.InfoNodeMDIManager;
import com.stendahls.nif.ui.toolbaractions.ToolbarActionManager;
import com.stendahls.nif.util.SXML;
import com.stendahls.ui.util.RecursiveBackgroundSetter;
import com.stendahls.util.TextUtils;
import de.javasoft.plaf.synthetica.SyntheticaRootPaneUI;
import de.javasoft.synthetica.addons.SystemMonitor;
import de.javasoft.synthetica.addons.systemmonitor.Collector;
import static org.alliance.core.CoreSubsystem.MB;
import org.alliance.core.NeedsUserInteraction;
import org.alliance.core.PublicChatHistory;
import org.alliance.core.CoreSubsystem;
import org.alliance.core.comm.BandwidthAnalyzer;
import org.alliance.core.interactions.*;
import org.alliance.core.node.Friend;
import org.alliance.core.node.Node;
import org.alliance.launchers.OSInfo;
import org.alliance.launchers.StartupProgressListener;
import org.alliance.ui.addfriendwizard.AddFriendWizard;
import org.alliance.ui.addfriendwizard.ForwardInvitationNodesList;
import org.alliance.ui.windows.*;
import org.alliance.ui.windows.search.SearchMDIWindow;
import org.alliance.ui.windows.viewshare.ViewShareMDIWindow;

import javax.swing.*;
import javax.swing.plaf.metal.MetalButtonUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-30
 * Time: 16:25:12
 */
public class MainWindow extends XUIFrame implements MenuItemDescriptionListener, MDIManagerEventListener, Runnable {
    private UISubsystem ui;
    private JLabel statusMessage, shareMessage;
    private JProgressBar bandwidthIn, bandwidthOut;
    private ToolbarActionManager toolbarActionManager;
    protected MDIManager mdiManager;

    private AddFriendWizard lastAddFriendWizard;

    private int userInteractionsInProgress = 0;
    private PublicChatMessageMDIWindow publicChat;

    public MainWindow() throws Exception {
    }

    public void init(final UISubsystem ui, StartupProgressListener pml) throws Exception {
        this.ui = ui;

        pml.updateProgress("Loading main window");
        init(ui.getRl(), "xui/mainwindow.xui.xml");

        bandwidthIn = (JProgressBar)xui.getComponent("bandwidthin");
        bandwidthOut = (JProgressBar)xui.getComponent("bandwidthout");

        ((JButton)xui.getComponent("rescan")).setUI(new MetalButtonUI());

        xui.setEventHandler(this);
        xui.setMenuItemDescriptionListener(this);
        statusMessage = (JLabel)xui.getComponent("statusbar");
        shareMessage = (JLabel)xui.getComponent("sharing");
//        uploadMessage = (JLabel)xui.getComponent("totalup");
//        downloadMessage = (JLabel)xui.getComponent("totaldown");

        setupToolbar();

        setupWindowEvents();

        setupSpeedDiagram();

        setupMDIManager();

        pml.updateProgress("Loading main window (friend list)");
        mdiManager.addWindow(new FriendListMDIWindow(mdiManager, ui));
        pml.updateProgress("Loading main window (public chat)");
        mdiManager.addWindow(publicChat = new PublicChatMessageMDIWindow(ui));
        pml.updateProgress("Loading main window (search)");
        mdiManager.addWindow(new SearchMDIWindow(ui));
        pml.updateProgress("Loading main window (download)");
        mdiManager.addWindow(new DownloadsMDIWindow(ui));
        pml.updateProgress("Loading main window (uploads)");
        EVENT_uploads(null);

        pml.updateProgress("Loading main window");
        for(PublicChatHistory.Entry e : ui.getCore().getPublicChatHistory().allMessages()) {
            publicChat.addMessage(ui.getCore().getFriendManager().nickname(e.fromGuid), e.message, e.tick, true);
        }

        mdiManager.selectWindow(publicChat);

        RecursiveBackgroundSetter.setBackground(xui.getComponent("bottompanel"), new Color(0xE3E2E6), false);

        setTitle("Alliance");

        getRootPane().setWindowDecorationStyle(JRootPane.NONE);
        getRootPane().setWindowDecorationStyle(JRootPane.FRAME);

        showWindow();

        Thread t = new Thread(this, "Regular Interval UI Update Thread");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();

        if (ui.getCore().getSettings().getMy().hasUndefinedNickname()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    OptionDialog.showInformationDialog(MainWindow.this, "Welcome to Alliance![p]Before starting you need to enter your nickname.[p]The options window will now open.[p]");
                    try {
                        EVENT_options(null);
                    } catch (Exception e) {
                        ui.handleErrorInEventLoop(e);
                    }
                }
            });
        }

        if (T.t) T.info("done");
    }

    private void setupSpeedDiagram() {
        SystemMonitor monitor = new SystemMonitor();
        String id = ""+Math.random();
        monitor.addCollector(id, 3000, 1000, new Collector() {
            public double getValue() {
                return ui.getCore().getNetworkManager().getBandwidthOutHighRefresh().getCPS();
            }

            public double getMaxValue() {
                //double d = Math.max(ui.getCore().getNetworkManager().getBandwidthInHighRefresh().getHighestCPS(), ui.getCore().getNetworkManager().getBandwidthOutHighRefresh().getHighestCPS());
                double d = ui.getCore().getNetworkManager().getBandwidthOutHighRefresh().getHighestCPS();
                return d;
            }
        });
        monitor.setColor(id, new Color(0xf68a08));

        id = ""+Math.random();
        monitor.addCollector(id, 3000, 1000, new Collector() {
            public double getValue() {
                return ui.getCore().getNetworkManager().getBandwidthInHighRefresh().getCPS();
            }

            public double getMaxValue() {
                //return Math.max(ui.getCore().getNetworkManager().getBandwidthInHighRefresh().getHighestCPS(), ui.getCore().getNetworkManager().getBandwidthOutHighRefresh().getHighestCPS());
                return ui.getCore().getNetworkManager().getBandwidthInHighRefresh().getHighestCPS();
            }
        });
        monitor.setColor(id, Color.green);

        monitor.setPopupEnabled(true);
        monitor.setSpotlightEnabled(true);
        ((JPanel)xui.getComponent("diagrampanel")).add(monitor);
    }

    private void setupMDIManager() {
        mdiManager = new InfoNodeMDIManager(ui);
        mdiManager.setEventListener(this);
        ((InfoNodeMDIManager)mdiManager).setMaximumNumberOfWindowsByClass(10);
        ((JPanel)xui.getComponent("applicationArea")).add(mdiManager);
    }

    private void setupToolbar() throws Exception {
        toolbarActionManager = new ToolbarActionManager(SXML.loadXML(xui.getResourceLoader().getResourceStream("toolbaractions.xml")));
        toolbarActionManager.bindToFrame(this);
    }

    public synchronized void setStatusMessage(final String s) {
        setStatusMessage(s, false);
    }

    private Thread messageFadeThread;
    public synchronized void setStatusMessage(final String s, final boolean important) {
        if (statusMessage.getText().trim().equals(s.trim())) return;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (s == null || s.length() == 0)
                    statusMessage.setText(" ");
                else
                    statusMessage.setText(s);
            }
        });

        if (messageFadeThread != null) messageFadeThread.interrupt();
        messageFadeThread = new Thread(new Runnable() {
            public void run() {
                Thread myThread = messageFadeThread;
                try {
                    if (myThread != messageFadeThread) return;
                    changeStatusMessageColor(0);
                    if (myThread != messageFadeThread) return;
                    Thread.sleep(5500);
                    if (myThread != messageFadeThread) return;
                    int c = 0;
                    for (int i = 0; i < 23 && messageFadeThread == myThread; i++) {
                        changeStatusMessageColor(c | c << 8 | c << 16);
                        c += 10;
                        Thread.sleep(80);
                    }
                    if (myThread != messageFadeThread) return;
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            statusMessage.setText(" ");
                        }
                    });
                } catch (InterruptedException e) {
                } catch (Exception e) {
                    if (T.t) T.error("Problem in progressMessage fade loop: " + e);
                }
            }
        });
        messageFadeThread.setName("MessageFadeThread");
        messageFadeThread.start();
    }

    private synchronized void changeStatusMessageColor(final int color) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                statusMessage.setForeground(new Color(color));
                statusMessage.repaint();
            }
        });
    }

    public void EVENT_hide(ActionEvent e) throws Exception {
        if (OSInfo.supportsTrayIcon() || OSInfo.isMac()) {
            setVisible(false);
        } else {
            OptionDialog.showInformationDialog(this, "You cant hide the Alliance window on your system. Simply close the window to shutdown Alliance.");
        }
    }

    private void setupWindowEvents() {
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (OSInfo.isMac()) {
                    if(T.t)T.info("Running on mac - never shutdown - just hide the window - that's how mac applications work");
                    setVisible(false);
                } else if (CoreSubsystem.isRunningAsTestSuite()) {
                    if(T.t)T.info("Running as testsuite - only hide window");
                    setVisible(false);
                } else if (OSInfo.supportsTrayIcon()) {
                    if (OSInfo.isWindows()) {
                        if(T.t)T.info("On windows Alliance can restart so we're triggering a restart here - in order to preserve memory");
                        Thread t = new Thread(new Runnable() {
                            public void run() {
                                try {
                                    Thread.sleep(100);
                                    setVisible(false);
                                    ui.getCore().restartProgram(false);
                                } catch (Exception e1) {
                                    ui.handleErrorInEventLoop(e1);
                                }
                            }
                        });
                        t.start();
                    } else {
                        if(T.t)T.info("We have a tray icon but cannot restart Alliance so we just hide the window here");
                        setVisible(false);
                    }
                } else {
                    if(T.t)T.info("No tray icon and not Mac - just shut down alliance");
                    shutdown();
                }
            }
        });
    }

    public void tryQuit() {
        shutdown();
    }

    public void showMenuItemDescription(String description) {
        setStatusMessage(description);
    }

    boolean shuttingDown=false;
    public boolean shutdown() {
        if (shuttingDown) return true;
        shuttingDown = true;

        saveWindowState();
        setVisible(false);
        dispose();
        ui.shutdown();
        return true;
    }

    public void saveWindowState() {
        try {
            if(T.t) T.info("Serializing window state");
            FileOutputStream out = new FileOutputStream(System.getProperty("user.home")+"/mainwindow.state"+System.getProperty("tracewindow.id"));
            ObjectOutputStream obj = new ObjectOutputStream(out);

            obj.writeObject(getLocation());
            obj.writeObject(getSize());
            obj.writeInt(getExtendedState());

            obj.flush();
            obj.close();
        } catch(Exception e) {
            if(T.t)T.error("Could not save window state "+e);
        }
    }

    public void showWindow() {
        if(T.t) T.info("Deserializing window state");
        try {
            // TODO tracewindow.id is read, but never set? => everytime null?
            // TODO user.home isn't the right place to put this file into, should be appdata/alliance or something similar
            FileInputStream in = new FileInputStream(System.getProperty("user.home")+"/mainwindow.state"+System.getProperty("tracewindow.id"));
            ObjectInputStream obj = new ObjectInputStream(in);

            setLocation((Point)obj.readObject());
            setSize((Dimension)obj.readObject());
            if (getRootPane().getUI() instanceof SyntheticaRootPaneUI)
                ((SyntheticaRootPaneUI)getRootPane().getUI()).setMaximizedBounds(this);
            setExtendedState(obj.readInt());
            obj.close();
            setVisible(true);
        } catch(Exception e) {
            display();
            Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
            if (ss.width <= 1024) setExtendedState(MAXIMIZED_BOTH);
        }
        toFront();
    }

    public void titleChanged(MDIWindow source, String newTitle) {
    }

    public void windowSelected(MDIWindow source) {
    }

    public ToolbarActionManager getToolbarActionManager() {
        return toolbarActionManager;
    }

    public MDIManager getMDIManager() {
        return mdiManager;
    }

    public void chatMessage(int guid, String message, long tick, boolean messageHasBeenQueuedAwayForAWhile) throws Exception {
        PrivateChatMessageMDIWindow w = (PrivateChatMessageMDIWindow)mdiManager.getWindow("msg"+guid);
        if (w == null) {
            w = new PrivateChatMessageMDIWindow(ui, guid);
            mdiManager.addWindow(w);
        }
        if (message != null) {
            w.addMessage(ui.getCore().getFriendManager().nickname(guid), message, tick, messageHasBeenQueuedAwayForAWhile);
        }
    }

    public void publicChatMessage(int guid, String message, long tick, boolean messageHasBeenQueuedAwayForAWhile) throws Exception {
        if (message != null) {
            if(T.t)T.info("Received public chat message: "+message);
            publicChat.addMessage(ui.getCore().getFriendManager().nickname(guid), message, tick, messageHasBeenQueuedAwayForAWhile);
            ui.getCore().getPublicChatHistory().addMessage(tick,  guid, message);
        }
    }

    public void viewShare(Node f) throws Exception {
        ArrayList<MDIWindow> al = new ArrayList<MDIWindow>();
        for(MDIWindow w : mdiManager) al.add(w);
        for(MDIWindow w : al) {
            if (w instanceof ViewShareMDIWindow) mdiManager.removeWindow(w);
        }
        ViewShareMDIWindow w = (ViewShareMDIWindow)mdiManager.getWindow("viewshare" + f.getGuid());
        if (w == null) {
            w = new ViewShareMDIWindow(ui, f);
            mdiManager.addWindow(w);
        }
        mdiManager.selectWindow(w);
    }

    public void EVENT_myshare(ActionEvent e) throws Exception {
        viewShare(ui.getCore().getFriendManager().getMe());
    }

    public SearchMDIWindow getSearchWindow() {
        return (SearchMDIWindow)mdiManager.getWindow("Search");
    }

    public TraceMDIWindow getTraceWindow() {
        if (mdiManager != null)
            return (TraceMDIWindow)mdiManager.getWindow("trace");
        else
            return null;
    }

    public void createTraceWindow() throws Exception {
        TraceMDIWindow w = (TraceMDIWindow)mdiManager.getWindow("trace");
        if (w == null) {
            w = new TraceMDIWindow(ui);
            mdiManager.addWindow(w);
        }
    }

    public ConnectionsMDIWindow getConnectionsWindow() {
        return (ConnectionsMDIWindow)mdiManager.getWindow("connections");
    }

    public DownloadsMDIWindow getDownloadsWindow() {
        return (DownloadsMDIWindow)mdiManager.getWindow("downloads");
    }

    public UploadsMDIWindow getUploadsWindow() {
        return (UploadsMDIWindow)mdiManager.getWindow("uploads");
    }

    public MDIWindow getFriendMDIWindow() {
        return (FriendsTreeMDIWindow)mdiManager.getWindow("friends");
    }

    public FriendListMDIWindow getFriendListMDIWindow() {
        return (FriendListMDIWindow)mdiManager.getWindow("friendlist");
    }

    public ConsoleMDIWindow getConsoleMDIWindow() {
        return (ConsoleMDIWindow)mdiManager.getWindow("console");
    }

    public void run() {
        while(!shuttingDown) {
            if (isVisible()) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (mdiManager != null && getConnectionsWindow() != null) getConnectionsWindow().updateConnectionData();
                        if (mdiManager != null && getDownloadsWindow() != null) getDownloadsWindow().update();
                        if (mdiManager != null && getUploadsWindow() != null) getUploadsWindow().update();
                        if (mdiManager != null && getFriendListMDIWindow() != null) getFriendListMDIWindow().update();

                        shareMessage.setText("Share: "+TextUtils.formatByteSize(ui.getCore().getShareManager().getFileDatabase().getTotalSize())+" in "+ui.getCore().getShareManager().getFileDatabase().getNumberOfFiles()+" files");
//                        uploadMessage.setText("Up: "+TextUtils.formatByteSize(ui.getCore().getNetworkManager().getBandwidthOut().getTotalBytes()));
//                        downloadMessage.setText("Down: "+TextUtils.formatByteSize(ui.getCore().getNetworkManager().getBandwidthIn().getTotalBytes()));

                        updateBandwidth("Downloading", "downloaded", bandwidthIn, ui.getCore().getNetworkManager().getBandwidthIn());
                        updateBandwidth("Uploading", "uploaded", bandwidthOut, ui.getCore().getNetworkManager().getBandwidthOut());

                        displayNagAboutInvitingFriendsIfNeeded();
                    }

                    private void displayNagAboutInvitingFriendsIfNeeded() {
                        try {
                            if (ui.getCore().getFriendManager().getMe().getNumberOfInvitedFriends() > 0 || (ui.getCore().getSettings().getInternal().getHastriedtoinviteafriend() != null && ui.getCore().getSettings().getInternal().getHastriedtoinviteafriend() != 0)) return;
                            Long tick = ui.getCore().getSettings().getInternal().getLastnaggedaboutinvitingafriend();
                            if (tick == null) {
                                tick = System.currentTimeMillis()-1000l*60*60*24*10;
                                ui.getCore().getSettings().getInternal().setLastnaggedaboutinvitingafriend(tick);
                            } else {
                                if (System.currentTimeMillis()-tick > 1000l*60*60*24*7) {
                                    //more then a week since we nagged last time
                                    if (ui.getCore().getFriendManager().getMe().getTotalBytesReceived() > 800*MB) {
                                        //more then 800mb downloaded
                                        if (ui.getCore().getNetworkManager().getBandwidthIn().getCPS() > ui.getCore().getNetworkManager().getBandwidthIn().getHighestCPS()/3) {
                                            //downloading at fairly high speed - let's show the infomration dialog
                                            ui.getCore().getSettings().getInternal().setLastnaggedaboutinvitingafriend(System.currentTimeMillis());
                                            if (OptionDialog.showQuestionDialog(MainWindow.this,
                                                    "You have not invited any friends to your Alliance network.[p]"+
                                                            "If you invite friends you will be able to download more files faster and the network will become more reliable.[p]"+
                                                            "[b]It is important for all Alliance users to invite at least once friend.[/b][p]"+
                                                            "Would you like to invite a friend to your Alliance network now?[p]")) {
                                                ui.getCore().getSettings().getInternal().setHastriedtoinviteafriend(1);
                                                openWizardAt(AddFriendWizard.STEP_PORT_OPEN_TEST);
                                            }
                                        }
                                    }
                                }
                            }
                        } catch(Exception e) {
                            if(T.t)T.error(e);
                        }
                    }

                    private void updateBandwidth(String s, String s2, JProgressBar pb, BandwidthAnalyzer a) {
                        double curr = a.getCPS();
                        double max = a.getHighestCPS();
                        pb.setString(a.getCPSHumanReadable());
                        pb.setStringPainted(true);
                        if (max == 0)
                            pb.setValue(0);
                        else
                            pb.setValue((int)(curr*100/max));
                        pb.setToolTipText("<html>"+s+" at "+a.getCPSHumanReadable()+"<br>Speed record: "+a.getHighestCPSHumanReadable()+"<br>Total bytes "+s2+": "+ TextUtils.formatByteSize(a.getTotalBytes())+"</html>");
                    }
                });
            }

//            NeedsUserInteraction nui;
//            while((nui = ui.getCore().fetchUserInteraction()) != null) {
//                final NeedsUserInteraction nui1 = nui;
//                try {
//                    SwingUtilities.invokeAndWait(new Runnable() {
//                        public void run() {
//                            handleNeedsUserInteraction(nui1);
//                        }
//                    });
//                } catch (InterruptedException e) {
//                } catch (InvocationTargetException e) {
//                }
//            }

//            if(T.t)T.trace("userInteractionsInProgress: "+userInteractionsInProgress+", in que: "+ui.getCore().getAllUserInteractionsInQue().size());
            final boolean[] removedAUserInteraction = new boolean[]{false};
            for(NeedsUserInteraction nui : ui.getCore().getAllUserInteractionsInQue()) {
                if (userInteractionsInProgress == 0 || nui.canRunInParallelWithOtherInteractions()) {
                    if (nui instanceof NewFriendConnectedUserInteraction && isConnectedToNewFriendDialogShowing) continue; //wait til the dialog is no longer displayed
                    if(T.t)T.info("running user interaction: "+nui);
                    ui.getCore().removeUserInteraction(nui);
                    final NeedsUserInteraction nui1 = nui;
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            handleNeedsUserInteraction(nui1);
                        }
                    });
                    if (!(nui instanceof NewFriendConnectedUserInteraction)) {
                        removedAUserInteraction[0] = true;
                    }
                    break;
                }
            }

            if (!removedAUserInteraction[0]) {
                try { Thread.sleep(1000); } catch (InterruptedException e) {}
            }
        }
    }

    // @todo: move this logic in separate class - it's getting too big
    private void handleNeedsUserInteraction(NeedsUserInteraction nui) {
        userInteractionsInProgress++;
        try {
            if (nui instanceof PostMessageInteraction) {
                PostMessageInteraction pmi = (PostMessageInteraction)nui;
                try {
                    if (pmi instanceof PostMessageToAllInteraction)
                        publicChatMessage(pmi.getFromGuid(), pmi.getMessage(), pmi.getSentAtTick(), pmi.isMessageWasPersisted());
                    else
                        chatMessage(pmi.getFromGuid(), pmi.getMessage(), pmi.getSentAtTick(), pmi.isMessageWasPersisted());
                } catch(Exception e) {
                    ui.handleErrorInEventLoop(e);
                }
            } else if (nui instanceof PleaseForwardInvitationInteraction) {
                final PleaseForwardInvitationInteraction pmi = (PleaseForwardInvitationInteraction)nui;
                try {
                    if (ui.getCore().getSettings().getInternal().getAlwaysallowfriendstoconnect() > 0) {
                        forwardInvitation(pmi);
                    } else {
                        ForwardInvitationDialog d = new ForwardInvitationDialog(ui, pmi); //blocks
                        if (d.hasPressedYes()) forwardInvitation(pmi);
                        if (d.alwaysAllowInvite()) ui.getCore().getSettings().getInternal().setAlwaysallowfriendstoconnect(1);
                    }
                } catch(Exception e) {
                    ui.handleErrorInEventLoop(e);
                }
            } else if (nui instanceof NeedsToRestartBecauseOfUpgradeInteraction) {
                if (!ui.getCore().getAwayManager().isAway()) {
                    OptionDialog.showInformationDialog(this, "A new version of Alliance has been downloaded and installed in the background.[p]Next time you start Alliance the new version will start.");
                } else {
                    try {
                        ui.getCore().restartProgram(true);
                    } catch (IOException e) {
                        ui.handleErrorInEventLoop(e);
                    }
                }
                /*
                this old way of handling new versions was less user friendly - usability testing showed this
                if (ui.getCore().getAwayManager().isAway() || OptionDialog.showQuestionDialog(this, "A new version of Alliance has been downloaded and installed in the background (the upgrade was verified using a 2048 bit RSA certificate).[p] You need to restart Alliance to use the new version. Note that it will take about two minutes before Alliance starts again. Would you like to do this now?[p]")) {
                    try {
                        ui.getCore().restartProgram(true);
                    } catch (IOException e) {
                        ui.handleErrorInEventLoop(e);
                    }
                } else {
                    //wait for 5 minutes and then ask again.
                    Thread t = new Thread(new Runnable() {
                        public void run() {
                            try { Thread.sleep(1000*60*5); } catch(InterruptedException e) {}
                            ui.getCore().invokeLater(new Runnable() {
                                public void run() {
                                    ui.getCore().queNeedsUserInteraction(new NeedsToRestartBecauseOfUpgradeInteraction());
                                }
                            });
                        }
                    });
                    t.start();
                }*/
            } else if (nui instanceof ForwardedInvitationInteraction) {
                ForwardedInvitationInteraction fii = (ForwardedInvitationInteraction)nui;
                if (ui.getCore().getFriendManager().getFriend(fii.getFromGuid()) != null && ui.getCore().getFriendManager().getFriend(fii.getFromGuid()).isConnected()) {
                    if(T.t)T.error("Already was connected to this friend!!");
                } else {
                    if (ui.getCore().getSettings().getInternal().getAutomaticallydenyallinvitations() == 0 && OptionDialog.showQuestionDialog(this, fii.getRemoteName()+" wants to connect to you. "+fii.getRemoteName()+" has a connection to "+fii.getMiddleman(ui.getCore()).getNickname()+". [p]Do you want to connect to "+fii.getRemoteName()+"?[p]")) {
                        try {
                            ui.getCore().getInvitaitonManager().attemptToBecomeFriendWith(fii.getInvitationCode(), fii.getMiddleman(ui.getCore()));
                            openWizardAt(AddFriendWizard.STEP_ATTEMPT_CONNECT, fii.getFromGuid());
                        } catch(Exception e) {
                            ui.handleErrorInEventLoop(e);
                        }
                    } else {
                        //nothing
                    }
                }
            } else if (nui instanceof NewFriendConnectedUserInteraction) {
                NewFriendConnectedUserInteraction i = (NewFriendConnectedUserInteraction)nui;
                String name = ui.getCore().getFriendManager().nickname(i.getGuid());
                if (lastAddFriendWizard != null) lastAddFriendWizard.connectionWasSuccessful();

                if (ui.getCore().doesInterationQueContain(ForwardedInvitationInteraction.class) ||
                        new ForwardInvitationNodesList.ForwardInvitationListModel(ui).getSize() == 0) {
                    if (lastAddFriendWizard != null) lastAddFriendWizard.getOuterDialog().dispose();
                    showSuccessfullyConnectedToNewFriendDialog(name);
                    //after this method completes the next pending interaction will be processed.
                } else {
                    if (ui.getCore().getSettings().getInternal().getAlwaysautomaticallyconnecttoallfriendsoffriend() == 1) {
                        ui.getCore().invokeLater(new Runnable() {
                            public void run() {
                                try {
                                    final ArrayList<ForwardInvitationNodesList.ListRow> al = new ArrayList<ForwardInvitationNodesList.ListRow>();
                                    ForwardInvitationNodesList.ForwardInvitationListModel m = new ForwardInvitationNodesList.ForwardInvitationListModel(ui);
                                    for(int j=0;j<m.getSize();j++) al.add((ForwardInvitationNodesList.ListRow) m.getElementAt(j));
                                    for(ForwardInvitationNodesList.ListRow r : al) {
                                        ui.getCore().getFriendManager().forwardInvitationTo(r.guid);
                                    }
                                } catch (Exception e) {
                                    ui.getCore().reportError(e, this);
                                }
                            }
                        });
                    } else {
                        showSuccessfullyConnectedToNewFriendDialog(name);
                        try {
                            openWizardAt(AddFriendWizard.STEP_FORWARD_INVITATIONS);
                        } catch (Exception e) {
                            ui.handleErrorInEventLoop(e);
                        }
                    }
                }
                try {
                    getFriendListMDIWindow().updateMyLevelInformation();
                } catch (IOException e) {
                    ui.handleErrorInEventLoop(e);
                }
            } else if (nui instanceof FriendAlreadyInListUserInteraction) {
                // FriendAlreadyInListUserInteraction i = (FriendAlreadyInListUserInteraction)nui;
                // String name = ui.getCore().getFriendManager().nickname(i.getGuid());
                if(T.t)T.trace("Last wizard: "+lastAddFriendWizard);
                if (lastAddFriendWizard != null) {
                    lastAddFriendWizard.getOuterDialog().dispose();
                    if(T.t)T.trace("Wizard disposed");
                }
                // no need to display the below. The user should not mind about this.
                //OptionDialog.showInformationDialog(this, "You already have a connection to "+name+". IP-Adress information was updated for this connection.");
            } else {
                System.out.println("unknown: "+nui);
            }
        } finally {
            userInteractionsInProgress--;
        }
    }

    private boolean isConnectedToNewFriendDialogShowing = false;
    private boolean isAddRuleWindowDialogShowing = false;
    public boolean isConnectedToNewFriendDialogShowing() { return isConnectedToNewFriendDialogShowing; }
    public boolean isAddRuleWindowDialogShowing() { return isAddRuleWindowDialogShowing; }
    public void setConnectedToNewFriendDialogShowing(boolean connectedToNewFriendDialogShowing) { isConnectedToNewFriendDialogShowing = connectedToNewFriendDialogShowing; }
    public void setAddRuleWindowDialogShowing(boolean AddruleWindowDialogShowing) { isAddRuleWindowDialogShowing = AddruleWindowDialogShowing; }
    private void showSuccessfullyConnectedToNewFriendDialog(String name) {
        try {
            if (ui.getCore().getSettings().getInternal().getAlwaysautomaticallyconnecttoallfriendsoffriend() == 0) {
                new ConnectedToNewFriendDialog(ui, this, name);
            }
        } catch (Exception e) {
            ui.handleErrorInEventLoop(e);
        }
    }

    public void forwardInvitation(final PleaseForwardInvitationInteraction pmi) {
        ui.getCore().invokeLater(new Runnable() {
            public void run() {
                try {
                    ui.getCore().getFriendManager().forwardInvitation(pmi);
                } catch(final IOException e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            ui.handleErrorInEventLoop(e);
                        }
                    });
                }
            }
        });
    }

    public void EVENT_options(ActionEvent e) throws Exception {
        new OptionsWindow(ui);
//        String s = JOptionPane.showInputDialog("Enter upload throttle, in kb/s (kilobyte per second).\n0 means no limit.\nIf you have 1mbit upstream a value of 70 is recommended.\nThis value will be saved in your configuration file.", ui.getCore().getSettings().getInternal().getUploadthrottle()/KB);
//        if (s != null) {
//            int limit = Integer.parseInt(s.trim());
//            ui.getCore().getSettings().getInternal().setUploadthrottle(limit);
//            ui.getCore().saveSettings();
//            ui.getCore().getNetworkManager().getUploadThrottle().setRate(limit*KB);
//        }
    }

    public void EVENT_addshare(ActionEvent e) throws Exception {
        new OptionsWindow(ui, true);
    }

    public void EVENT_trace(ActionEvent e) throws Exception {
        if (!org.alliance.T.t) {
            OptionDialog.showInformationDialog(this, "The trace has been disabled in this build of Alliance.");
        } else {
            createTraceWindow();
        }
    }

/*    public void EVENT_shutdown6(ActionEvent e) throws Exception{
        ui.getCore().restartProgram(false, 6*60);
    }
    
    public void EVENT_shutdown3(ActionEvent e) throws Exception{
        ui.getCore().restartProgram(false, 3*60);
    }

    public void EVENT_shutdown1(ActionEvent e) throws Exception{
        ui.getCore().restartProgram(false, 60);
    }

    public void EVENT_shutdown30(ActionEvent e) throws Exception{
        ui.getCore().restartProgram(false, 30);
    }

    public void EVENT_shutdownForever(ActionEvent e) throws Exception{
        if(JOptionPane.showConfirmDialog(null, "Are you sure you wish to close Alliance?", "Are you sure?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
            if (ui.getCore() != null) {
                ui.getCore().shutdown();
            }

            if (ui != null) {
                ui.shutdown();
                ui = null;
            }
            System.exit(0);
        }
    }*/

    public void EVENT_addally(ActionEvent e) throws IOException {
        String invitation = JOptionPane.showInputDialog(ui.getMainWindow(), "Enter the connection code you got from your friend: ");
        try {
            if (invitation != null) ui.getCore().getInvitaitonManager().attemptToBecomeFriendWith(invitation.trim(), null, 0);
        } catch(EOFException ex) {
            OptionDialog.showErrorDialog(this, "Your connection code is corrupt. It seems to be too short. Maybe you did not enter all characters? Please try again. If that doesn't help try with a new code.");
        }
    }

    public void EVENT_rescan(ActionEvent e) {
        ui.getCore().getShareManager().getShareScanner().startScan(true);
    }

    public void EVENT_console(ActionEvent e) throws Exception {
        mdiManager.addWindow(new ConsoleMDIWindow(ui));
    }

    public void EVENT_connections(ActionEvent e) throws Exception {
        mdiManager.addWindow(new ConnectionsMDIWindow(ui));
    }

    public void EVENT_changelog(ActionEvent e) throws Exception {
        mdiManager.addWindow(new WelcomeMDIWindow(ui));
    }

    public void EVENT_uploads(ActionEvent e) throws Exception {
        mdiManager.addWindow(new UploadsMDIWindow(ui));
    }

    public void EVENT_dups(ActionEvent e) throws Exception {
        mdiManager.addWindow(new DuplicatesMDIWindow(ui));
    }

    public void EVENT_friendtree(ActionEvent e) throws Exception {
        if (UISubsystem.NODE_TREE_MODEL_DISABLED) {
            OptionDialog.showInformationDialog(this, "The network topology is disabled because it is buggy and can cause a network to crash.");
        } else {
            mdiManager.addWindow(new FriendsTreeMDIWindow(mdiManager, ui));
        }
    }

    public void EVENT_addfriendwizard(ActionEvent e) throws Exception {
        openWizard();
    }

    public void openWizard() throws Exception {
        if(T.t)T.ass(lastAddFriendWizard == null || !lastAddFriendWizard.getOuterDialog().isVisible(), "Wizard already open!");
        lastAddFriendWizard = AddFriendWizard.open(ui, AddFriendWizard.STEP_INTRO);
        lastAddFriendWizard.getOuterDialog().display();
    }

    public void openWizardAt(int step, Integer invitationFromGuid) throws Exception {
        if (lastAddFriendWizard != null) if(T.t)T.trace("visible: "+lastAddFriendWizard.getOuterDialog().isVisible());
        if (lastAddFriendWizard != null && lastAddFriendWizard.getOuterDialog().isVisible()) {
            if(T.t)T.ass(step == AddFriendWizard.STEP_FORWARD_INVITATIONS || step == AddFriendWizard.STEP_ATTEMPT_CONNECT || step == AddFriendWizard.STEP_PORT_OPEN_TEST , "No support for starting at step "+step+" like this");
            lastAddFriendWizard.setInvitationFromGuid(invitationFromGuid);
            if (step == AddFriendWizard.STEP_FORWARD_INVITATIONS)
                lastAddFriendWizard.goToForwardInvitations();
            else if (step == AddFriendWizard.STEP_ATTEMPT_CONNECT)
                lastAddFriendWizard.goToAttemptConnect();
            else
                lastAddFriendWizard.goToPortTest();
        } else {
            lastAddFriendWizard = AddFriendWizard.open(ui, step);
            lastAddFriendWizard.setInvitationFromGuid(invitationFromGuid);
            lastAddFriendWizard.getOuterDialog().display();
        }
    }

    public void openWizardAt(int step) throws Exception {
        openWizardAt(step, null);
    }

    public void shareBaseListReceived(Friend friend, String[] shareBaseNames) {
        if (friend == null) return;
        ViewShareMDIWindow w = (ViewShareMDIWindow)mdiManager.getWindow("viewshare"+friend.getGuid());
        if (w == null) {
            if(T.t)T.error("Could not find view share window for "+friend);
        } else {
            w.shareBaseListReceived(shareBaseNames);
        }
    }

    public void directoryListingReceived(Friend friend, int shareBaseIndex, String path, String[] files) {
        if (friend == null) return;
        ViewShareMDIWindow w = (ViewShareMDIWindow)mdiManager.getWindow("viewshare"+friend.getGuid());
        if (w == null) {
            if(T.t)T.error("Could not find view share window for "+friend);
        } else {
            w.directoryListingReceived(shareBaseIndex, path, files);
        }
    }

    public PublicChatMessageMDIWindow getPublicChat() {
        return publicChat;
    }
}
