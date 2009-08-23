package org.alliance.ui.windows;

import com.stendahls.nif.ui.framework.JTreeFoldStateManager;
import com.stendahls.nif.ui.mdi.MDIManager;
import com.stendahls.nif.ui.mdi.MDIWindow;
import org.alliance.core.node.Friend;
import org.alliance.core.node.MyNode;
import org.alliance.ui.UISubsystem;
import org.alliance.ui.nodetreemodel.NodeTreeNode;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-30
 * Time: 16:22:07
 */
public class FriendsTreeMDIWindow extends AllianceMDIWindow  {
	private UISubsystem ui;
    private JTree tree;

    private ImageIcon iconMe, iconFriend, iconNode, iconFriendDimmed, iconNodeDimmed, iconRecursion;

    public FriendsTreeMDIWindow() {
    }

    public FriendsTreeMDIWindow(MDIManager manager, UISubsystem ui) throws Exception {
        super(manager, "friends", ui);
        this.ui = ui;

        iconMe = new ImageIcon(ui.getRl().getResource("gfx/icons/me.png"));
        iconFriend = new ImageIcon(ui.getRl().getResource("gfx/icons/friend.png"));
        iconFriendDimmed = new ImageIcon(ui.getRl().getResource("gfx/icons/friend_dimmed.png"));
        iconNode = new ImageIcon(ui.getRl().getResource("gfx/icons/node.png"));
        iconNodeDimmed = new ImageIcon(ui.getRl().getResource("gfx/icons/node_dimmed.png"));
        iconRecursion = new ImageIcon(ui.getRl().getResource("gfx/icons/recursion.png"));

        createUI();
        setTitle("Network Topology");
    }

    private void createUI() throws Exception {
        ((JLabel)xui.getComponent("status")).setText(" ");

        tree = new JTree(ui.getNodeTreeModel(true));
        tree.setCellRenderer(new FriendCellRenderer());
        ((JScrollPane)xui.getComponent("treepanel")).setViewportView(tree);

        postInit();
    }

    public void save() throws Exception {
    }

    public String  getIdentifier() {
        return "friends";
    }

    public void revert() throws Exception {
        Set<Object> set = JTreeFoldStateManager.saveState(tree);
        ui.purgeNodeTreeModel();
        createUI();
        JTreeFoldStateManager.restoreState(tree, set);
    }

    public void serialize(ObjectOutputStream out) throws IOException {
    }

    public MDIWindow deserialize(ObjectInputStream in) throws IOException {
        return null;
    }

    public void EVENT_chat(ActionEvent e) throws Exception {
        if (tree.getSelectionPath() == null) return;
        NodeTreeNode n = (NodeTreeNode)tree.getSelectionPath().getLastPathComponent();
        if (n != null) {
            if (n.getNode() != null) ui.getMainWindow().chatMessage(n.getNode().getGuid(), null, 0, false);
        }
    }

    private class FriendCellRenderer extends DefaultTreeCellRenderer {

		public Component getTreeCellRendererComponent(
                JTree tree,
                Object value,
                boolean sel,
                boolean expanded,
                boolean leaf,
                int row,
                boolean hasFocus) {

            super.getTreeCellRendererComponent(
                    tree, value, sel,
                    expanded, leaf, row,
                    hasFocus);

            NodeTreeNode n = (NodeTreeNode)value;


            if (n.getNode() != null && n.getNode().isConnected()) {
                setForeground(Color.black);
                if (n.getNode() instanceof Friend) {
                    setIcon(iconFriend);
                } else {
                    setIcon(iconNode);
                }
            } else {
                setForeground(Color.lightGray);
                if (n.getNode() == null) {
                    setIcon(iconRecursion);
                } else if (n.getNode() instanceof Friend) {
                    setIcon(iconFriendDimmed);
                } else {
                    setIcon(iconNodeDimmed);
                }
            }

            if (n.getNode() instanceof MyNode) setIcon(iconMe);

            if (sel) setForeground(Color.white);

            return this;
        }
    }
}
