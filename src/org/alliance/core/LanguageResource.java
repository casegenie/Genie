package org.alliance.core;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;

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
        return getResource(getKeyHeader(c), key);
    }

    public static String getLocalizedString(Class<?> c, String key, String... params) {
        String paramsString = getResource(getKeyHeader(c), key);
        for (String param : params) {
            param = param.replace("\\", "\\\\");
            paramsString = paramsString.replaceFirst("\\$[^\\$]*\\$", param);
        }
        return paramsString;
    }

    public static void getLocalizedXUIToolbar(Class<?> c, ArrayList<JButton> buttons) {
        for (JButton b : buttons) {
            String tooltipText = b.getToolTipText();
            tooltipText = tooltipText.replace("$DES$", getResource(getKeyHeader(c), "toolbar", b.getActionCommand(), "description"));
            tooltipText = tooltipText.replace("$NAME$", getResource(getKeyHeader(c), "toolbar", b.getActionCommand(), "name"));
            b.setToolTipText(tooltipText);
        }
    }

    private static String getKeyHeader(Class<?> c) {
        return c.getName().substring(13).replaceAll("\\$\\d*", "");
    }

    private static String getResource(String... strings) {
        StringBuilder sb = new StringBuilder(50);
        for (String s : strings) {
            sb.append(s);
            sb.append(".");
        }
        sb.deleteCharAt(sb.length() - 1);
        return LANGUAGE_BUNDLE.getString(sb.toString());
    }

    /*public static void translateXUIElements(Collection c) {
    for (Object o : c) {
    if (o instanceof XUIElement) {
    XUIElement element = (XUIElement) o;
    System.out.println(element.getId());
    }
    }
    }*/
}
