package org.alliance.core.file;

import org.alliance.core.CoreSubsystem;
import org.alliance.core.Manager;
import org.alliance.core.comm.AutomaticUpgrade;
import org.alliance.core.file.blockstorage.BlockMask;
import org.alliance.core.file.blockstorage.BlockStorage;
import org.alliance.core.file.blockstorage.CacheStorage;
import org.alliance.core.file.blockstorage.DownloadStorage;
import org.alliance.core.file.filedatabase.FileDatabase;
import org.alliance.core.file.filedatabase.FileDescriptor;
import org.alliance.core.file.filedatabase.FileType;
import org.alliance.core.file.hash.Hash;
import org.alliance.core.file.share.ShareManager;
import org.alliance.core.settings.Settings;

import java.io.IOException;

/**
 * Keeps track of all files used in Alliance. Contains the ShareManager, DownloadStorage and AutomaticUpgrade. Should
 * contain FileDatabase but it's actually contained in the ShareManager. Also contains the CacheStorage (that's hardly
 * used right now).
 * <P>
 * This is the place to check if we have a certain root hash (complete or incomplete), to get FileDescriptors and to
 * do file searches.
 * <p>
 * @see DownloadStorage, ShareManager, AutomaticUpgrade, FileDatabase
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-22
 * Time: 19:02:07
 * To change this template use File | Settings | File Templates.
 */
public class FileManager extends Manager {
    private CacheStorage cache;
    private DownloadStorage downloads;
    private ShareManager shareManager;
    private CoreSubsystem core;
    private Settings settings;
    private AutomaticUpgrade automaticUpgrade;
    public static final String INCOMPLETE_FOLDER_NAME = "_incomplete_";

    public FileManager(CoreSubsystem core, Settings settings) throws IOException {
        this.core = core;
        this.settings = settings;
    }

    public void init() throws IOException {
        cache = new CacheStorage(settings.getInternal().getCachefolder()+"/"+FileManager.INCOMPLETE_FOLDER_NAME, settings.getInternal().getCachefolder(), core);
        downloads = new DownloadStorage(settings.getInternal().getDownloadfolder()+"/"+FileManager.INCOMPLETE_FOLDER_NAME, settings.getInternal().getDownloadfolder(), core);
        automaticUpgrade = new AutomaticUpgrade(core, cache);
        shareManager = new ShareManager(core, settings);
    }

    public ShareManager getShareManager() {
        return shareManager;
    }

    public void shutdown() throws IOException {
        shareManager.shutdown();
        cache.shutdown();
        downloads.shutdown();
    }

    public boolean contains(Hash root) {
        if (cache.contains(root)) return true;
        if (downloads.contains(root)) return true;
        if (shareManager.getFileDatabase().contains(root)) return true;
        return false;
    }

    public FileDescriptor[] search(String query, int maxHits, FileType ft) throws IOException {
        return shareManager.getFileDatabase().search(query, maxHits, ft);
    }

    public long getTotalBytesShared() {
        return shareManager.getFileDatabase().getTotalSize();
    }

    public boolean containsComplete(Hash root) {
        return shareManager.getFileDatabase().contains(root);
    }

    public BlockMask getBlockMask(Hash root) throws IOException {
        BlockMask bm = downloads.getBlockMaskFor(root);
        if (bm != null) return bm;
        return cache.getBlockMaskFor(root);
    }

    public FileDescriptor getFd(Hash root) throws IOException {
        FileDescriptor fd = shareManager.getFileDatabase().getFd(root);
        if (fd != null) return fd;
        fd = downloads.getFD(root);
        if (fd != null) return fd;
        fd = cache.getFD(root);
        return fd;
    }

    public BlockStorage getBlockStorageFor(Hash root) {
        if (downloads.contains(root)) return downloads;
        if (cache.contains(root)) return cache;
        return null;
    }
    
    public CacheStorage getCache() {
        return cache;
    }

    public DownloadStorage getDownloadStorage() {
        return downloads;
    }

    public boolean hasBlock(Hash rootHash, int blockNumber) throws IOException {
        if (shareManager.getFileDatabase().contains(rootHash)) return true;
        if (downloads.contains(rootHash) && downloads.getBlockMaskFor(rootHash).get(blockNumber)) return true;
        return cache.contains(rootHash) && cache.getBlockMaskFor(rootHash).get(blockNumber);
    }

    public FileDatabase getFileDatabase() {
        return shareManager.getFileDatabase();
    }

    public boolean isRecentlyDownloadedOrComplete(Hash rootHash) {
        if (containsComplete(rootHash)) return true;
        if (downloads.isRecentlyDownloaded(rootHash)) return true;
        return cache.isRecentlyDownloaded(rootHash);
    }

    public AutomaticUpgrade getAutomaticUpgrade() {
        return automaticUpgrade;
    }
}
