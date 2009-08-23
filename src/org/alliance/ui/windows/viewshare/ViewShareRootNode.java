package org.alliance.ui.windows.viewshare;

import com.stendahls.nif.util.EnumerationIteratorConverter;
import com.stendahls.ui.T;

import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-aug-10
 * Time: 18:15:45
 */
public class ViewShareRootNode extends ViewShareTreeNode {
    private ArrayList<ViewShareShareBaseNode> shareBases = new ArrayList<ViewShareShareBaseNode>();
    private ViewShareTreeModel model;

    public ViewShareRootNode() {
        super("root", null, null);
    }

    void fill(String[] shareBaseNames) {
        shareBases.clear();
        for(int i=0;i<shareBaseNames.length;i++) {
            if (!shareBaseNames[i].equals("cache")) shareBases.add(new ViewShareShareBaseNode(shareBaseNames[i], this, i));
        }
    }

    public ViewShareShareBaseNode getChildAt(int childIndex) {
        return shareBases.get(childIndex);
    }

    protected int getShareBaseIndex() {
        throw new RuntimeException("This may not be called!");
    }

    protected String getFileItemPath() {
        if(T.t)T.warn("This should not be called");
        return null;
    }

    public int getChildCount() {
        return shareBases.size();
    }

    public ViewShareTreeNode getParent() {
        return null;
    }

    public int getIndex(TreeNode node) {
        if (!(node instanceof ViewShareShareBaseNode)) return -1;
        return shareBases.indexOf((ViewShareShareBaseNode)node);
    }

    public boolean getAllowsChildren() {
        return true;
    }

    public boolean isLeaf() {
        return false;
    }

    public Enumeration children() {
        return EnumerationIteratorConverter.enumeration(shareBases.iterator());
    }

    public ViewShareTreeModel getModel() {
        return model;
    }

    public void setModel(ViewShareTreeModel viewShareTreeModel) {
        model = viewShareTreeModel;
    }

    public ViewShareShareBaseNode getByShareBase(int shareBaseIndex) {
        for(ViewShareShareBaseNode s : shareBases) {
            if (s.getShareBaseIndex() == shareBaseIndex) return s;
        }
        return null;
    }
}
