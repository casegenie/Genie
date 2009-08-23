package org.alliance.core.comm.rpc;

import org.alliance.core.T;
import org.alliance.core.comm.Packet;
import org.alliance.core.comm.RPC;
import org.alliance.core.file.blockstorage.BlockFile;
import org.alliance.core.file.blockstorage.BlockMask;
import org.alliance.core.file.hash.Hash;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-21
 * Time: 16:34:22
 * To change this template use File | Settings | File Templates.
 */
public class GetBlockMask extends RPC {
    private Hash root;

    public GetBlockMask() {
    }

    public GetBlockMask(Hash root) {
        this();
        this.root = root;
    }

    public void execute(Packet data) throws IOException {
        root = new Hash();
        data.readArray(root.array());

        core.logNetworkEvent("GetBlockMast for "+core.getFileManager().getFd(root)+" from "+con.getRemoteFriend());

        if (manager.getCore().getFileManager().containsComplete(root) && manager.getCore().getFileManager().getFd(root) != null) {
            if(T.t)T.info("Found complete file for root "+root);
            send(new BlockMaskResult(root, true,
                    BlockFile.getNumberOfBlockForSize(manager.getCore().getFileManager().getFd(root).getSize()))); //will automatically route to correct person
        } else {
            BlockMask bm = manager.getCore().getFileManager().getBlockMask(root);
            if (bm != null) {
                if(T.t)T.info("Found incomplete file for root "+root);
                send(new BlockMaskResult(root, bm));
            } else {
                if(T.t)T.info("Root "+root+" not found.");
            }
        }

        core.getNetworkManager().getDownloadManager().interestedInHash(con.getRemoteFriend(), root);
    }

    public Packet serializeTo(Packet p) {
        p.writeArray(root.array());
        return p;
    }
}
