package org.alliance.core.file;

import org.alliance.core.file.share.T;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-06
 * Time: 15:50:12
 * To change this template use File | Settings | File Templates.
 */
public class ChunkStorage {
    private final static byte MAGIC_ALIVE = 123, MAGIC_MARKED_FOR_DELETION=101;

    private RandomAccessFile raf;

    public ChunkStorage(String file) throws IOException {
        raf = new RandomAccessFile(file, "rw");
    }

    public int getSize() throws IOException {
        return (int)raf.length();
    }

    /**
     * @return The offset in our storage where this chunk is located
     */
    public synchronized int appendChunk(byte buf[]) throws IOException {
        if(T.t)T.trace("Appending "+buf.length+" bytes to chunk storage.");
        int pos = (int)raf.length();
        raf.seek(pos);
        raf.write(MAGIC_ALIVE);
        raf.writeInt(buf.length);
        raf.write(buf);
        return pos;
    }

    public synchronized InputStream getChunk(int off) throws IOException {
//        if(T.t)T.info("Reading from chunk storage at "+off);
        raf.seek(off);
        int magic = raf.readByte();

        if (magic == MAGIC_MARKED_FOR_DELETION) {
            return null;
        } else if (magic != MAGIC_ALIVE) {
            throw new IOException("Magic number incorrect in database file database. The database might be corrupt!");
        }

        int len = raf.readInt();
        byte buf[] = new byte[len];
        int r = raf.read(buf);
        if (r != len) if(T.t)T.error("Inconsistency in getChunk! Read less then expected");
        return new ByteArrayInputStream(buf);
    }

    public void flush() throws IOException {
        raf.getFD().sync();
    }

    public void markAsRemoved(int off) throws IOException {
        if(T.t)T.trace("Marking offset "+off+" as removed.");
        raf.seek(off);
        raf.writeByte(MAGIC_MARKED_FOR_DELETION);
    }

    public String getPercetMarkedForDeletion() throws IOException {
        long bytesDeleted = 0;
        long bytesUsed = 0;
        long off = 0;

        while(off < raf.length()) {
            raf.seek(off);
            int magic = raf.readByte();
            long size = raf.readInt();
            off += size+1+4;
            if (magic == MAGIC_MARKED_FOR_DELETION) {
                bytesDeleted += size;
                continue;
            } else if (magic != MAGIC_ALIVE) {
                throw new IOException("Magic number incorrect in database file database. The database might be corrupt!");
            }
            bytesUsed += size;
        }
        return (bytesDeleted*100) / (bytesUsed+bytesDeleted) + "%";
    }
}
