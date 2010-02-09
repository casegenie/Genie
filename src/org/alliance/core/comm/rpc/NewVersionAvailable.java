package org.alliance.core.comm.rpc;

import org.alliance.core.comm.Packet;
import org.alliance.core.comm.RPC;
import org.alliance.core.comm.T;
import org.alliance.core.file.hash.Hash;
import org.alliance.core.node.Friend;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-21
 * Time: 16:36:41
 * To change this template use File | Settings | File Templates.
 */
public class NewVersionAvailable extends RPC {

	private int buildNumber;
    private Hash binaryHash;
    private Hash signatureHash;

    public NewVersionAvailable(Hash hash) {
        this.binaryHash = hash;
    }

    public NewVersionAvailable() {
    }

    @Override
    public void execute(Packet p) throws IOException {
    	buildNumber = p.readInt();
        binaryHash = new Hash();
        p.readArray(binaryHash.array());
        signatureHash = new Hash();
        p.readArray(signatureHash.array());
        
        if (T.t) {
            T.info("Received new version info. Queuing for download.");
        }
        
        Friend remote = core.getFriendManager().getFriend(fromGuid);
        
        core.getFileManager().getAutomaticUpgrade().beginDownloadAndUpgrade(buildNumber, remote, binaryHash, signatureHash);
    }

    @Override
    public Packet serializeTo(Packet p) {
    	p.writeInt(buildNumber);
        p.writeArray(binaryHash.array());
        p.writeArray(signatureHash.array());
        return p;
    }
}
