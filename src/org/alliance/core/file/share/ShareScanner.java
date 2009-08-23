package org.alliance.core.file.share;

import com.stendahls.nif.util.SimpleTimer;
import com.stendahls.util.TextUtils;
import org.alliance.core.AwayManager;
import org.alliance.core.CoreSubsystem;
import static org.alliance.core.CoreSubsystem.GB;
import org.alliance.core.file.FileManager;
import org.alliance.core.file.filedatabase.FileDatabase;
import org.alliance.core.file.filedatabase.FileDescriptor;
import org.alliance.launchers.OSInfo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-06
 * Time: 15:48:44
 * To change this template use File | Settings | File Templates.
 */
public class ShareScanner extends Thread {
    private boolean alive = true;
    private ShareManager manager;
    private long bytesScanned;
    private CoreSubsystem core;
    private boolean shouldBeFastScan = false;
    private boolean scanInProgress = false;
    private boolean scannerHasBeenStarted = false;

    private ArrayList<String> filesQueuedForHashing = new ArrayList<String>();
    private long lastFullScanCompletedAt;
    private long lastFlushCompletedAt;

    public ShareScanner(CoreSubsystem core, ShareManager manager) {
        this.core = core;
        this.manager = manager;
        setName("ShareScanner -- "+core.getSettings().getMy().getNickname());
        setPriority(MIN_PRIORITY);
    }

    public void run() {
        try { Thread.sleep(60*1000); } catch (InterruptedException e) {} //wait a while before starting first scan
        core.getAwayManager().addListener(new AwayManager.AwayStatusListener() {
            public void awayStatusChanged(boolean away) throws IOException {
                if (away && System.currentTimeMillis()-lastFlushCompletedAt > 1000*60*20 && !scanInProgress) {
                    if(T.t)T.trace("Flushing database because user is away and it was a while since we did it.");
                    manager.getFileDatabase().flush();
                    core.saveState();
                }
            }
        });

        scannerHasBeenStarted = true;
        if (core.getSettings().getInternal().getRescansharewhenalliancestarts() != null && core.getSettings().getInternal().getRescansharewhenalliancestarts() != 1) waitForNextScan();
        while(alive) {
            scanInProgress = true;
            filesScannedCounter = 0;
            if (filesQueuedForHashing.size() > 0) {
                ArrayList<String> al = new ArrayList<String>(filesQueuedForHashing);
                filesQueuedForHashing.clear();
                for (String file : al) {
                    try {
                        if (manager.getFileDatabase().getFDsByPath(file).size() > 0) {
                            if(T.t)T.trace("File already is hashed: "+file);
                            continue;
                        }
                        File f = new File(file);
                        if (!f.isDirectory() && f.canRead()) hash(file);
                    } catch (FileNotFoundException e) {
                        if(T.t)T.error("Problem while hashing file "+file+": "+e+", trying again later");
                        queFileForHashing(file, true);
                    } catch (IOException e) {
                        if(T.t)T.error("Could not hash file "+file+": "+e);
                    }
                }
            }

            if (System.currentTimeMillis() - lastFullScanCompletedAt > getShareManagerCycle() || shouldBeFastScan) {
                manager.getFileDatabase().cleanupDuplicates();

                cleanup();

                ArrayList<ShareBase> al = new ArrayList<ShareBase>(manager.shareBases());
                for(ShareBase base : al) {
                    if (!alive) break;
                    try {
                        scanPath(base);
                    } catch(Exception e) {
                        if(T.t)T.error("Could not scan "+base+": "+e);
                    }
                }

                manager.getCore().getUICallback().statusMessage("Share scan complete.");
                lastFullScanCompletedAt = System.currentTimeMillis();
            }

            try {
                //flush fairly often when user is away - the UI locks when you flush so we want to avoid doing that while the user is by the computer
                if (((core.getAwayManager().isAway() || !core.getUICallback().isUIVisible()) && System.currentTimeMillis()-lastFlushCompletedAt > 1000*60*20)
                        || System.currentTimeMillis()-lastFlushCompletedAt > 1000*60*60*2) { //if user insists on constantly beeing by the computer with alliance visible then forcefully flush every second hour - note that a flush will be made as soon as the user is away because of the awaystatuslistener
                    manager.getFileDatabase().flush();
                    core.saveState();
                    lastFlushCompletedAt = System.currentTimeMillis();
                }
            } catch(IOException e) {
                core.reportError(e, this);
            }

            shouldBeFastScan = false;
            scanInProgress = false;
            if (!alive) break;
            waitForNextScan();
        }
    }

    private void waitForNextScan() {
        try {
            if(T.t)T.info("Wating for next share scan.");
            if (filesQueuedForHashing.size() > 0) {
                if(T.t)T.info("Files are in hash que so don't wait for too long");
                Thread.sleep(1000*5);
            } else  {
                Thread.sleep(getShareManagerCycle());
            }
        } catch(Exception e) {}
    }

    private long getShareManagerCycle() {
        if (OSInfo.isWindows())
            return 1000*60*manager.getSettings().getInternal().getSharemanagercyclewithfilesystemeventsactive();
        else
            return 1000*60*manager.getSettings().getInternal().getSharemanagercycle();
    }

    private void cleanup() {
        if(T.t)T.info("Cleaning up index...");
        FileDatabase fd = manager.getFileDatabase();

        int n = fd.getNumberOfFiles();
        for(int i = 0; i < n; i++) {
            if(!alive) return;
            try {
                // If file is missing the descriptor will automatically be removed from the index
                fd.getFd(i, false);

                int sleepEveryXFiles = shouldBeFastScan ? 500 : 100;
                if(i % sleepEveryXFiles == 0) {
                    manager.getCore().getUICallback().statusMessage("Checking share for removed files ("+(i*100/n)+"%)...");
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {}
                }
            } catch (IOException e) {
                if(T.t)T.warn("Unable to retrieve file descriptor: "+e);
            }
        }
    }

    private void scanPath(ShareBase base) throws IOException {
        if(T.t)T.info("Scanning "+base.getPath()+"...");
        scanPathRecursive(base.getPath(),base,1);
    }

    private int filesScannedCounter=0;
    private void scanPathRecursive(String dir, ShareBase base, int level) throws IOException {
        if (!alive) return;

        if (shouldSkip(dir)) return;

        File top = new File(dir);

        File files[] = top.listFiles();
        if (files != null) for(int i=0;i<files.length;i++) {
            File file = files[i];
            file = file.getCanonicalFile();
            if (!shouldBeFastScan && (filesScannedCounter % 100) == 0) try {Thread.sleep(100);} catch (InterruptedException e) {}
            filesScannedCounter++;
            if (file.isDirectory()) {
                if(T.t)T.trace("Scanning "+file.getPath()+"...");
                manager.getCore().getUICallback().statusMessage("Scanning "+file.getPath()+"...");
                scanPathRecursive(file.getPath(),base,level+1);
            } else {
                try {
                    if (!manager.getFileDatabase().contains(file.toString())) {
                        hash(base, file);
                    }
                } catch(IOException e) {
                    if(T.t)T.warn("Could not hash file "+file+": "+e);
                }
            }
        }
    }

    private void hash(String file) throws IOException {
        if (manager.getShareBaseByFile(file) == null) {
            if(T.t)T.info("Share base not found for "+file+" - cant hash");
            return;
        }
        File f = new File(file);
        if (!f.exists()) {
            if(T.t)T.warn("File "+file+" does not exist - cant hash!");
            return;
        }
        hash(manager.getShareBaseByFile(file), f);
    }

    private void hash(ShareBase base, File file) throws IOException {
        if (file.isHidden() || file.length() == 0) {
            if(T.t)T.debug("Skipping hidden or empty file "+file);
            return;
        }
        if (manager.getFileDatabase().isDuplicate(file.getCanonicalPath())) return;

        SimpleTimer st = new SimpleTimer();
        FileDescriptor fd;
        try {
            fd = new FileDescriptor(base.getPath(), file, shouldBeFastScan ? 0 : core.getSettings().getInternal().getHashspeedinmbpersecond(), manager.getCore().getUICallback());
        } catch(FileDescriptor.FileModifiedWhileHashingException e) {
            manager.getCore().getUICallback().statusMessage("File modified while hashing: "+file);
            queFileForHashing(file.toString(), true);
            return;
        }
        manager.getCore().getUICallback().statusMessage("Hashed "+fd.getFilename()+" in "+st.getTime()+" ("+ TextUtils.formatByteSize((long)(fd.getSize()/(st.getTimeInMs()/1000.)))+"/s)");
        manager.getFileDatabase().add(fd);

        bytesScanned += fd.getSize();
        if (bytesScanned > manager.getCore().getSettings().getInternal().getPolitehashingintervalingigabytes()*GB) {
            bytesScanned = 0;
            try {
                if(T.t)T.info("Polite scanning in progress. Sleeping for "+manager.getCore().getSettings().getInternal().getPolitehashingwaittimeinminutes()+" minutes for harddrive to cool down.");
                Thread.sleep(manager.getCore().getSettings().getInternal().getPolitehashingwaittimeinminutes()*60*1000);
            } catch(InterruptedException e) {}
        }
    }

    private void queFileForHashing(String file, boolean lowPriority) {
        try {
            if (manager.getFileDatabase().getFDsByPath(file).size() > 0) {
                if(T.t)T.trace("File already is hashed: "+file);
                return;
            }
        } catch(Exception e) {
            if(T.t)T.warn("Problem while cheking if file already is hashed: "+e);
        }
        if (filesQueuedForHashing.contains(file)) return;
        if (!lowPriority) {
            filesQueuedForHashing.add(0, file);
            interrupt();
        } else {
            filesQueuedForHashing.add(file);
        }
    }

    private boolean shouldSkip(String dir) {
        String s = TextUtils.makeSurePathIsMultiplatform(dir);
        if (s.endsWith("/")) s = s.substring(0,s.length()-1);
        return s.endsWith(FileManager.INCOMPLETE_FOLDER_NAME);
    }

    public void kill() {
        alive = false;
        interrupt();
    }

    public void startScan(boolean fastScan) {
        shouldBeFastScan = fastScan;
        interrupt();
    }

    public ShareManager getManager() {
        return manager;
    }

    public void signalFileRenamed(final String oldFile, final String newFile) {
        if (!scannerHasBeenStarted || newFile.indexOf(FileManager.INCOMPLETE_FOLDER_NAME) != -1) return;
        core.invokeLater(new Runnable() {
            public void run() {
                try {
                    manager.getFileDatabase().getFDsByPath(oldFile);
                    queFileForHashing(newFile, false);
                } catch (IOException e) {
                    core.reportError(e, this);
                }
            }
        });
    }

    public void signalFileDeleted(final String file) {
        if (!scannerHasBeenStarted|| file.indexOf(FileManager.INCOMPLETE_FOLDER_NAME) != -1) return;
        core.invokeLater(new Runnable() {
            public void run() {
                try {
                    manager.getFileDatabase().removeFromDuplicates(file); //in case the file exists in duplicates it will be removed
                    manager.getFileDatabase().getFDsByPath(file); //will notice that the file is no longer avail and remove it from index
                    manager.getCore().getUICallback().statusMessage("Removed file "+file+" from share.");
                } catch (IOException e) {
                    core.reportError(e, this);
                }
            }
        });
    }

    public void signalFileCreated(final String file) {
        if (!scannerHasBeenStarted || file.indexOf(FileManager.INCOMPLETE_FOLDER_NAME) != -1) return;
        queFileForHashing(file, false);
    }
}
