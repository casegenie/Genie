package org.alliance.core.plugins;

import org.alliance.core.CoreSubsystem;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2008-jun-05
 * Time: 19:16:39
 * To change this template use File | Settings | File Templates.
 */
public interface PlugIn {
    abstract void init(CoreSubsystem core) throws Exception;
    abstract void shutdown() throws Exception;
}
