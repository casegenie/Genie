package org.alliance.ui.windows;

import com.stendahls.nif.ui.mdi.MDIWindow;
import com.stendahls.ui.JHtmlLabel;
import org.alliance.core.file.filedatabase.FileType;
import org.alliance.ui.UISubsystem;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-feb-09
 * Time: 14:12:18
 */
public class WelcomeMDIWindow extends AllianceMDIWindow {
	public static final String IDENTIFIER = "welcome";
    private JHtmlLabel label;
    private JLabel imageLabel;
    private UISubsystem ui;

    private String html, image;

    public WelcomeMDIWindow() {
    }

    public WelcomeMDIWindow(UISubsystem ui) throws Exception {
        super(ui.getMainWindow().getMDIManager(), "welcome", ui);
        setWindowType(WINDOWTYPE_OBJECT);
        this.ui = ui;
        BufferedReader r = new BufferedReader(new InputStreamReader(ui.getRl().getResourceStream("welcome.html")));
        StringBuffer data = new StringBuffer();
        String line = null;
        while((line = r.readLine()) != null) data.append(line);
        init(data.toString(), "Changelog");
    }

    private void init(String html, String title) throws Exception {
        this.html = html;

        label = (JHtmlLabel) xui.getComponent("label");
        label.setText(html);

        label.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                String s = e.getURL().toString();
                s = s.substring(s.length()-1);
                FileType ft = FileType.getFileTypeById(Integer.parseInt(s));

                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        ui.getMainWindow().getMDIManager().selectWindow(ui.getMainWindow().getSearchWindow());
                        ui.getMainWindow().getSearchWindow().searchForNewFilesOfType(ft);
                    } catch (IOException e1) {
                        ui.handleErrorInEventLoop(e1);
                    }
                } else if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
                    ui.getMainWindow().setStatusMessage("Click here to search for new files in type "+ft.description());
                }
            }
        });

//            imageLabel = (JLabel) xui.getComponent("image");
//            imageLabel.setIcon(new ImageIcon(ui.getRl().getResource(image)));

        setTitle(title);
        postInit();
    }

    public void save() throws Exception {
    }

    public String getIdentifier() {
        return IDENTIFIER;
    }

    public void revert() throws Exception {
    }

    public void serialize(ObjectOutputStream out) throws IOException {
    }

    public MDIWindow deserialize(ObjectInputStream in) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
