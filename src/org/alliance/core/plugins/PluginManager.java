package org.alliance.core.plugins;

import org.alliance.core.Manager;
import org.alliance.core.CoreSubsystem;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2008-jun-05
 * Time: 19:04:56
 * To change this template use File | Settings | File Templates.
 */
public class PluginManager extends Manager {
    private CoreSubsystem core;
    private URLClassLoader classLoader;
    private List<PlugIn> plugIns = new ArrayList<PlugIn>();

    public PluginManager(CoreSubsystem core) {
        this.core = core;
    }

    public void init() throws Exception {
        if (core.getSettings().getPluginlist().size() > 0) {
            setupClassLoader();
            for(org.alliance.core.settings.Plugin p : core.getSettings().getPluginlist()) {
                try {
                    PlugIn pi = (PlugIn)classLoader.loadClass(p.getPluginclass()).newInstance();
                    pi.init(core);
                    plugIns.add(pi);
                } catch(Exception e) {
                    throw new Exception("Could not start plugin "+p.getPluginclass(), e);
                }
            }
        }
    }

    private void setupClassLoader() throws Exception {
        List<URL> l = new ArrayList<URL>();
        for(org.alliance.core.settings.Plugin p : core.getSettings().getPluginlist()) {
            File f = new File(p.getJar());
            if (!f.exists()) throw new IOException("Could not find jar: "+f);
            l.add(f.toURI().toURL());
        }
        URL[] u = new URL[l.size()];
        u = l.toArray(u);
        classLoader = new URLClassLoader(u);
    }

    public void shutdown() throws Exception {
        for(PlugIn pi : plugIns) pi.shutdown();
    }
}
