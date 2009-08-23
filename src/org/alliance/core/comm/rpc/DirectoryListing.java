package org.alliance.core.comm.rpc;

import org.alliance.core.comm.T;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-21
 * Time: 16:34:22
 * To change this template use File | Settings | File Templates.
 */
public class DirectoryListing extends CompressedRPC {
    private String files[];
    private int shareBaseIndex;
    private String path;

    public DirectoryListing() {
    }

    public DirectoryListing(int shareBaseIndex, String path, String files[]) {
        this.files = files;
        this.shareBaseIndex = shareBaseIndex;
        this.path = path;
    }

    public void executeCompressed(DataInputStream in) throws IOException {
        shareBaseIndex = in.readInt();
        path = in.readUTF();
        int nFiles = in.readInt();
        if(T.t)T.info("Decompressing "+nFiles+" files for share base "+shareBaseIndex+" and path "+path);

        files = new String[nFiles];
        for(int i=0;i<files.length;i++) files[i] = in.readUTF();

        if(T.t)T.info("Found the following files:");
        for(String s : files) {
            if(T.t)T.info("  "+s);
        }

        core.getUICallback().receivedDirectoryListing(con.getRemoteFriend(), shareBaseIndex, path, files);
    }

    public void serializeCompressed(DataOutputStream out) throws IOException {
        if(T.t)T.info("compressing directory listing and sending..");

        out.writeInt(shareBaseIndex);
        out.writeUTF(path);

        out.writeInt(files.length);
        for(String s : files) out.writeUTF(s);
    }
}
