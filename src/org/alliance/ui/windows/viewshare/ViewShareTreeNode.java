package org.alliance.ui.windows.viewshare;

import com.stendahls.nif.ui.OptionDialog;
import com.stendahls.nif.ui.framework.IdentifiableTreeNode;
import com.stendahls.nif.util.EnumerationIteratorConverter;
import org.alliance.core.comm.rpc.GetDirectoryListing;
import org.alliance.core.node.Friend;
import org.alliance.core.node.Node;
import org.alliance.core.file.share.ShareBase;
import org.alliance.ui.T;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-aug-10
 * Time: 18:16:51
 */
public abstract class ViewShareTreeNode implements IdentifiableTreeNode {
    protected String name;
    protected ViewShareRootNode root;
    protected ViewShareTreeNode parent;
    protected boolean hasSentQueryForChildren = false;

    protected ArrayList<ViewShareFileNode> children = new ArrayList<ViewShareFileNode>();

    public ViewShareTreeNode(String name, ViewShareRootNode root, ViewShareTreeNode parent) {
        this.name = name;
        this.root = root;
        this.parent = parent;
    }

    protected abstract int getShareBaseIndex();
    protected abstract String getFileItemPath();

    protected void assureChildrenAreLoaded() {
        if (!hasSentQueryForChildren) {
            final Node node = root.getModel().getNode();
            root.getModel().getCore().invokeLater(new Runnable() {
                public void run() {
                    try {
                        if (node instanceof Friend) {
                            Friend f = (Friend) node;
                            if (f.isConnected()) {
                                f.getFriendConnection().send(new GetDirectoryListing(getShareBaseIndex(), getFileItemPath()));
                            } else {
                                OptionDialog.showInformationDialog(root.getModel().getUi().getMainWindow(),
                                        f.getNickname()+" has gone offline!");
                            }
                        } else {
                            ShareBase sb = root.getModel().getCore().getShareManager().getBaseByIndex(getShareBaseIndex());
                            String path = getFileItemPath();
                            if (path.startsWith("/")) path = path.substring(1);
                            if(T.t)T.info("Getting files for my share. ShareBase: "+getShareBaseIndex()+", path: "+path);
                            final String files[] = root.getModel().getCore().getFileManager().getFileDatabase().getDirectoryListing(sb, path);
                            for(String s : files) if(T.t)T.info("File: "+s);
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    root.getModel().getWin().directoryListingReceived(getShareBaseIndex(), getFileItemPath(), files);
                                }
                            });
                        }
                    } catch(Exception e) {
                        root.getModel().getCore().reportError(e, this);
                    }
                }
            });
            hasSentQueryForChildren = true;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (children.size() == 0) {
                        children.add(new ViewShareLoadingNode(root, ViewShareTreeNode.this));
                        root.getModel().nodeStructureChanged(ViewShareTreeNode.this);
                    }
                }
            });
        }
    }

    public int getChildCount() {
        assureChildrenAreLoaded();
        return children.size();
    }

    public TreeNode getChildAt(int childIndex) {
        return children.get(childIndex);
    }

    public ViewShareTreeNode getParent() {
        return parent;
    }

    public int getIndex(TreeNode node) {
        assureChildrenAreLoaded();
        return children.indexOf(node);
    }

    public boolean getAllowsChildren() {
        return true;
    }

    public boolean isLeaf() {
        if (name == null) return false;
        return !name.endsWith("/");
    }

    public Enumeration children() {
        assureChildrenAreLoaded();
        return EnumerationIteratorConverter.enumeration(children.iterator());
    }

    public String toString() {
        if (name != null && name.endsWith("/")) return name.substring(0, name.length()-1);
        return name;
    }

    public void pathUpdated(String path, String[] files) {
        if(T.t)T.info(this+": Path updated: "+path);

        if (path.trim().length() == 0) {
            if(T.t)T.info("It's our files - remove all our children and add the new ones.");
            children.clear();
            for(String s : files)  {
                if(T.t)T.trace("Adding: "+s);
                children.add(new ViewShareFileNode(s, root, this));
            }
            root.getModel().nodeStructureChanged(this);
        } else {
            String item = getFirstPathItem(path);
            if(T.t)T.trace("Looking for path item "+item);
            ViewShareFileNode node = getNodeByName(item);
            if (node == null) {
                if(T.t)T.warn("Ehh. Did not find item. Creating it.");
                node = new ViewShareFileNode(item, root, this);
                children.add(node);
                root.getModel().nodeStructureChanged(this);
            }
            node.pathUpdated(path.substring(item.length()), files);
        }
    }

    private ViewShareFileNode getNodeByName(String item) {
        for(ViewShareFileNode n : children) {
            if(T.t)T.trace("comparing: "+n.getName()+" - "+item);
            if (n.getName().equals(item)) return n;
        }
        return null;
    }

    protected String getFirstPathItem(String path) {
        if (path.startsWith("/")) path = path.substring(1);
        int i = path.indexOf('/');
        if (i == -1) {
            return path;
        } else {
            return path.substring(0, i+1);
        }
    }

    public String getName() {
        return name;
    }

    public Object getIdentifier() {
        return name + getFileItemPath();
    }
}
