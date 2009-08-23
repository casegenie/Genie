package org.alliance.core.comm.rpc;

import org.alliance.core.comm.Packet;
import org.alliance.core.comm.RPC;
import org.alliance.core.comm.T;
import org.alliance.core.file.share.ShareBase;

import java.io.IOException;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-21
 * Time: 16:34:22
 * To change this template use File | Settings | File Templates.
 */
public class ShareBaseList extends RPC {
    public ShareBaseList() {
    }

    public void execute(Packet data) throws IOException {
        int n = data.readInt();
        String shareBaseName[] = new String[n];
        for(int i=0;i<n;i++) {
            shareBaseName[i] = data.readUTF();
            if(T.t) T.trace("Found share base name: "+shareBaseName[i]);
        }
        
        core.getUICallback().receivedShareBaseList(con.getRemoteFriend(), shareBaseName);
    }

    public Packet serializeTo(Packet p) {
        Collection<ShareBase> c = manager.getCore().getShareManager().shareBases();
        p.writeInt(c.size());
        for(ShareBase sb : c) p.writeUTF(sb.getName());
        return p;
    }
}
