package org.alliance.ui.windows.search;

import com.stendahls.nif.ui.mdi.MDIWindow;
import com.stendahls.ui.util.TableSorter;
import com.stendahls.util.TextUtils;
import org.alliance.core.comm.SearchHit;
import org.alliance.core.file.filedatabase.FileType;
import org.alliance.core.file.hash.Hash;
import org.alliance.ui.UISubsystem;
import org.alliance.ui.windows.AllianceMDIWindow;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-05
 * Time: 13:21:55
 * To change this template use File | Settings | File Templates.
 */
public class SearchMDIWindowOld extends AllianceMDIWindow {
	private HashMap<Hash, HashHit> hashHits = new HashMap<Hash, HashHit>();
    private ArrayList<HashHit> rows = new ArrayList<HashHit>(5000);

    private int totalHits;

    private JTable table;
    private SearchMDIWindowOld.SearchTableModel model;
    private TableSorter sorter;
    private JComboBox type;
    private JRadioButton newfiles, keywords;
    private JPopupMenu popup;

    private JTextField search;

    public SearchMDIWindowOld(final UISubsystem ui) throws Exception {
        super(ui.getMainWindow().getMDIManager(), "search", ui);

        search = (JTextField)xui.getComponent("search1");
        table = (JTable)xui.getComponent("table");
        type = (JComboBox)xui.getComponent("type");
        popup = (JPopupMenu)xui.getComponent("popup");

        newfiles = (JRadioButton)xui.getComponent("newfiles");
        keywords = (JRadioButton)xui.getComponent("keywords");
        keywords.setSelected(true);

        for(FileType v : FileType.values()) type.addItem(v.description());

        model = new SearchMDIWindowOld.SearchTableModel();
        sorter = new TableSorter(model);
        table.setModel(sorter);
        sorter.setTableHeader(table.getTableHeader());
        table.setAutoCreateColumnsFromModel(false);
        setupDefaultSortOrder();

        table.getColumnModel().getColumn(0).setCellRenderer(new SearchMDIWindowOld.StringCellRenderer());
        table.getColumnModel().getColumn(1).setCellRenderer(new SearchMDIWindowOld.StringCellRenderer());
        table.getColumnModel().getColumn(2).setCellRenderer(new SearchMDIWindowOld.BytesizeCellRenderer());
        table.getColumnModel().getColumn(3).setCellRenderer(new SearchMDIWindowOld.DaysOldCellRenderer());
        table.getColumnModel().getColumn(4).setCellRenderer(new SearchMDIWindowOld.StringCellRenderer());

        table.getColumnModel().getColumn(0).setPreferredWidth(225);
        table.getColumnModel().getColumn(1).setPreferredWidth(225);
        table.getColumnModel().getColumn(2).setPreferredWidth(50);
        table.getColumnModel().getColumn(3).setPreferredWidth(50);
        table.getColumnModel().getColumn(4).setPreferredWidth(50);

        table.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
                if (e.getClickCount() >= 2) {
                    EVENT_download(null);
                }
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    boolean b = false;
                    for(int r : table.getSelectedRows()) {
                        if (r == row) {
                            b = true;
                            break;
                        }
                    }
                    if (!b) table.getSelectionModel().setSelectionInterval(row,row);
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        setTitle("File search");
        postInit();
    }

    private void setupDefaultSortOrder() {
        sorter.cancelSorting();
        sorter.setSortingStatus(4, TableSorter.DESCENDING);
        sorter.setSortingStatus(3, TableSorter.ASCENDING);
    }

    public void EVENT_download(ActionEvent e) {
        int selection[] = table.getSelectedRows();

        if (selection != null && selection.length > 0) for(int i : selection) {
            boolean changeWindow = false;
            if (i >= 0 && i < rows.size()) {
                final HashHit h = rows.get(sorter.modelIndex(i));
                if (ui.getCore().getFileManager().containsComplete(h.hash)) {
                    ui.getCore().getUICallback().statusMessage("You already have the file "+h.filename+"!");
                } else if (ui.getCore().getNetworkManager().getDownloadManager().getDownload(h.hash) != null) {
                    ui.getCore().getUICallback().statusMessage("You are already downloading "+h.filename+"!");
                } else {
                    ui.getCore().invokeLater(new Runnable() {
                        public void run() {
                            try {
                                ui.getCore().getNetworkManager().getDownloadManager().queDownload(h.hash, h.filename, h.getUserGuids());
                            } catch(IOException e1) {
                                ui.handleErrorInEventLoop(e1);
                            }
                        }
                    });
                    changeWindow = true;
                }
            }
            if (changeWindow) ui.getMainWindow().getMDIManager().selectWindow(ui.getMainWindow().getDownloadsWindow());
        }
    }

    public void searchHits(int sourceGuid, int hops, java.util.List<SearchHit> hits) {
        for(SearchHit sh : hits) {
            Hash hash = sh.getRoot();
            if (ui.getCore().getFileManager().getFileDatabase().contains(hash)) continue;
            String filename = sh.getPath();
            long size = sh.getSize();
            HashHit hh = hashHits.get(hash);
            if (hh == null) {
                hh = new HashHit(hash, ui.getCore());
                hashHits.put(hash, hh);
                rows.add(hh);
            }
            hh.addHit(hops, size, filename, sourceGuid, sh.getHashedDaysAgo());
            totalHits++;
        }
        ((JLabel)xui.getComponent("status")).setText(totalHits +" hits");

        model.fireTableDataChanged();
    }

    public void windowSelected() {
        super.windowSelected();
        search.requestFocus();
    }

    public void EVENT_search1(ActionEvent e) throws IOException {
        search(search.getText());
    }

    public void EVENT_search2(ActionEvent e) throws IOException {
        search(search.getText());
    }

    private void search(String text) throws IOException {
        search(text, FileType.values()[type.getSelectedIndex()]);
        if (keywords.isSelected()) search.setText("");
    }

    public void searchForNewFilesOfType(FileType ft) throws IOException {
        setupDefaultSortOrder();
        search("", ft);
    }

    public void search(String text, final FileType ft) throws IOException {
        if (newfiles.isSelected()) {
            text = "";
            setupDefaultSortOrder();
        }
        final String t = text;

        String s;
        if (t.trim().length() == 0)
            s = "Searching for all files of type "+ft.description();
        else
            s = "Searching for "+t +" in "+ft.description()+"...";
        ((JLabel)xui.getComponent("status")).setText(s);
        totalHits=0;
        hashHits.clear();
        rows.clear();
        model.fireTableDataChanged();
        ui.getCore().invokeLater(new Runnable() {
            public void run() {
                try {
                    ui.getCore().getFriendManager().getNetMan().sendSearch(t, ft);
                } catch(IOException e) {
                    ui.handleErrorInEventLoop(e);
                }
            }
        });
    }

    public String getIdentifier() {
        return "Search";
    }

    public void save() throws Exception {}
    public void revert() throws Exception {}
    public void serialize(ObjectOutputStream out) throws IOException {}
    public MDIWindow deserialize(ObjectInputStream in) throws IOException { return null; }

    public class SearchTableModel extends AbstractTableModel  {

		public int getRowCount() {
            return rows.size();
        }

        public int getColumnCount() {
            return 5;
        }

        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 2) return Long.class;
            if (columnIndex == 3) return Integer.class;
            if (columnIndex == 4) return Integer.class;
            return super.getColumnClass(columnIndex);
        }

        public String getColumnName(int columnIndex) {
            switch(columnIndex) {
                case 0:
                    return "File";
                case 1:
                    return "Folder";
                case 2:
                    return "Size";
                case 3:
                    return "Days ago";
                case 4:
                    return "Sources";
                default:
                    return "undefined";
            }
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            switch(columnIndex) {
                case 0:
                    return rows.get(rowIndex).filename;
                case 1:
                    return rows.get(rowIndex).folder;
                case 2:
                    return rows.get(rowIndex).size;
                case 3:
                    return rows.get(rowIndex).daysAgo;
                case 4:
                    return rows.get(rowIndex).hits;
                default:
                    return "undefined";
            }
        }
    }


    public class StringCellRenderer extends DefaultTableCellRenderer {

		public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int rowIndex, int vColIndex) {
            super.getTableCellRendererComponent(table, value,  isSelected,  hasFocus,  rowIndex, vColIndex);
            HashHit h = rows.get(sorter.modelIndex(rowIndex));
            setToolTipText("<html>"+h.path+"<br>"+h.getListOfUsers()+"<br>"+h.hash.getRepresentation());
            return this;
        }
        public void validate() {}
        public void revalidate() {}
        protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {}
        public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}
    }

    public class BytesizeCellRenderer extends DefaultTableCellRenderer {

		public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int rowIndex, int vColIndex) {
            super.getTableCellRendererComponent(table, value,  isSelected,  hasFocus,  rowIndex, vColIndex);
            setText(TextUtils.formatByteSize((Long)value));
            setToolTipText(String.valueOf(value));
            return this;
        }
        public void validate() {}
        public void revalidate() {}
        protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {}
        public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}
    }

    public class DaysOldCellRenderer extends DefaultTableCellRenderer {

		public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int rowIndex, int vColIndex) {
            super.getTableCellRendererComponent(table, value,  isSelected,  hasFocus,  rowIndex, vColIndex);
            int val = (Integer)value;
            if (val == 255)
                setText("Old");
            else
                setText(String.valueOf(val));
            return this;
        }
        public void validate() {}
        public void revalidate() {}
        protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {}
        public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}
    }

    public void EVENT_keywords(ActionEvent e) {
        search.setEnabled(true);
    }

    public void EVENT_newfiles(ActionEvent e) {
        search.setEnabled(false);
    }
}
