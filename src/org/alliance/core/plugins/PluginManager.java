package org.alliance.core.plugins;

import org.alliance.core.Manager;
import org.alliance.core.CoreSubsystem;
import org.alliance.core.settings.Plugin;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

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

    @Override
    public void init() throws Exception {
        if (!core.getSettings().getPluginlist().isEmpty()) {
            setupClassLoader();
            for (org.alliance.core.settings.Plugin p : core.getSettings().getPluginlist()) {
                try {
                    PlugIn pi = (PlugIn) classLoader.loadClass(p.retrievePluginClass()).newInstance();
                    pi.init(core);
                    plugIns.add(pi);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, "There was an error loading the main class of  " + p.getJar() + " so this plugin will be disabled");
                }
            }
        }
    }

    private void setupClassLoader() throws Exception {
        List<URL> l = new ArrayList<URL>();
        Iterator<Plugin> itr = core.getSettings().getPluginlist().iterator();
        while (itr.hasNext()) {
            org.alliance.core.settings.Plugin p = itr.next();
            File f = new File(p.getJar());
            if (f.exists()) {
                l.add(f.toURI().toURL());
            } else {
                int response = JOptionPane.showConfirmDialog(null, "There was an error loading the plugin " + f + " because the file does not exist \n" +
                        "would you like to attempt to locate the jar?", "Error Loading jar", JOptionPane.YES_NO_OPTION);
                if (response == JOptionPane.YES_OPTION) {
                    JFileChooser file = new JFileChooser();
                    FileNameExtensionFilter filter = new FileNameExtensionFilter("JAR files", "JAR", "jar", "Jar");
                    file.setFileFilter(filter);
                    int returnVal = file.showOpenDialog(null);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        try {
                            Plugin newPlugin = new Plugin();
                            newPlugin.init(file.getSelectedFile());
                            l.add(file.getSelectedFile().toURI().toURL());
                            //I'm not sure if the order matters in the arraylist....don't think so
                            core.getSettings().getPluginlist().remove(p);
                            core.getSettings().getPluginlist().add(newPlugin);
                        } catch (IOException e) {
                            JOptionPane.showMessageDialog(null,
                                    "Could not parse given jar file to find entry point");
                        }
                    }
                } else {
                    core.getSettings().getPluginlist().remove(p);
                    //This is a hack to prevent a java concurrent modification exception
                    //might waste a few cycles, but it should work, if there's a better way...please update
                    itr = core.getSettings().getPluginlist().iterator();
                    l = new ArrayList<URL>();
                }
            }
        }
        URL[] u = new URL[l.size()];
        u = l.toArray(u);
        classLoader = new URLClassLoader(u);
    }

    public void shutdown() throws Exception {
        for (PlugIn pi : plugIns) {
            pi.shutdown();
        }
    }
}
