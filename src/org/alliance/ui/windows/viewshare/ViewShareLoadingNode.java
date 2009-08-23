package org.alliance.ui.windows.viewshare;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-aug-10
 * Time: 18:16:51
 */

public class ViewShareLoadingNode extends ViewShareFileNode {
    public ViewShareLoadingNode(ViewShareRootNode root, ViewShareTreeNode parent) {
        super("Loading...", root, parent);
    }

    protected int getShareBaseIndex() {
        return getParent().getShareBaseIndex();
    }

    protected String getFileItemPath() {
        return getParent().getFileItemPath()+getName();
    }

    public boolean isLeaf() {
        return true;
    }
}
