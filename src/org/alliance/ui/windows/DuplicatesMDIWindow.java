package org.alliance.ui.windows;

import org.alliance.ui.UISubsystem;
import org.alliance.core.file.filedatabase.FileDescriptor;
import org.alliance.core.file.hash.Hash;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.util.ArrayList;
import java.util.TreeSet;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.File;
import java.awt.event.ActionEvent;
import java.awt.*;

import com.stendahls.nif.ui.mdi.MDIWindow;
import com.stendahls.nif.ui.OptionDialog;
import com.stendahls.util.TextUtils;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-05
 * Time: 13:21:55
 * To change this template use File | Settings | File Templates.
 */
public class DuplicatesMDIWindow extends AllianceMDIWindow {
	private DuplicatesMDIWindow.TableModel model;
    private JTable table;
    private ArrayList<Dup> dups = new ArrayList<Dup>();

    public DuplicatesMDIWindow(UISubsystem ui) throws Exception {
        super(ui.getMainWindow().getMDIManager(), "duplicates", ui);

        table = (JTable)xui.getComponent("table");
        table.setModel(model = new DuplicatesMDIWindow.TableModel());
        table.setAutoCreateColumnsFromModel(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(0).setCellRenderer(new MyCellRenderer());
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setCellRenderer(new MyCellRenderer());
        table.getColumnModel().getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setColumnSelectionAllowed(true);

        setTitle("Duplicates in my share");

        TreeSet<String> ts = new TreeSet<String>(ui.getCore().getFileManager().getFileDatabase().getDuplicates());
        for(String s : ts) {
            Hash h = ui.getCore().getFileManager().getFileDatabase().getHashForDuplicate(s);
            if (h == null) {
                dups.add(new Dup(s, "<lost>"));
            } else {
                FileDescriptor fd = ui.getCore().getFileManager().getFd(h);
                if (fd != null) dups.add(new Dup(fd.getFullPath(), s));
            }
        }

        ((JLabel)xui.getComponent("status")).setText("Number of duplicates: "+dups.size());
        postInit();
    }

    public void EVENT_delete(ActionEvent e) throws Exception {
        if (table.getSelectedColumnCount() <= 0 && table.getSelectedRowCount() <= 0) return;
        if (table.getSelectedColumnCount() > 1) {
            OptionDialog.showErrorDialog(ui.getMainWindow(), "Please select files on only one column - not both.");
            return;
        }

        ArrayList<String> filesThatNeedHashing = new ArrayList<String>();
        ArrayList<String> al = new ArrayList<String>();
        for(int i : table.getSelectedRows()) {
            if (table.getSelectedColumn() == 0) {
                al.add(dups.get(i).inShare);
                filesThatNeedHashing.add(dups.get(i).duplicate);
            } else if (table.getSelectedColumn() == 1) {
                al.add(dups.get(i).duplicate);
            }
        }

        if (OptionDialog.showQuestionDialog(ui.getMainWindow(), "Are you sure you want to delete "+al.size()+" file(s)?")) {
            int deleted=0;

            for(String s : al) {
                if (!new File(s).delete()) {
                    /*OptionDialog.showErrorDialog(ui.getMainWindow(), "Could not delete "+s+".");
                    return;*/
                } else {
                    deleted++;
                }
            }

            //duplicates that no longer have their corresponding file in share - make sure they are hashed now.
            for (String f : filesThatNeedHashing) {
                ui.getCore().getShareManager().getShareScanner().signalFileCreated(f);
            }

            OptionDialog.showInformationDialog(ui.getMainWindow(), deleted+"/"+al.size()+" files deleted. Note that it might take a while before the duplicate list is updated.");
            revert();
        }
    }

    public String getIdentifier() {
        return "duplicates";
    }

    public void save() throws Exception {}
    public void revert() throws Exception {
        manager.recreateWindow(this, new DuplicatesMDIWindow(ui));
    }
    public void serialize(ObjectOutputStream out) throws IOException {}
    public MDIWindow deserialize(ObjectInputStream in) throws IOException { return null; }

    private class TableModel extends AbstractTableModel {

		public int getRowCount() {
            return dups.size();
        }

        public int getColumnCount() {
            return 2;
        }

        public String getColumnName(int columnIndex) {
            switch(columnIndex) {
                case 0:
                    return "In share";
                case 1:
                    return "Duplicate";
                default:
                    return "undefined";
            }
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            switch(columnIndex) {
                case 0:
                    return dups.get(rowIndex).inShare;
                case 1:
                    return dups.get(rowIndex).duplicate;
                default:
                    return "undefined";
            }
        }
    }

    private class Dup {
        public Dup(String inShare, String duplicate) {
            this.inShare = TextUtils.makeSurePathIsMultiplatform(inShare);
            this.duplicate = TextUtils.makeSurePathIsMultiplatform(duplicate);
        }
        String inShare, duplicate;
    }

    private class MyCellRenderer extends DefaultTableCellRenderer {

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String s = String.valueOf(value);
            int i = s.lastIndexOf('/');
            if (i != -1) s = s.substring(i+1) + " ("+s.substring(0,i)+")";
            setText(s);
            return this;
        }
    }
}
