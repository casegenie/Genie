package org.alliance.core.file.filedatabase;

import com.stendahls.nif.util.SimpleTimer;
import com.stendahls.util.TextUtils;
import org.alliance.core.CoreSubsystem;
import org.alliance.core.comm.SearchHit;
import org.alliance.core.file.hash.Hash;
import org.alliance.core.file.share.ShareBase;
import org.alliance.core.file.FileManager;
import org.alliance.launchers.console.Console;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-06
 * Time: 15:49:43
 * To change this template use File | Settings | File Templates.
 */
public class FileDatabase {

    private CoreSubsystem core;
    private long shareSize = 0;
    private int numberOfShares = 0;
    private boolean priority = false;

    public FileDatabase(CoreSubsystem core) throws IOException {
        this.core = core;
        updateCacheCounters(true, true);
    }

    public void addEntry(FileDescriptor fd) {
        FileIndex fileIndex = new FileIndex(fd.getBasePath(), mergePathParts(fd.getBasePath(), fd.getSubpath(), null));
        byte fileType = FileType.getByFileName(fileIndex.getFilename()).id();
        if (core.getDbCore().getDbShares().addEntry(fileIndex.getBasePath(), fileIndex.getSubPath(), fileIndex.getFilename(), fileType, fd.getSize(), fd.getRootHash().array(), fd.getModifiedAt())) {
            int blockNumber = 0;
            for (Hash h : fd.getHashList()) {
                core.getDbCore().getDbHashes().addEntry(fd.getRootHash().array(), h.array(), blockNumber);
                blockNumber++;
            }
        } else {
            core.getDbCore().getDbDuplicates().addEntry(mergePathParts(fd.getBasePath(), fd.getSubpath(), null), fd.getRootHash().array(), fd.getModifiedAt());
        }
    }

    public void removeEntry(byte[] rootHash) {
        core.getDbCore().getDbShares().deleteEntryByRootHash(rootHash);
    }

    private void removeObsoleteShare(String basePath, int removedFiles) {
        ResultSet results = core.getDbCore().getDbShares().getEntriesByBasePath(basePath, 1024, 0);
        try {
            boolean recurse = false;
            while (results.next()) {
                removeEntry(results.getBytes("root_hash"));
                removedFiles++;
                core.getUICallback().statusMessage("Removed " + removedFiles + " files from removed share - " + basePath);
                recurse = true;
            }
            if (recurse) {
                removeObsoleteShare(basePath, removedFiles);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private int removeObsoleteFiles(String subPath, int offset, int scannedFiles) {
        ResultSet results = core.getDbCore().getDbShares().getEntriesBySubPath(subPath, 1024, offset);
        try {
            boolean recurse = false;
            while (results.next()) {
                core.getUICallback().statusMessage("Searching for removed files: (" + scannedFiles * 100 / numberOfShares + "%)");
                scannedFiles++;
                if (!(new File(mergePathParts(results.getString("base_path"), results.getString("sub_path"), results.getString("filename"))).exists())) {
                    removeEntry(results.getBytes("root_hash"));
                }
                recurse = true;
            }
            if (recurse) {
                scannedFiles = removeObsoleteFiles(subPath, offset + 1024, scannedFiles);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return scannedFiles;
    }

    public void removeObsoleteEntries(ArrayList<ShareBase> shareBase) {
        // long time = System.currentTimeMillis();
        ResultSet results = core.getDbCore().getDbShares().getBasePaths();
        try {
            //Clean by removed sharabases
            ArrayList<String> shares = new ArrayList<String>();
            for (ShareBase sharebase : core.getFileManager().getShareManager().shareBases()) {
                shares.add(sharebase.getPath());
            }
            while (results.next()) {
                if (!shares.contains(results.getString("base_path"))) {
                    removeObsoleteShare(results.getString("base_path"), 0);
                }
            }
            //Clean by removed files
            int scannedFiles = 0;
            results = core.getDbCore().getDbShares().getSubPaths();
            while (results.next()) {
                scannedFiles = removeObsoleteFiles(results.getString("sub_path"), 0, scannedFiles);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        //System.out.println("removeObsoleteShares - " + (System.currentTimeMillis() - time));
    }

    public FileDescriptor getFd(Hash rootHash) throws IOException {
        // long time = System.currentTimeMillis();
        ResultSet result = core.getDbCore().getDbShares().getEntryByRootHash(rootHash.array());
        try {
            while (result.next()) {
                String basePath = result.getString("base_path");
                String subPath = mergePathParts(null, result.getString("sub_path"), result.getString("filename"));
                long size = result.getLong("size");
                long modifiedAt = result.getLong("modified");

                result = core.getDbCore().getDbHashes().getEntryByRootHash(rootHash.array());
                ArrayList<Hash> hashArray = new ArrayList<Hash>();
                while (result.next()) {
                    hashArray.add(new Hash(result.getBytes("hash")));
                }
                Hash[] hashList = hashArray.toArray(new Hash[hashArray.size()]);
                // System.out.println("getFD - " + (System.currentTimeMillis() - time));
                FileDescriptor fd = new FileDescriptor(basePath, subPath, size, rootHash, hashList, modifiedAt);
                return fd;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
        return null;
    }

    public byte[] getRootHash(String basePath, String path) throws IOException {
        FileIndex fileIndex = new FileIndex(basePath, path);
        ResultSet result = core.getDbCore().getDbShares().getEntryByFullPath(fileIndex.getBasePath(), fileIndex.getSubPath(), fileIndex.getFilename());
        try {
            while (result.next()) {
                return result.getBytes("root_hash");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
        return null;
    }

    public HashMap<Hash, String> getRootHashWithPath(String path, String basePath) {
        HashMap<Hash, String> hashPath = new HashMap<Hash, String>();
        try {
            FileIndex fileIndex = new FileIndex(basePath, mergePathParts(basePath, path, null));
            if (!path.endsWith("/")) {
                ResultSet results = core.getDbCore().getDbShares().getEntryByFullPath(fileIndex.getBasePath(), fileIndex.getSubPath(), fileIndex.getFilename());
                while (results.next()) {
                    hashPath.put(new Hash(results.getBytes("root_hash")), mergePathParts(null, null, results.getString("filename")));
                }
            } else {
                ResultSet results = core.getDbCore().getDbShares().getEntriesByBasePathAndSubPath(fileIndex.getBasePath(), fileIndex.getSubPath(), true, 512);
                path = path.substring(0, path.length() - 1);
                path = path.substring(0, path.lastIndexOf("/") + 1);
                path = basePath + "/" + path;
                while (results.next()) {
                    hashPath.put(new Hash(results.getBytes("root_hash")), mergePathParts(results.getString("base_path"), results.getString("sub_path"), results.getString("filename")).replace(path, ""));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return hashPath;
    }

    public HashMap<Hash, Long> getRootHashWithSize(String basePath, String path) {
        HashMap<Hash, Long> hashSize = new HashMap<Hash, Long>();
        try {
            FileIndex fileIndex = new FileIndex(basePath, mergePathParts(basePath, path, null));
            if (!path.endsWith("/")) {
                ResultSet results = core.getDbCore().getDbShares().getEntryByFullPath(fileIndex.getBasePath(), fileIndex.getSubPath(), fileIndex.getFilename());
                while (results.next()) {
                    hashSize.put(new Hash(results.getBytes("root_hash")), results.getLong("size"));
                }
            } else {
                ResultSet results = core.getDbCore().getDbShares().getEntriesByBasePathAndSubPath(fileIndex.getBasePath(), fileIndex.getSubPath(), true, 512);
                while (results.next()) {
                    hashSize.put(new Hash(results.getBytes("root_hash")), results.getLong("size"));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return hashSize;
    }

    public synchronized ArrayList<SearchHit> getSearchHits(String query, byte type, int limit) {
        //long time = System.currentTimeMillis();
        priority = true;
        ResultSet results = core.getDbCore().getDbShares().getEntriesBySearchQuery(query, type, limit);
        ArrayList<SearchHit> hitList = new ArrayList<SearchHit>();
        try {
            Hash root;
            String path;
            long size;
            int hashedDaysAgo;
            String basepath;
            while (results.next()) {
                root = new Hash(results.getBytes("root_hash"));
                basepath = results.getString("base_path");
                path = mergePathParts(null, results.getString("sub_path"), results.getString("filename"));
                size = results.getLong("size");
                hashedDaysAgo = (int) ((System.currentTimeMillis() - results.getLong("modified")) / 1000 / 60 / 60 / 24);
                if (hashedDaysAgo > 255) {
                    hashedDaysAgo = 255;
                }
                hitList.add(new SearchHit(root, path, size, basepath, hashedDaysAgo));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        //System.out.println("getsearchhit - " + (System.currentTimeMillis() - time));
        priority = false;
        return hitList;
    }

    public HashMap<String, String> getDuplicates(int limit) {
        ResultSet results = core.getDbCore().getDbDuplicates().getAllEntries(limit);
        HashMap<String, String> duplicates = new HashMap<String, String>();
        try {
            while (results.next()) {
                duplicates.put(results.getString("path"), mergePathParts(results.getString("base_path"), results.getString("sub_path"), results.getString("filename")));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return duplicates;
    }

    public synchronized void flush() throws IOException {
        core.getUICallback().statusMessage("<html><b><font color=blue>Saving settings...</font></b></html>");
        SimpleTimer st = new SimpleTimer();
        core.getUICallback().statusMessage("Saved settings in " + st.getTime());
    }

    public boolean contains(String basePath, String path, boolean checkDuplicates) {
        FileIndex fileIndex = new FileIndex(basePath, path);
        ResultSet result = core.getDbCore().getDbShares().contains(fileIndex.getBasePath(), fileIndex.getSubPath(), fileIndex.getFilename());
        try {
            while (result.next()) {
                if (result.getBoolean("contains")) {
                    return true;
                } else if (checkDuplicates) {
                    result = core.getDbCore().getDbDuplicates().getEntryByPath(mergePathParts(fileIndex.getBasePath(), fileIndex.getSubPath(), fileIndex.getFilename()));
                    return result.next();
                } else {
                    return false;
                }
            }
            return false;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public boolean contains(Hash rootHash) {
        ResultSet result = core.getDbCore().getDbShares().getEntryByRootHash(rootHash.array());
        try {
            return result.next();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public synchronized void updateCacheCounters(boolean updateNumber, boolean updateSize) {
        try {
            if (updateNumber) {
                ResultSet results = core.getDbCore().getDbShares().getNumberOfShares();
                while (results.next()) {
                    numberOfShares = results.getInt(1);
                }
            }
            if (updateSize) {
                ResultSet results = core.getDbCore().getDbShares().getTotalSizeOfFiles();
                while (results.next()) {
                    shareSize = results.getLong(1);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public long getTotalSize() {
        return shareSize;
    }

    public int getNumberOfShares() {
        return numberOfShares;
    }

    public synchronized TreeMap<String, Long> getDirectoryListing(ShareBase base, String subPath) {
        priority = true;
        //long time = System.currentTimeMillis();

        ArrayList<String> entries = new ArrayList<String>();
        ResultSet results = core.getDbCore().getDbShares().getEntriesByBasePathAndSubPath(base.getPath(), subPath, false, 8196);
        try {
            while (results.next()) {
                entries.add(mergePathParts(results.getString("base_path"), results.getString("sub_path"), results.getString("filename")));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        TreeMap<String, Long> fileSize = new TreeMap<String, Long>(new Comparator<String>() {

            @Override
            public int compare(String s1, String s2) {
                if (s1 == null || s2 == null) {
                    return 0;
                }
                if (s1.equalsIgnoreCase(s2)) {
                    return s1.compareTo(s2);
                }
                if (s1.endsWith("/") && !s2.endsWith("/")) {
                    return -1;
                }
                if (!s1.endsWith("/") && s2.endsWith("/")) {
                    return 1;
                }
                return s1.compareToIgnoreCase(s2);
            }
        });

        File dirPath = new File(mergePathParts(base.getPath(), subPath, null));
        File[] list = dirPath.listFiles();
        int politeCounter = 0;
        if (list != null) {
            for (File f : list) {
                if (f.isDirectory() && !f.getName().contains(FileManager.INCOMPLETE_FOLDER_NAME) && f.listFiles().length != 0) {
                    fileSize.put(f.getName() + "/", 0L);
                } else if (entries.contains(TextUtils.makeSurePathIsMultiplatform(f.getPath()))) {
                    fileSize.put(f.getName(), f.length());
                }
                politeCounter++;
                if (politeCounter == 500) {
                    politeCounter = 0;
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }
        
        //System.out.println("directory listing - " + (System.currentTimeMillis() - time));
        priority = false;
        return fileSize;
    }

    private String mergePathParts(String basePath, String subPath, String filename) {
        StringBuilder path = new StringBuilder();
        if (basePath != null) {
            path.append(basePath);
            path.append("/");
        }
        if (subPath != null) {
            path.append(subPath);
        }
        if (filename != null) {
            path.append(filename);
        }
        return path.toString();
    }

    public boolean isPriority() {
        return priority;
    }

    public void printStats(Console.Printer printer) throws IOException {
        printer.println("Filedatabse stats:");
    }

    private class FileIndex {

        private String basePath;
        private String subPath;
        private String filename;

        public FileIndex(String basePath, String path) {
            this.basePath = TextUtils.makeSurePathIsMultiplatform(basePath);
            subPath = TextUtils.makeSurePathIsMultiplatform(path).substring(this.basePath.length() + 1);
            filename = subPath.substring(subPath.lastIndexOf("/") + 1, subPath.length());
            subPath = subPath.substring(0, subPath.length() - filename.length());
        }

        public String getBasePath() {
            return basePath;
        }

        public String getFilename() {
            return filename;
        }

        public String getSubPath() {
            return subPath;
        }
    }
}
