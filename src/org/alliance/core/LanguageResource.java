package org.alliance.core;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Bastvera
 */
public class LanguageResource {

    private ResourceBundle myResources;

    public LanguageResource() {
        try {
            URL[] url = {new File("language/").toURI().toURL()};
            myResources = ResourceBundle.getBundle("alliance", Locale.UK, new URLClassLoader(url));
        } catch (MalformedURLException ex) {
            Logger.getLogger(LanguageResource.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getLocalizedString(Class<?> c, String key) {
        return myResources.getString(c.getName() + "." + key);
    }
}
