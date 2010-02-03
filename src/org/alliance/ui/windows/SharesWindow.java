package org.alliance.ui.windows;

import com.stendahls.XUI.XUIDialog;
import com.stendahls.util.TextUtils;
import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.tree.MutableTreeNode;
import org.alliance.ui.UISubsystem;

import javax.swing.JTree;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

public class SharesWindow extends XUIDialog {

    private DefaultTreeModel sharesTreeModel;
    private JTree sharesTree;

    public SharesWindow(UISubsystem ui) throws Exception {
        super(ui.getMainWindow());

        init(ui.getRl(), ui.getRl().getResourceStream("xui/shareswindow.xui.xml"));
        sharesTree = (JTree) xui.getComponent("sharesTree");
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        sharesTree.setRootVisible(false);
        sharesTreeModel = new DefaultTreeModel(root);
        sharesTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        //Insert roots
        File[] roots = File.listRoots();
        for (int i = 0; i < roots.length; i++) {
            DefaultMutableTreeNode rootPath = new DefaultMutableTreeNode(roots[i].toString());
            rootPath.insert(new DefaultMutableTreeNode("Loading..."), 0);
            sharesTreeModel.insertNodeInto(rootPath, root, i);
        }

        sharesTree.addTreeExpansionListener(new TreeExpansionListener() {

            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                DefaultMutableTreeNode selectedDirNode = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();

                if (selectedDirNode.getFirstChild().toString().equals("Loading...")) {
                    //Get path from selection
                    Object[] pathParts = event.getPath().getPath();
                    String path = pathParts[1].toString();
                    for (int i = 2; i < pathParts.length; i++) {
                        path += pathParts[i].toString();
                        path += "/";
                    }
                    path = TextUtils.makeSurePathIsMultiplatform(path);

                    File selectedDir = new File(path);
                    if (selectedDir.listFiles() != null) {
                        int i = 1;
                        for (File file : selectedDir.listFiles()) {
                            if (file.isDirectory() && !file.isHidden()) {
                                DefaultMutableTreeNode newDirNode = new DefaultMutableTreeNode(file.getName());
                                newDirNode.insert(new DefaultMutableTreeNode("Loading..."), 0);
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

        sharesTree.setModel(sharesTreeModel);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        display();
    }

    public void EVENT_ok(ActionEvent a) throws Exception {
        dispose();
    }
}
