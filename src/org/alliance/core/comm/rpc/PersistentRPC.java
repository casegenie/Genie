package org.alliance.core.comm.rpc;

import org.alliance.core.comm.RPC;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-apr-19
 * Time: 15:38:42
 *
 * A persistent RPC is an RPC that can be sent using NetworkManager.sendPersistently(...). It is guaranteed to
 * arrive at its destination. Even if the destination will not be connected for several days and even if
 * the application is restarted.
 *
 */
public abstract class PersistentRPC extends RPC implements Serializable {
    private static final long TIMEOUT = 1000 * 60 * 60 * 24 * 31; 
    private int destinationGuid;
    private long timestamp;
    protected boolean hasBeenQueuedForLaterSend;

    public int getDestinationGuid() {
        return destinationGuid;
    }

    public void setDestinationGuid(int destinationGuid) {
        this.destinationGuid = destinationGuid;
    }

    public void resetTimestamp() {
        timestamp = System.currentTimeMillis();
    }

    /**
     * Return true if this PersistentRPC is older than a month.
     */
    public boolean hasExpired() {
        return System.currentTimeMillis() - timestamp > TIMEOUT;
    }

    public void notifyRPCQueuedForLaterSend() {
        hasBeenQueuedForLaterSend = true;
    }
}
