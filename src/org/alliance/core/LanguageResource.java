package org.alliance.core;

import com.stendahls.XUI.XUIElement;
import com.stendahls.ui.JHtmlLabel;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextArea;

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
        StringBuilder paramsString = new StringBuilder(getResource(getKeyHeader(c), key));
        for (String param : params) {
            int paramStart = paramsString.indexOf("<%");
            int paramEnd = paramsString.indexOf("%>") + 2;
            if (paramStart != -1 && paramEnd != -1) {
                paramsString = paramsString.replace(paramStart, paramEnd, param);
            }
        }
        return paramsString.toString();
    }

    public static void getLocalizedXUIToolbar(Class<?> c, ArrayList<JButton> buttons) {
        for (JButton b : buttons) {
            String tooltipText = b.getToolTipText();
            tooltipText = tooltipText.replace("%DES%", getResource(getKeyHeader(c), "toolbar", b.getActionCommand(), "description"));
            tooltipText = tooltipText.replace("%NAME%", getResource(getKeyHeader(c), "toolbar", b.getActionCommand(), "name"));
            b.setToolTipText(tooltipText);
        }
    }

    public static void translateXUIElements(Class<?> c, Collection coll) {
        for (Object o : coll) {
            XUIElement element = (XUIElement) o;
            JComponent comp = (JComponent) element.getComponent();
            if (comp.getToolTipText() != null && !comp.getToolTipText().isEmpty()) {
                comp.setToolTipText(getResource(getKeyHeader(c), "xui", element.getId(), "tooltip"));
            }
            if (comp instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) comp;
                if (button.getText() != null && !button.getText().isEmpty()) {
                    button.setText(getResource(getKeyHeader(c), "xui", element.getId()));
                }
                continue;
            }
            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                if (label.getText() != null && !label.getText().isEmpty()) {
                    label.setText(getResource(getKeyHeader(c), "xui", element.getId()));
                }
                continue;
            }
            if (comp instanceof JHtmlLabel) {
                JHtmlLabel label = (JHtmlLabel) comp;
                if (label.getText() != null && !label.getText().isEmpty()) {
                    label.setText(getResource(getKeyHeader(c), "xui", element.getId()));
                }
                continue;
            }
            if (comp instanceof JTextArea) {
                JTextArea area = (JTextArea) comp;
                if (area.getText() != null && !area.getText().isEmpty()) {
                    area.setText(getResource(getKeyHeader(c), "xui", element.getId()));
                }
                continue;
            }
            // System.out.println(element.getId());
            // System.out.println(element.getComponent());
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
}
