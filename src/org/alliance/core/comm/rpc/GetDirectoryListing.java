package org.alliance.core.comm.rpc;

import org.alliance.core.comm.Packet;
import org.alliance.core.comm.RPC;
import org.alliance.core.comm.T;
import org.alliance.core.file.share.ShareBase;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-21
 * Time: 16:34:22
 * To change this template use File | Settings | File Templates.
 */
public class GetDirectoryListing extends RPC {
    private String path;
    private int shareBaseIndex;

    public GetDirectoryListing() {
    }

    public GetDirectoryListing(int shareBaseIndex, String path) {
        if(T.t)T.info("About to send GetDirectoryListing. ShareBaseIndex: "+shareBaseIndex+", path: "+path);
        this.shareBaseIndex = shareBaseIndex;
        this.path = path;
    }

    public void execute(Packet data) throws IOException {
        shareBaseIndex = data.readInt();
        String path = data.readUTF();
        if (path.startsWith("/")) path = path.substring(1);
        ShareBase sb = manager.getCore().getShareManager().getBaseByIndex(shareBaseIndex);
        String files[] = manager.getCore().getFileManager().getFileDatabase().getDirectoryListing(sb, path);
        send(new DirectoryListing(shareBaseIndex, path, files));
    }

    public Packet serializeTo(Packet p) {
        p.writeInt(shareBaseIndex);
        p.writeUTF(path);
        return p;
    }
}
