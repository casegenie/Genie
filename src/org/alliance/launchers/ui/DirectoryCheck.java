package org.alliance.launchers.ui;

import java.io.File;
import org.alliance.core.LauncherJava;

/**
 *
 * @author Bastvera
 */
public class DirectoryCheck {

    public DirectoryCheck() {
        if (!new File("alliance.jar").exists()) {
            String newCurrentDirPath = getClass().getResource("/res/alliance.cer").toString();
            newCurrentDirPath = newCurrentDirPath.replace("file:/", "");
            newCurrentDirPath = newCurrentDirPath.replace("jar:", "");
            newCurrentDirPath = newCurrentDirPath.substring(0, newCurrentDirPath.indexOf("alliance.jar"));
            try {
                LauncherJava.execJar(newCurrentDirPath + "alliance.jar", new String[0], newCurrentDirPath);
                System.exit(0);
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(0);
            }
        }
    }
}
