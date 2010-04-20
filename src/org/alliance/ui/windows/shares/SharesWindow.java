package org.alliance.ui.windows.shares;

import com.stendahls.XUI.XUIDialog;
import com.stendahls.util.TextUtils;
import org.alliance.core.settings.Share;
import org.alliance.core.LanguageResource;
import org.alliance.ui.UISubsystem;
import org.alliance.ui.dialogs.AddGroupDialog;
import org.alliance.ui.dialogs.OptionDialog;
import org.alliance.ui.themes.util.SubstanceThemeHelper;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.TreeSet;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.MenuEvent;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.tree.MutableTreeNode;
import javax.swing.JTree;
import javax.swing.event.MenuListener;
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
    private JMenu addGroupMenu;
    private JMenu removeGroupMenu;
    private MouseAdapter addGroupAdapter;
    private MouseAdapter removeGroupAdapter;
    private TreeSet<String> groups = new TreeSet<String>();
    private boolean shareListHasBeenModified = false;
    private final static String PUBLIC_GROUP = "Public";
    private final static String GROUP_SEPARATOR = ",";

    public SharesWindow(UISubsystem ui) throws Exception {
        super(ui.getMainWindow());
        this.ui = ui;

        init(ui.getRl(), ui.getRl().getResourceStream("xui/shareswindow.xui.xml"));
        LanguageResource.translateXUIElements(getClass(), xui.getXUIComponents());
        SubstanceThemeHelper.setButtonsToGeneralArea(xui.getXUIComponents());
        setTitle(LanguageResource.getLocalizedString(getClass(), "title"));

        setupPopupMenu();
        setupShareTree();
        setupShareList();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        display();
    }

    private void setupPopupMenu() {
        popupList = (JPopupMenu) xui.getComponent("popupList");
        popupTree = (JPopupMenu) xui.getComponent("popupTree");
        addGroupMenu = (JMenu) xui.getComponent("addgroup");
        removeGroupMenu = (JMenu) xui.getComponent("removegroup");

        removeGroupMenu.addMenuListener(new MenuListener() {

            @Override
            public void menuSelected(MenuEvent e) {
                String groupString = shareListModel.elementAt(shareList.getSelectedIndex() + 1).toString();
                ArrayList<String> groupList = new ArrayList<String>();
                for (String group : groupString.split(GROUP_SEPARATOR)) {
                    groupList.add(group);
                }
                for (Component c : removeGroupMenu.getMenuComponents()) {
                    if (c instanceof JMenuItem) {
                        JMenuItem menuItem = (JMenuItem) c;
                        if (groupList.contains(menuItem.getText())) {
                            menuItem.setVisible(true);
                        } else {
                            menuItem.setVisible(false);
                        }
                    } else {
                        break;
                    }
                }
            }

            @Override
            public void menuDeselected(MenuEvent e) {
            }

            @Override
            public void menuCanceled(MenuEvent e) {
            }
        });

        addGroupMenu.addMenuListener(new MenuListener() {

            @Override
            public void menuSelected(MenuEvent e) {
                String groupString = shareListModel.elementAt(shareList.getSelectedIndex() + 1).toString();
                ArrayList<String> groupList = new ArrayList<String>();
                for (String group : groupString.split(GROUP_SEPARATOR)) {
                    groupList.add(group);
                }
                for (Component c : addGroupMenu.getMenuComponents()) {
                    if (c instanceof JMenuItem) {
                        JMenuItem menuItem = (JMenuItem) c;
                        if (!groupList.contains(menuItem.getText())) {
                            menuItem.setVisible(true);
                        } else {
                            menuItem.setVisible(false);
                        }
                    } else {
                        break;
                    }
                }
            }

            @Override
            public void menuDeselected(MenuEvent e) {
            }

            @Override
            public void menuCanceled(MenuEvent e) {
            }
        });

        addGroupAdapter = new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent evt) {
                int groupRowId = shareList.getSelectedIndex() + 1;
                String existingsGroups = shareListModel.elementAt(groupRowId).toString();
                existingsGroups = existingsGroups.replace(PUBLIC_GROUP, "");
                String groupString = sortGroupNames(existingsGroups + GROUP_SEPARATOR + ((JMenuItem) evt.getComponent()).getText());
                shareListModel.setElementAt(groupString, groupRowId);
            }
        };

        removeGroupAdapter = new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent evt) {
                int groupRowId = shareList.getSelectedIndex() + 1;
                String existingsGroups = shareListModel.elementAt(groupRowId).toString();
                existingsGroups = existingsGroups.replace(((JMenuItem) evt.getComponent()).getText(), "");
                String groupString = sortGroupNames(existingsGroups);
                shareListModel.setElementAt(groupString, groupRowId);
            }
        };
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

            @Override
            public void mouseClicked(MouseEvent e) {
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
        shareList.setCellRenderer(new SharesListCellRenderer(LanguageResource.getLocalizedString(getClass(), "group")).getRenderer());
        shareList.setModel(shareListModel);
        for (Share share : ui.getCore().getSettings().getSharelist()) {
            shareListModel.addElement(share.getPath());
            shareListModel.addElement(share.getSgroupname());
            //Store all groups from this share
            for (String group : share.getSgroupname().split(GROUP_SEPARATOR)) {
                if (!group.equals(PUBLIC_GROUP)) {
                    groups.add(sortGroupNames(group));
                }
            }
        }
        rebuildGroupMenu();

        shareList.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
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
                        popupList.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });
    }

    private void rebuildGroupMenu() {
        addGroupMenu.removeAll();
        removeGroupMenu.removeAll();
        for (String group : groups) {
            JMenuItem addMenu = new JMenuItem(group);
            JMenuItem removeMenu = new JMenuItem(group);
            addMenu.addMouseListener(addGroupAdapter);
            removeMenu.addMouseListener(removeGroupAdapter);
            addGroupMenu.add(addMenu);
            removeGroupMenu.add(removeMenu);
        }
        addGroupMenu.addSeparator();
        removeGroupMenu.addSeparator();
        addGroupMenu.add(xui.getComponent("addnewgroup"));
        removeGroupMenu.add(xui.getComponent("settopublic"));
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
        File selectedDir = new File(TextUtils.makeSurePathIsMultiplatform(path.toString()));

        for (int i = 0; i < shareListModel.getSize(); i += 2) {
            if (shareListModel.getElementAt(i).toString().equalsIgnoreCase(path.toString())) {
                return;
            }
        }

        if (selectedDir.exists() || selectedDir.isDirectory()) {
            shareListModel.addElement(selectedDir.getAbsolutePath());
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

    public void EVENT_addnewgroup(ActionEvent a) throws Exception {
        String newGroup = createNewGroup();
        if (newGroup == null || newGroup.equalsIgnoreCase(PUBLIC_GROUP)) {
            return;
        }
        int groupRowId = shareList.getSelectedIndex() + 1;
        String existingsGroups = shareListModel.elementAt(groupRowId).toString();
        existingsGroups = existingsGroups.replace(PUBLIC_GROUP, "");
        String groupString = sortGroupNames(existingsGroups + GROUP_SEPARATOR + newGroup);
        shareListModel.setElementAt(groupString, groupRowId);
        groups.add(sortGroupNames(newGroup));
        rebuildGroupMenu();
    }

    private String createNewGroup() throws Exception {
        AddGroupDialog groupDialog = new AddGroupDialog(ui, null);
        String groupString = groupDialog.getGroupname();
        if (groupString == null || groupString.trim().length() == 0) {
            return null;
        }
        if (groupString.contains(GROUP_SEPARATOR)) {
            groupString = groupString.replace(GROUP_SEPARATOR, "");
        }
        return groupString.trim();
    }

    private String sortGroupNames(String groupString) {
        TreeSet<String> groupSorted = new TreeSet<String>();
        String[] groupsSplit = groupString.split(GROUP_SEPARATOR);
        for (String group : groupsSplit) {
            group = group.trim().toLowerCase();
            if (group.length() > 1) {
                //Uppercase 1st letter rest Lowercase 
                groupSorted.add(Character.toUpperCase(group.charAt(0)) + group.substring(1));
            } else if (group.length() == 1) {
                groupSorted.add(group.toUpperCase());
            }
        }
        StringBuilder sb = new StringBuilder();
        for (String group : groupSorted) {
            sb.append(group.trim());
            sb.append(GROUP_SEPARATOR);
        }
        try {
            sb.deleteCharAt(sb.length() - 1);
        } catch (StringIndexOutOfBoundsException ex) {
            return PUBLIC_GROUP;
        }
        return sb.toString();
    }

    public void EVENT_locate(ActionEvent a) throws Exception {
        String sharePath = shareList.getSelectedValue().toString();
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) sharesTreeModel.getRoot();
        browseTreeNodes(rootNode, sharePath, true);
    }

    private void browseTreeNodes(DefaultMutableTreeNode node, String sharePath, boolean isRoot) {
        for (int i = 1; i <= node.getChildCount(); i++) {
            //Browse from last child to first
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(node.getChildCount() - i);
            if (sharePath.startsWith(childNode.toString())) {
                sharePath = sharePath.substring(childNode.toString().length());
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

    public void EVENT_ok(ActionEvent a) throws Exception {
        ui.getCore().getSettings().getSharelist().clear();
        for (int i = 0; i < shareListModel.size(); i += 2) {
            ui.getCore().getSettings().getSharelist().add(new Share(shareListModel.getElementAt(i).toString(), shareListModel.getElementAt(i + 1).toString()));
        }
        ui.getCore().getShareManager().updateShareBases();
        if (shareListHasBeenModified) {
            ui.getCore().getShareManager().getShareScanner().startScan(true);
        }
        dispose();
    }

    public void EVENT_cancel(ActionEvent a) throws Exception {
        dispose();
    }
}
