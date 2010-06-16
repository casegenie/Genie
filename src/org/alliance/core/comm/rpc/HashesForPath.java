package org.alliance.core.comm.rpc;

import com.stendahls.util.TextUtils;
import org.alliance.core.comm.T;
import org.alliance.core.file.hash.Hash;
import org.alliance.core.CoreSubsystem;
import org.alliance.core.LanguageResource;
import org.alliance.core.file.filedatabase.FileDescriptor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-21
 * Time: 16:34:22
 * To change this template use File | Settings | File Templates.
 */
public class HashesForPath extends CompressedRPC {

    private ArrayList<FileDescriptor> fdList = new ArrayList<FileDescriptor>();

    public HashesForPath() {
    }

    public HashesForPath(String path, String basePath, CoreSubsystem core) {
        if (core != null) {
            fdList = core.getShareManager().getFileDatabase().getHashesForPath(path, basePath);
        }
    }

    @Override
    public void serializeCompressed(DataOutputStream out) throws IOException {
        out.writeUTF(""); //Compatibility<1.0.9
        out.writeInt(fdList.size());
        for (FileDescriptor fd : fdList) {
            out.write(fd.getRootHash().array());
            out.writeUTF(fd.getSubPath());
        }
        fdList.clear();
    }

    @Override
    public void executeCompressed(DataInputStream in) throws IOException {
        String path = TextUtils.makeSurePathIsMultiplatform(in.readUTF());
        int numberOfFiles = in.readInt();

        for (int i = 0; i < numberOfFiles; i++) {
            Hash hash = new Hash();
            in.readFully(hash.array());
            FileDescriptor fd = new FileDescriptor();
            fd.setRootHash(hash);
            fd.setSubPath(in.readUTF());
            fdList.add(fd);
        }

        Collections.sort(fdList, new Comparator<FileDescriptor>() {

            @Override
            public int compare(FileDescriptor fd1, FileDescriptor fd2) {
                String s1 = fd1.getSubPath();
                String s2 = fd2.getSubPath();
                return s1.compareToIgnoreCase(s2);
            }
        });

        if (T.t) {
            T.info("Loaded " + numberOfFiles);
        }

        //@todo: this is soo messed up - client starts downloading without any state
        ArrayList<Integer> guid = new ArrayList<Integer>();
        guid.add(con.getRemoteUserGUID());

        String subPath = "";
        String downloadDir = "";

        for (FileDescriptor fd : fdList) {
            String commonPath = TextUtils.makeSurePathIsMultiplatform(fd.getSubPath());

            //Compatibility<1.0.9
            if (!path.isEmpty()) {
                if (subPath.isEmpty()) {
                    subPath = path;
                    while (!commonPath.startsWith(subPath)) {
                        subPath = subPath.substring(subPath.indexOf("/") + 1, subPath.length());
                    }
                    if (subPath.endsWith("/")) {
                        subPath = subPath.substring(0, subPath.length() - 1);
                    }
                    subPath = subPath.substring(0, subPath.lastIndexOf("/") + 1);
                }
                commonPath = commonPath.replace(subPath, "");
            }

            if (downloadDir != null && downloadDir.isEmpty()) {
                downloadDir = core.getFileManager().getDownloadStorage().getCustomDownloadDir(con.getRemoteUserGUID(), commonPath);
            }

            if (core.getFileManager().containsComplete(fd.getRootHash())) {
                core.getUICallback().statusMessage(LanguageResource.getLocalizedString(getClass(), "samefile", commonPath));
            } else if (core.getNetworkManager().getDownloadManager().getDownload(fd.getRootHash()) != null) {
                core.getUICallback().statusMessage(LanguageResource.getLocalizedString(getClass(), "downloadinprogress", commonPath));
            } else {
                core.getNetworkManager().getDownloadManager().queDownload(fd.getRootHash(), commonPath, guid, downloadDir);
            }
        }
        fdList.clear();
    }
}
