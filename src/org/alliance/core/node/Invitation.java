package org.alliance.core.node;

import com.stendahls.util.HumanReadableEncoder;
import org.alliance.core.CoreSubsystem;
import org.alliance.core.T;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.Enumeration;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-mar-16
 * Time: 14:20:47
 */
public class Invitation implements Serializable {
	private int invitationPassKey;
    private String completeInvitaitonString;
    private long createdAt;
    private Integer destinationGuid;
    private int middlemanGuid;

    public Invitation() {
    }

    public Invitation(CoreSubsystem core, Integer destinationGuid, Integer middlemanGuid) throws Exception {
        this.destinationGuid = destinationGuid;
        this.middlemanGuid = middlemanGuid == null ? 0 : middlemanGuid;

        String myhost;
        if (core.getSettings().getServer().getLansupport() != null && core.getSettings().getServer().getLansupport() == 1) {
            myhost = getLocalIPNumber();
        } else {
            myhost = core.getFriendManager().getMe().getExternalIp(core);
        }

        if(T.t)T.trace("Creating invitation for host: "+myhost);
        byte[] ip = InetAddress.getByName(myhost).getAddress();
        if(T.t)T.trace("Got: "+ip[0]+"."+ip[1]+"."+ip[2]+"."+ip[3]);

        ByteArrayOutputStream o = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(o);

        //ip
        for(byte b : ip) out.write(b);

        //port
        out.writeShort(core.getSettings().getServer().getPort());

        //passkey
        invitationPassKey = new Random().nextInt();
        out.writeInt(invitationPassKey);
        if(T.t)T.trace("passkey: "+invitationPassKey);

        out.flush();
        completeInvitaitonString = HumanReadableEncoder.toBase64SHumanReadableString(o.toByteArray()).trim();

        createdAt = System.currentTimeMillis();
        if(T.t)T.info("Created invitation. String: "+completeInvitaitonString);
    }

    private String getLocalIPNumber() throws SocketException, UnknownHostException {
        //get the local ip number of machine
        Enumeration netInterfaces= NetworkInterface.getNetworkInterfaces();
        while(netInterfaces.hasMoreElements()){
            NetworkInterface ni=(NetworkInterface)netInterfaces.nextElement();
            if (ni.getInetAddresses().hasMoreElements()) {
                InetAddress ip=(InetAddress)ni.getInetAddresses().nextElement();
                if(!ip.isSiteLocalAddress() && !ip.isLoopbackAddress() && ip.getHostAddress().indexOf(":")==-1){
                    return ip.getHostAddress();
                }
            }
        }
        return InetAddress.getLocalHost().getHostAddress();
    }

    public int getInvitationPassKey() {
        return invitationPassKey;
    }

    public void setInvitationPassKey(int invitationPassKey) {
        this.invitationPassKey = invitationPassKey;
    }

    public String getCompleteInvitaitonString() {
        return completeInvitaitonString;
    }

    public void setCompleteInvitaitonString(String completeInvitaitonString) {
        this.completeInvitaitonString = completeInvitaitonString;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public Integer getDestinationGuid() {
        return destinationGuid;
    }

    public int getMiddlemanGuid() {
        return middlemanGuid;
    }

    public boolean isForwardedInvitation() {
        return middlemanGuid != 0;
    }
}
