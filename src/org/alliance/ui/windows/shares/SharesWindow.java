package org.alliance.ui.windows.shares;

import com.stendahls.XUI.XUIDialog;
import com.stendahls.util.TextUtils;
import org.alliance.core.settings.Share;
import org.alliance.core.LanguageResource;
import org.alliance.ui.UISubsystem;
import org.alliance.ui.dialogs.OptionDialog;
import org.alliance.ui.themes.util.SubstanceThemeHelper;
import org.alliance.ui.windows.EditGroupWindow;

import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.tree.MutableTreeNode;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public class SharesWindow extends XUIDialog {

    private UISubsystem ui;
    private JTree sharesTree;
    private DefaultTreeModel sharesTreeModel;
    private JList shareList;
    private DefaultListModel shareListModel;
    private JPopupMenu popupList;
    private JPopupMenu popupTree;
    private boolean shareListHasBeenModified = false;
    private final static String PUBLIC_GROUP = "Public";

    public SharesWindow(UISubsystem ui) throws Exception {
        super(ui.getMainWindow());
        this.ui = ui;

        init(ui.getRl(), ui.getRl().getResourceStream("xui/shareswindow.xui.xml"));
        LanguageResource.translateXUIElements(getClass(), xui.getXUIComponents());
        SubstanceThemeHelper.setButtonsToGeneralArea(xui.getXUIComponents());
        setTitle(LanguageResource.getLocalizedString(getClass(), "title"));

        popupList = (JPopupMenu) xui.getComponent("popupList");
        popupTree = (JPopupMenu) xui.getComponent("popupTree");
        setupShareTree();
        setupShareList();
        setupQuickJump();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        display();
    }

    private void setupShareTree() {
        sharesTree = (JTree) xui.getComponent("sharesTreeBrowse");
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        sharesTree.setRootVisible(false);
        sharesTree.setShowsRootHandles(true);
        sharesTreeModel = new DefaultTreeModel(root);
        sharesTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        //Insert roots
        File[] roots = File.listRoots();
        for (int i = 0; i < roots.length; i++) {
            DefaultMutableTreeNode rootPath = new DefaultMutableTreeNode(roots[i].toString());
            rootPath.insert(new DefaultMutableTreeNode(LanguageResource.getLocalizedString(getClass(), "loadnode")), 0);
            sharesTreeModel.insertNodeInto(rootPath, root, i);
        }
        sharesTree.setModel(sharesTreeModel);

        sharesTree.addTreeExpansionListener(new TreeExpansionListener() {

            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                DefaultMutableTreeNode selectedDirNode = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();

                if (selectedDirNode.getFirstChild().toString().equals(LanguageResource.getLocalizedString(getClass(), "loadnode"))) {
                    //Get path from selection
                    Object[] pathParts = event.getPath().getPath();
                    StringBuilder path = new StringBuilder();
                    path.append(pathParts[1].toString());
                    for (int i = 2; i < pathParts.length; i++) {
                        path.append(pathParts[i].toString());
                        path.append("/");
                    }

                    File selectedDir = new File(TextUtils.makeSurePathIsMultiplatform(path.toString()));
                    if (selectedDir.listFiles() != null) {
                        int i = 1;
                        for (File file : selectedDir.listFiles()) {
                            if (file.isDirectory() && !file.isHidden()) {
                                DefaultMutableTreeNode newDirNode = new DefaultMutableTreeNode(file.getName());
                                newDirNode.insert(new DefaultMutableTreeNode(LanguageResource.getLocalizedString(getClass(), "loadnode")), 0);
                                sharesTreeModel.insertNodeInto(newDirNode, selectedDirNode, i);
                                i++;
                            }
                        }
                    }
                    sharesTreeModel.removeNodeFromParent((MutableTreeNode) selectedDirNode.getFirstChild());
                }
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
            }
        });

        sharesTree.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = sharesTree.getClosestRowForLocation(e.getX(), e.getY());
                    sharesTree.setSelectionRow(row);
                    //Hide popup for roots
                    if (sharesTree.getSelectionPath().getPath().length > 2) {
                        popupTree.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });
    }

    private void setupShareList() {
        shareList = (JList) xui.getComponent("sharesListSelected");
        shareListModel = new DefaultListModel();
        shareList.setCellRenderer(new SharesListCellRenderer(LanguageResource.getLocalizedString(getClass(), "group")));
        shareList.setModel(shareListModel);
        for (Share share : ui.getCore().getSettings().getSharelist()) {
            shareListModel.addElement(share.getPath());
            shareListModel.addElement(share.getSgroupname());
        }

        shareList.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            private int selectionHelper(MouseEvent e) {
                int row = shareList.locationToIndex(e.getPoint());
                if (row % 2 == 1) {
                    row--;
                }
                return row;
            }

            private void maybeShowPopup(MouseEvent e) {
                int[] indicates = {selectionHelper(e), selectionHelper(e) + 1};
                shareList.setSelectedIndices(indicates);
                if (e.isPopupTrigger()) {
                    if (shareList.getSelectedIndex() != -1) {
                        for (int i = 1; i < popupList.getComponentCount(); i++) {
                            popupList.getComponent(i).setEnabled(true);
                        }
                        popupList.show(e.getComponent(), e.getX(), e.getY());
                    } else if (shareListModel.isEmpty()) {
                        for (int i = 1; i < popupList.getComponentCount(); i++) {
                            popupList.getComponent(i).setEnabled(false);
                        }
                        popupList.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });
    }

    private void setupQuickJump() {
        ((JTextField) xui.getComponent("quick")).addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) sharesTreeModel.getRoot();
                    browseTreeNodes(rootNode, ((JTextField) e.getSource()).getText().toLowerCase(), true);
                }
            }
        });

    }

    public void EVENT_addshare(ActionEvent a) throws Exception {
        Object[] pathParts = sharesTree.getSelectionPath().getPath();
        StringBuilder path = new StringBuilder();
        path.append(pathParts[1].toString());
        for (int i = 2; i < pathParts.length; i++) {
            path.append(pathParts[i].toString());
            path.append("/");
        }
        path.deleteCharAt(path.length() - 1);
        addNewSharePath(new File(TextUtils.makeSurePathIsMultiplatform(path.toString())));
    }

    private void addNewSharePath(File selectedDir) {
        if (selectedDir.exists() && selectedDir.isDirectory()) {
            String path = selectedDir.getAbsolutePath();
            for (int i = 0; i < shareListModel.getSize(); i += 2) {
                if (shareListModel.getElementAt(i).toString().equalsIgnoreCase(path)) {
                    return;
                }
            }
            shareListModel.addElement(path);
            shareListModel.addElement(PUBLIC_GROUP);
        }
        while (removeDuplicateShare()) {
        }
        shareListHasBeenModified = true;
    }

    private boolean removeDuplicateShare() {
        for (int i = 0; i < shareListModel.size(); i += 2) {
            String shareRow = shareListModel.getElementAt(i).toString();
            ArrayList<String> shares = new ArrayList<String>();
            for (int j = 0; j < shareListModel.size(); j += 2) {
                shares.add(shareListModel.getElementAt(j).toString());
            }
            shares.add(ui.getCore().getSettings().getInternal().getDownloadfolder());
            for (String share : shares) {
                String pathDir = TextUtils.makeSurePathIsMultiplatform(new File(shareRow).getAbsolutePath());
                String checkPathDir = TextUtils.makeSurePathIsMultiplatform(new File(share).getAbsolutePath());
                if (!checkPathDir.equals(pathDir) && pathContains(pathDir, checkPathDir)) {
                    OptionDialog.showInformationDialog(ui.getMainWindow(), LanguageResource.getLocalizedString(getClass(),
                            "subshare", pathDir, checkPathDir));
                    shareListModel.removeElementAt(i);
                    shareListModel.removeElementAt(i);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean pathContains(String path, String file) {
        String s1[] = TextUtils.makeSurePathIsMultiplatform(path).split("/");
        String s2[] = TextUtils.makeSurePathIsMultiplatform(file).split("/");
        if (s1.length < s2.length) {
            return false;
        }

        for (int i = 0; i < s2.length; i++) {
            if (!s1[i].equals(s2[i])) {
                return false;
            }
        }
        return true;
    }

    public void EVENT_changegroup(ActionEvent a) throws Exception {
        int groupRowId = shareList.getSelectedIndex() + 1;
        EditGroupWindow editWindow = new EditGroupWindow(ui, shareListModel.elementAt(groupRowId).toString());
        String groupString = editWindow.getGroupString();
        if (groupString == null) {
            return;
        }
        if (groupString.isEmpty()) {
            shareListModel.setElementAt(PUBLIC_GROUP, groupRowId);
        } else {
            shareListModel.setElementAt(groupString, groupRowId);
        }
    }

    public void EVENT_locate(ActionEvent a) throws Exception {
        String sharePath = shareList.getSelectedValue().toString();
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) sharesTreeModel.getRoot();
        browseTreeNodes(rootNode, sharePath.toLowerCase(), true);
    }

    private void browseTreeNodes(DefaultMutableTreeNode node, String sharePath, boolean isRoot) {
        for (int i = 1; i <= node.getChildCount(); i++) {
            //Browse from last child to first
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(node.getChildCount() - i);
            String childName = childNode.toString().toLowerCase();
            if (sharePath.startsWith(childName)) {
                sharePath = sharePath.substring(childName.length());
                TreePath nodePath = new TreePath(childNode.getPath());
                if (sharePath.isEmpty()) {
                    //We have found target dir
                    sharesTree.setSelectionPath(nodePath);
                    sharesTree.scrollPathToVisible(nodePath);
                    return;
                }
                if (!isRoot) {
                    //Remove file separator
                    sharePath = sharePath.substring(1);
                }
                sharesTree.expandPath(nodePath);
                browseTreeNodes(childNode, sharePath, false);
            } else {
                //TODO Target dir have been removed info or something
            }
        }
    }

    public void EVENT_removeshare(ActionEvent a) throws Exception {
        while (shareList.getSelectedIndices().length != 0) {
            shareListModel.removeElementAt(shareList.getSelectedIndex());
        }
        shareListHasBeenModified = true;
    }

    public void EVENT_settopublic(ActionEvent a) throws Exception {
        int groupRowId = shareList.getSelectedIndex() + 1;
        shareListModel.setElementAt(PUBLIC_GROUP, groupRowId);
    }

    public void EVENT_addbrowse(ActionEvent a) throws Exception {
        JFileChooser fc = new JFileChooser(shareListModel.getSize() > 0
                ? shareListModel.getElementAt(shareListModel.getSize() - 2).toString()
                : ".");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String path = fc.getSelectedFile().getAbsolutePath();
            if (!new File(path).exists()) {
                path = new File(new File(path).getParent()).getAbsolutePath();
                addNewSharePath(new File(TextUtils.makeSurePathIsMultiplatform(path)));
                return;
            }
            addNewSharePath(new File(TextUtils.makeSurePathIsMultiplatform(path)));
        }
    }

    private void saveShares() throws Exception {
        ui.getCore().getSettings().getSharelist().clear();
        for (int i = 0; i < shareListModel.size(); i += 2) {
            ui.getCore().getSettings().getSharelist().add(new Share(shareListModel.getElementAt(i).toString(), shareListModel.getElementAt(i + 1).toString()));
        }
        ui.getCore().getShareManager().updateShareBases();
        ui.getCore().saveSettings();
    }

    public void EVENT_ok(ActionEvent a) throws Exception {
        saveShares();
        if (shareListHasBeenModified) {
            ui.getCore().getShareManager().getShareScanner().startScan(true, true);
        }
        dispose();
    }

    public void EVENT_apply(ActionEvent a) throws Exception {
        saveShares();
    }

    public void EVENT_cancel(ActionEvent a) throws Exception {
        dispose();
    }
}
