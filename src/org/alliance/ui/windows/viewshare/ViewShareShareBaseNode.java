package org.alliance.ui.windows.viewshare;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-aug-10
 * Time: 18:16:51
 */
public class ViewShareShareBaseNode extends ViewShareTreeNode {

    private int shareBaseIndex;

    public ViewShareShareBaseNode(String name, ViewShareRootNode root, int shareBaseIndex) {
        super(name, root, root);
        this.shareBaseIndex = shareBaseIndex;
    }

    @Override
    protected int getShareBaseIndex() {
        return shareBaseIndex;
//        return root.getIndex(this);
    }

    @Override
    protected String getFileItemPath() {
        return "";
    }

    @Override
    public boolean isLeaf() {
        assureChildrenAreLoaded();
        return children.size() == 0;
    }
}
