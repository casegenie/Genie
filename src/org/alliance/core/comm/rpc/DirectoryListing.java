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

    @Override
    public void executeCompressed(DataInputStream in) throws IOException {
        shareBaseIndex = in.readInt();
        path = in.readUTF();
        int nFiles = in.readInt();
        if (T.t) {
            T.info("Decompressing " + nFiles + " files for share base " + shareBaseIndex + " and path " + path);
        }

        files = new String[nFiles];
        for (int i = 0; i < files.length; i++) {
            files[i] = in.readUTF();
        }

        if (T.t) {
            T.info("Found the following files:");
        }
        for (String s : files) {
            if (T.t) {
                T.info("  " + s);
            }
        }

        core.getUICallback().receivedDirectoryListing(con.getRemoteFriend(), shareBaseIndex, path, files);
    }

    @Override
    public void serializeCompressed(DataOutputStream out) throws IOException {
        if (T.t) {
            T.info("compressing directory listing and sending..");
        }
        boolean positive = false;
        //Bastvera
        //(Group names for specific user)
        String usergroupname = con.getRemoteGroupName();

        //Bastvera (Group names for specific shared folder)
        String sbgroupname = manager.getCore().getShareManager().getBaseByIndex(shareBaseIndex).getSBGroupName();
        if (sbgroupname.equalsIgnoreCase("public")) {
            positive = true;
        } else {

            //Split Multi sbgroupname names to single cell in array
            String[] dividedu = usergroupname.split(",");
            String[] dividedsb = sbgroupname.split(",");

            //Compare every usergroupname with every sbgroupname break if positive match

            for (String testsb : dividedsb) {
                for (String testu : dividedu) {
                    if (testsb.equalsIgnoreCase(testu)) {
                        positive = true;
                        break;
                    }
                }
                if (positive == true) {
                    break;
                }
            }
        }
        //Send matched directory listing and always send public folders
        if (positive == true) {
            out.writeInt(shareBaseIndex);
            out.writeUTF(path);
            out.writeInt(files.length);
            for (String s : files) {
                out.writeUTF(s);
            }
        } else {
            out.writeInt(shareBaseIndex);
            out.writeUTF(path);
            out.writeInt(0); //Do not list hidden folders
        }
    }
}
