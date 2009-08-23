package org.alliance.core.comm.rpc;

import org.alliance.core.comm.Packet;
import org.alliance.core.comm.RPC;
import org.alliance.core.comm.SearchHit;
import org.alliance.core.comm.T;
import org.alliance.core.file.filedatabase.FileDescriptor;
import org.alliance.core.file.hash.Hash;

import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * Sent when a friend needs information about the correct ip/port of a common friend of ours.
 *
 * Received when we need info about a friend that we haven't got the correct ip/port to.
 *
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-28
 * Time: 18:42:48
 */
public class SearchHits extends RPC {
    public static final int MAX_SEARCH_HITS = 500;

    private ArrayList<SearchHit> hits = new ArrayList<SearchHit>();

    public SearchHits() {
    }

    public void addHit(FileDescriptor fd) {
        hits.add(new SearchHit(fd));
    }

    public void execute(Packet in) throws IOException {
        for(;;) {
            long size = in.readLong();
            if (size == -1) break;
            Hash h = new Hash();
            in.readArray(h.array());
            String path = in.readUTF();
            int daysAgo = in.readUnsignedByte();
            hits.add(new SearchHit(h, path, size, daysAgo));
        }
        if(T.t)T.info("Received "+hits.size()+" hits.");
        manager.getCore().getUICallback().searchHits(fromGuid, hops, hits);
    }

    public Packet serializeTo(Packet p) {
        int n = hits.size();
        if (n > MAX_SEARCH_HITS) n = MAX_SEARCH_HITS;
//        p.writeShort(n);
        int i=0;
        for(SearchHit sh : hits) {
            if (p.getAvailable() < sh.getPath().length()*2+20) {
                if(T.t)T.info("Can't send more search hits - buffer would overflow.");
                break;
            }
            p.writeLong(sh.getSize());
            p.writeArray(sh.getRoot().array());
            p.writeUTF(sh.getPath());
            p.writeByte((byte)sh.getHashedDaysAgo());
            i++;
            if (i>n) break;
        }
        p.writeLong(-1);
        return p;
    }

    public int getNHits() {
        return hits.size();
    }
}
