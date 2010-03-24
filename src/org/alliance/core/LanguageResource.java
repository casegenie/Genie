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

    private static ResourceBundle LANGUAGE_BUNDLE;

    public LanguageResource() {
        try {
            URL[] url = {new File("language/").toURI().toURL()};
            LANGUAGE_BUNDLE = ResourceBundle.getBundle("alliance", Locale.UK, new URLClassLoader(url));
        } catch (MalformedURLException ex) {
            Logger.getLogger(LanguageResource.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static String getLocalizedString(Class<?> c, String key) {
        return LANGUAGE_BUNDLE.getString(c.getName() + "." + key);
    }
}
