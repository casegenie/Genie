package org.alliance.core.node;

import com.stendahls.util.HumanReadableEncoder;
import org.alliance.core.CoreSubsystem;
import org.alliance.core.T;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.Random;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-mar-16
 * Time: 14:20:47
 */
public class Invitation implements Serializable {

    private int invitationPassKey;
    private String completeInvitationString;
    private long createdAt;
    private Integer destinationGuid;
    private int middlemanGuid;

    public Invitation() {
    }

    public Invitation(CoreSubsystem core, Integer destinationGuid, Integer middlemanGuid) throws Exception {
        this.destinationGuid = destinationGuid;
        this.middlemanGuid = middlemanGuid == null ? 0 : middlemanGuid;

        String myhost;
        myhost = core.getFriendManager().getMe().getExternalIp(core);

        if (T.t) {
            T.trace("Creating invitation for host: " + myhost);
        }
        byte[] ip = InetAddress.getByName(myhost).getAddress();
        if (T.t) {
            T.trace("Got: " + ip[0] + "." + ip[1] + "." + ip[2] + "." + ip[3]);
        }

        ByteArrayOutputStream o = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(o);

        //ip
        for (byte b : ip) {
            out.write((byte)(b ^ 0xFF));
        }

        //port
        out.writeShort(core.getSettings().getServer().getPort());

        //passkey
        invitationPassKey = new Random().nextInt();
        out.writeInt(invitationPassKey);
        if (T.t) {
            T.trace("passkey: " + invitationPassKey);
        }

        out.flush();
        completeInvitationString = HumanReadableEncoder.toBase64SHumanReadableString(o.toByteArray()).trim();

        createdAt = System.currentTimeMillis();
        if (T.t) {
            T.info("Created invitation. String: " + completeInvitationString);
        }
    }

    public int getInvitationPassKey() {
        return invitationPassKey;
    }

    public void setInvitationPassKey(int invitationPassKey) {
        this.invitationPassKey = invitationPassKey;
    }

    public String getCompleteInvitationString() {
        return completeInvitationString;
    }

    public void setCompleteInvitationString(String completeInvitationString) {
        this.completeInvitationString = completeInvitationString;
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
