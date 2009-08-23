package org.alliance.ui.windows;

import com.stendahls.nif.ui.mdi.MDIWindow;
import com.stendahls.trace.TraceWindow;
import org.alliance.ui.UISubsystem;

import javax.swing.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-05
 * Time: 13:21:55
 * To change this template use File | Settings | File Templates.
 */
public class TraceMDIWindow extends AllianceMDIWindow {
	private TraceWindow tw;

    public TraceMDIWindow(UISubsystem ui) throws Exception {
        super(ui.getMainWindow().getMDIManager(), "trace", ui);

        tw = new TraceWindow(false);
        JPanel p = (JPanel)xui.getComponent("panel");
        p.removeAll();
        p.add(tw.getContentPane());

        setTitle("Trace");
        postInit();
    }

    public void trace(int level, String text, Exception stackTrace) throws IOException {
        tw.print(level, text, stackTrace);
    }

    public String getIdentifier() {
        return "trace";
    }

    public void save() throws Exception {}
    public void revert() throws Exception {}
    public void serialize(ObjectOutputStream out) throws IOException {}
    public MDIWindow deserialize(ObjectInputStream in) throws IOException { return null; }
}
