package org.alliance.ui.addfriendwizard;

import org.alliance.core.node.Friend;
import org.alliance.core.node.UntrustedNode;
import org.alliance.core.node.Invitation;
import org.alliance.ui.UISubsystem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-apr-19
 * Time: 12:39:16
 * To change this template use File | Settings | File Templates.
 */
public class ForwardInvitationNodesList extends JList {
	private UISubsystem ui;
    private AddFriendWizard addFriendWizard;

    public ForwardInvitationNodesList(UISubsystem ui, final AddFriendWizard addFriendWizard) {
        super(new ForwardInvitationListModel(ui));
        this.ui = ui;
        this.addFriendWizard = addFriendWizard;

        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int index = locationToIndex(e.getPoint());
                if (index == -1) return;
                ListRow item = (ListRow) getModel().getElementAt(index);
                item.selected = !item.selected;
                if (item.selected) addFriendWizard.enableNext();
                Rectangle rect = getCellBounds(index, index);
                repaint(rect);
            }
        });
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setCellRenderer(new CheckListRenderer());
    }

    public void selectAll() {
        for (int i = 0; i < getModel().getSize(); i++) {
            ((ListRow) getModel().getElementAt(i)).selected = true;
            addFriendWizard.enableNext();
        }

        repaint();
    }

    private static class CheckListRenderer extends JCheckBox implements ListCellRenderer {

		public CheckListRenderer() {
            setBackground(UIManager.getColor("List.textBackground"));
            setForeground(UIManager.getColor("List.textForeground"));
        }

        public Component getListCellRendererComponent(JList list, Object value,
                                                      int index, boolean isSelected, boolean hasFocus) {
            setEnabled(list.isEnabled());
            setSelected(((ListRow) value).selected);
            setFont(list.getFont());
            setText(value.toString());
            return this;
        }
    }

    public static class ListRow {
        public String nickname, connectedThrough;
        public int guid;
        public String toString;
        public boolean selected;

        public ListRow(String nickname, String connectedThrough, int guid) {
            this.nickname = nickname;
            this.connectedThrough = connectedThrough;
            this.guid = guid;

            toString = "<html>" + nickname + " <font color=gray>(connected to " + connectedThrough + ")</font></html>";
        }

        public String toString() {
            return toString;
        }
    }

    public static class ForwardInvitationListModel extends DefaultListModel {
		private UISubsystem ui;

        public ForwardInvitationListModel(final UISubsystem ui) {
            this.ui = ui;

            TreeSet<Integer> secondaryNodeGuids = new TreeSet<Integer>(new Comparator<Integer>() {
                public int compare(Integer o1, Integer o2) {
                    String n1 = ui.getCore().getFriendManager().nickname(o1);
                    String n2 = ui.getCore().getFriendManager().nickname(o2);
                    if (n1.compareToIgnoreCase(n2) == 0) {
                        return o1.compareTo(o2);
                    } else {
                        return n1.compareToIgnoreCase(n2);
                    }
                }
            });
            Collection<Friend> friends = ui.getCore().getFriendManager().friends();
            for (Friend f : friends.toArray(new Friend[friends.size()])) {
                if (f.friendsFriends() != null) {
                    Collection<UntrustedNode> ff = f.friendsFriends();
                    for (UntrustedNode n : ff.toArray(new UntrustedNode[ff.size()])) {
                        if (ui.getCore().getFriendManager().getFriend(n.getGuid()) == null &&
                                ui.getCore().getFriendManager().getMyGUID() != n.getGuid())
                            secondaryNodeGuids.add(n.getGuid());
                    }
                }
            }

            for(Invitation i : ui.getCore().getInvitaitonManager().allInvitations()) {
                if (i.getDestinationGuid() == null) continue;
                if (ui.getCore().getInvitaitonManager().hasBeenRecentlyInvited(i)) {
                    secondaryNodeGuids.remove(i.getDestinationGuid()); //often the guid won't exist in the list - that's fine.
                }
            }

            for (int guid : secondaryNodeGuids) {
                addElement(new ListRow(ui.getCore().getFriendManager().nickname(guid), createConnectedThroughList(guid), guid));
            }
        }

        private String createConnectedThroughList(int guid) {
            String s = "";
            for (Friend f : ui.getCore().getFriendManager().friends()) {
                if (f.getFriendsFriend(guid) != null) {
                    if (s.length() > 0) s += ", ";
                    s += f.getNickname();
                }
            }
            return s;
        }
    }

    public void forwardSelectedInvitations() {
        for (int i = 0; i < getModel().getSize(); i++) {
            final ListRow r = (ListRow) getModel().getElementAt(i);
            if (r.selected) {
                ui.getCore().invokeLater(new Runnable() {
                    public void run() {
                        try {
                            ui.getCore().getFriendManager().forwardInvitationTo(r.guid);
                        } catch (Exception e) {
                            ui.getCore().reportError(e, this);
                        }
                    }
                });
            }
        }
    }
}
