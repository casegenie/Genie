package org.alliance.core.comm.networklayers.tcpnio;

import org.alliance.core.comm.Packet;
import org.alliance.core.comm.T;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-26
 * Time: 14:28:03
 * To change this template use File | Settings | File Templates.
 */
public class NIOPacket extends Packet {
    private ByteBuffer buffer;
    private boolean hasLengthBytes;

    public NIOPacket(ByteBuffer buffer, boolean hasLengthBytes) {
        this.buffer = buffer;
        if (hasLengthBytes) writeShort(0); //make space for packet length that will be sent later
        this.hasLengthBytes = hasLengthBytes;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void compact() {
        buffer.compact();
    }

    public void flip() {
        buffer.flip();
    }

    public int getAvailable() {
        return buffer.remaining();
    }

    public int getSize() {
        return buffer.limit();
    }

    public void setSize(int i) {
        buffer.limit(i);
    }

    public int getPos() {
        return buffer.position();
    }

    public void setPos(int pos) {
        buffer.position(pos);
    }

    public void skip(int n) {
        buffer.position(buffer.position()+n);
    }

    public byte readByte() {
        return buffer.get();
    }

    public void writeByte(byte b) {
        buffer.put(b);
    }

    public int readInt() {
        return buffer.getInt();
    }

    public void writeInt(int i) {
        buffer.putInt(i);
    }

    public void writeBoolean(boolean v) {
        buffer.put((byte)(v ? 1 : 0));
    }

    public boolean readBoolean() {
        return buffer.get() == 1;
    }

    public void readArray(byte[] arr) {
        buffer.get(arr);
    }

    public void readArray(byte[] arr, int off, int len) {
        buffer.get(arr, off, len);
    }

    public void writeArray(byte[] buf) {
        buffer.put(buf);
    }

    public void writeArray(byte[] buf, int off, int len) {
        buffer.put(buf, off, len);
    }

    public void writeLong(long l) {
        buffer.putLong(l);
    }

    public long readLong() {
        return buffer.getLong();
    }

    public void writeBuffer(ByteBuffer buf) {
        buffer.put(buf);
    }

    public void prepareForSend() throws IOException {
        int len = buffer.position()-2; //two bytes to store length
        if (len >= 0xffff) throw new IOException("Packet size overflow! Packet size: "+len);
        buffer.flip();
        mark();
        writeShort(len);
        reset();
        if(T.netTrace)T.trace("Patched packet length: "+len);
    }

    public byte[] asArray() {
        int len = buffer.position();
        if (hasLengthBytes) {
            buffer.position(2);
            len -= 2;
        } else {
            buffer.position(0);
        }
        byte[] buf = new byte[len];
        for(int i=0;i<len;i++) buf[i] = buffer.get();
        buffer.position(len);
        return buf;
    }

    public void mark() {
        buffer.mark();
    }

    public void reset() {
        buffer.reset();
    }
}
