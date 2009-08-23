package org.alliance.core.comm.rpc;

import org.alliance.core.comm.Packet;
import org.alliance.core.comm.RPC;
import org.alliance.core.comm.T;
import org.alliance.core.file.filedatabase.FileDescriptor;

import java.io.IOException;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-21
 * Time: 16:34:22
 * To change this template use File | Settings | File Templates.
 */
public class GetHashesForPath extends RPC {
    private String path;
    private int shareBaseIndex;

    public GetHashesForPath() {
    }

    public GetHashesForPath(int shareBaseIndex, String path) {
        this.shareBaseIndex = shareBaseIndex;
        this.path = path;
    }

    public void execute(Packet data) throws IOException {
        shareBaseIndex = data.readInt();
        path = data.readUTF();

        String basePath = core.getShareManager().getBaseByIndex(shareBaseIndex).getPath();
        path = basePath += '/'+path;

        Collection<FileDescriptor> c = core.getFileManager().getFileDatabase().getFDsByPath(path);
        if(T.t)T.info("Found "+c.size()+" hashes for path "+path);
        send(new HashesForPath(path, c));
    }

    public Packet serializeTo(Packet p) {
        p.writeInt(shareBaseIndex);
        p.writeUTF(path);
        return p;
    }
}

