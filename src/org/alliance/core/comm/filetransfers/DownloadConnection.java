package org.alliance.core.comm.filetransfers;

import org.alliance.core.comm.NetworkManager;
import org.alliance.core.comm.Packet;
import org.alliance.core.file.hash.Hash;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-26
 * Time: 21:36:37
 * To change this template use File | Settings | File Templates.
 */
public class DownloadConnection extends TransferConnection {

    public static final int CONNECTION_ID = 2;
    private Download download;
    private ArrayList<DataConsumer> consumerQueue = new ArrayList<DataConsumer>();
    private boolean inHandshake = true;

    public DownloadConnection(NetworkManager netMan, Direction direction, int userGUID, Download download) {
        super(netMan, direction, userGUID);
        this.download = download;
    }

    @Override
    public Hash getRoot() {
        return download.getRoot();
    }

    @Override
    public void init() throws IOException {
        super.init();
        download.connectionEstablished(this);
        sendHash();
        if (download.getFd() == null && !download.isDownloadingFd()) {
            if (T.t) {
                T.info("Attempting to download FileDescriptor");
            }
            sendGetFD();
        }
        inHandshake = false;
        if (download.getFd() != null) {
            startDownloadingBlock();
        }
    }

    @Override
    public void bytesReceived(int n) {
        super.bytesReceived(n);
        download.getBandwidth().update(n);
    }

    public void sendGetFD() throws IOException {
        sendCommand(Command.GET_FD);
        switchMode(Mode.RAW);
        consumerQueue.add(new FileDescriptorConsumer(download, this));
        if (T.t) {
            T.ass(consumerQueue.size() == 1, "Something's strange with queue " + consumerQueue.size());
        }
    }

    public void sendGracefulClose() throws IOException {
        sendCommand(Command.GRACEFULCLOSE);
        close();
    }

    @Override
    public void close() throws IOException {
        super.close();
        download.removeConnection(this);
    }

    @Override
    public void received(ByteBuffer buf) throws IOException {
        if (mode == Mode.PACKET) {
            super.received(buf);
        } else {
            while (buf.remaining() > 0) {
                //this might seem weird, but when a consumer is done, it will compact the buffer,
                // remove itself from the queue and return. This way we continue with the next consumer.
                consumerQueue.get(0).consume(buf);
//                if(T.t)T.trace("Bytes left in buf: "+buf.remaining());
            }
        }
    }

    @Override
    public void packetReceived(Packet p) throws IOException {
        if (T.t) {
            T.ass(false, "eh. We're not receiving any packets here");
        }
    }

    private void sendHash() throws IOException {
        if (T.t) {
            T.debug("Sending command HASH");
        }
        Packet p = netMan.createPacketForSend();
        p.writeByte(Command.HASH.value());
        p.writeArray(download.getRoot().array());
        send(p);
    }

    private void sendOneOrMoreGetBlocks(int blockNumber) throws IOException {
        if (T.t) {
            T.debug("Sending command GETBLOCK " + blockNumber);
        }
        switchMode(Mode.PACKET);

        while (consumerQueue.size() < core.getSettings().getInternal().getNumberofblockstopipeline()) {
            if (blockNumber == -1) {
                if (T.t) {
                    T.info("Trying to queue another get block");
                }
                blockNumber = download.selectBestBlockForDownload(getRemoteFriend());
                if (blockNumber == -1) {
                    if (T.t) {
                        T.info("Couldn't find block to queue");
                    }
                    break;
                }
            }
            if (T.t) {
                T.info("Queueing blocknumber " + blockNumber + " for download.");
            }
            Packet p = netMan.createPacketForSend();
            p.writeByte(Command.GETBLOCK.value());
            p.writeInt(blockNumber);
            send(p);
            consumerQueue.add(new BlockConsumer(this, blockNumber, download.getStorage()));
            blockNumber = -1;
        }

        switchMode(Mode.RAW);
    }

    @Override
    protected int getConnectionId() {
        return CONNECTION_ID;
    }

    @Override
    public int getConnectionIdForRemote() {
        return UploadConnection.CONNECTION_ID;
    }

    public boolean isDownloadingFd() {
        return consumerQueue.size() > 0 && consumerQueue.get(0) instanceof FileDescriptorConsumer;
    }

    public boolean isDownloadingBlock() {
        return consumerQueue.size() > 0 && consumerQueue.get(0) instanceof BlockConsumer;
    }

    public void blockDownloadComplete(int blockNumber) throws IOException {
        if (T.t) {
            T.info("Block " + blockNumber + " download complete.");
        }
        consumerQueue.remove(0);
        download.signalBlockComplete(blockNumber);
    }

    public void startDownloadingBlock() throws IOException {
        int blockNumber = download.selectBestBlockForDownload(getRemoteFriend());
        if (blockNumber == -1) {
            if (consumerQueue.size() == 0) {
                if (T.t) {
                    T.info("Did not find anything to download. Closing connection.");
                }
                sendGracefulClose();
            } else {
                if (T.t) {
                    T.info("Nothing to download but something in queue. Don't shutdown quite yet.");
                }
            }
        } else {
            setStatusString("Downloading block " + blockNumber);

            if (T.t) {
                T.info("Starting to download block " + blockNumber);
            }
            sendOneOrMoreGetBlocks(blockNumber);
        }
    }

    public Download getDownload() {
        return download;
    }

    public boolean isDownloading() {
        return isDownloadingBlock() || isDownloadingFd();
    }

    public boolean readyToStartDownload() {
        return !isDownloading() && !inHandshake;
    }

    public void fileDescriptorReceived() {
        consumerQueue.remove(0);
        if (T.t) {
            T.ass(consumerQueue.size() == 0, "Somethings in queue when it shouldn't");
        }
    }

    @Override
    public String toString() {
        if (getRemoteFriend() == null) {
            return "Establishing connection (download)";
        }
        return getRemoteFriend().getNickname() + " (download)";
    }
}
