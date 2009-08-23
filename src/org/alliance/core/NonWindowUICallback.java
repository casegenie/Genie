package org.alliance.core;

import org.alliance.core.comm.SearchHit;
import org.alliance.core.node.Friend;
import org.alliance.core.node.Node;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-02
 * Time: 21:35:21
 * To change this template use File | Settings | File Templates.
 */
public class NonWindowUICallback implements UICallback {
    public void nodeOrSubnodesUpdated(Node node) {
    }

    public void noRouteToHost(Node node) {
    }

    public void pluginCommunicationReceived(Friend source, String data) {
    }

    public void messageRecieved(int srcGuid, String message) {
        // @todo: handle this in some good way. balloon should be shown. should add it to ui - and balloon click should open ui with chat window
    }

    public void searchHits(int srcGuid, int hops, List<SearchHit> hits) {
    }

    public void trace(int level, String message, Exception stackTrace) {
    }

    public void handleError(Throwable e, Object source) {
        System.err.println("Error for : "+source+": ");
        e.printStackTrace();
    }

    public void statusMessage(String s) {
        if (!CoreSubsystem.isRunningAsTestSuite()) System.out.println(s);
    }

    public void toFront() {
    }

    public void signalFriendAdded(Friend friend) {
    }

    public boolean isUIVisible() {
        return false;
    }

    public void logNetworkEvent(String event) {
    }

    public void receivedShareBaseList(Friend friend, String[] shareBaseNames) {}

    public void receivedDirectoryListing(Friend friend, int i, String s, String[] files) {
    }

    public void newUserInteractionQueued(NeedsUserInteraction ui) {
        
    }

    public void firstDownloadEverFinished() {}

    public void callbackRemoved() {
    }

    public void signalFileDatabaseFlushStarting() {
    }

    public void signalFileDatabaseFlushComplete() {
    }
}
