package org.alliance.core.node;

import org.alliance.core.CoreSubsystem;
import org.alliance.core.T;
import org.alliance.Version;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-03
 * Time: 14:38:28
 * To change this template use File | Settings | File Templates.
 */
public class MyNode extends Node {
    private static final String WHATSMYIPURL = "http://www.alliancep2p.com/myip";

    private String externalIp;
    private CoreSubsystem core;

    public MyNode(String nickname, int guid, CoreSubsystem core) {
        super(nickname, guid);
        this.core = core;
    }

    public boolean isConnected() {
        return true;
    }

    public int getNumberOfInvitedFriends() {
        return core.getSettings().getMy().getInvitations();
    }

    public boolean hasNotBeenOnlineForLongTime() {
        return false;
    }

    public long getLastSeenOnlineAt() {
        return System.currentTimeMillis();
    }

    public String getExternalIp(CoreSubsystem core) throws IOException {
        autodetectExternalIp(core);
        return getExternalIp();
    }

    public String getExternalIp() {
        return externalIp;
    }

    public void setExternalIp(String externalIp) {
        this.externalIp = externalIp;
    }

    private boolean alreadyTriedAutodetect = false;
    public void autodetectExternalIp(CoreSubsystem core) throws IOException {
        if (externalIp == null && !alreadyTriedAutodetect) {
            try {
                URLConnection c = new URL(WHATSMYIPURL).openConnection();
                InputStream in = c.getInputStream();
                StringBuffer result = new StringBuffer();
                BufferedReader r = new BufferedReader(new InputStreamReader(in));
                String line;
                while((line = r.readLine()) != null) result.append(line);

                line = result.toString();
                externalIp = line.trim();

                if(T.t)T.info("Detected external ip: "+externalIp);
            } catch(Exception e) {
                if(T.t)T.error("Could not detected external ip: "+e);
                throw new IOException("Could not detect your external IP: "+e);
            } finally{
                alreadyTriedAutodetect = true;
            }
        }
    }


    public int getAllianceBuildNumber() {
        return Version.BUILD_NUMBER;
    }

    public int getNumberOfFilesShared() {
        return core.getFileManager().getFileDatabase().getNumberOfFiles();
    }

    public double getHighestOutgoingCPS() {
        return core.getSettings().getInternal().getRecordoutspeed();
    }

    public double getHighestIncomingCPS() {
        return core.getSettings().getInternal().getRecordinspeed();
    }

    public long getTotalBytesSent() {
        return core.getNetworkManager().getBandwidthOut().getTotalBytes();
    }

    public long getTotalBytesReceived() {
        return core.getNetworkManager().getBandwidthIn().getTotalBytes();
    }
}
