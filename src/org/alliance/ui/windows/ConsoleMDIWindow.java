package org.alliance.ui.windows;

import com.stendahls.nif.ui.mdi.MDIWindow;
import org.alliance.launchers.console.Console;
import org.alliance.ui.UISubsystem;

import javax.swing.*;
import java.awt.event.ActionEvent;
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
public class ConsoleMDIWindow extends AllianceMDIWindow implements Console.Printer {
	private JTextArea textarea;
    private JTextField chat;
    private Console console;

    public ConsoleMDIWindow(UISubsystem ui) throws Exception {
        super(ui.getMainWindow().getMDIManager(), "console", ui);

        console = new Console(ui.getCore());
        console.setPrinter(this);

        textarea = (JTextArea)xui.getComponent("textarea");
        chat = (JTextField)xui.getComponent("chat");

        setTitle("Debug console");
        postInit();
    }

    public void windowSelected() {
        super.windowSelected();
        chat.requestFocus();
    }

    public void EVENT_chat(ActionEvent e) throws Exception {
        send(chat.getText());
        chat.setText("");
    }

    private void send(String text) throws Exception {
        console.handleLine(text);
    }

    public String getIdentifier() {
        return "console";
    }

    public void save() throws Exception {}
    public void revert() throws Exception {}
    public void serialize(ObjectOutputStream out) throws IOException {}
    public MDIWindow deserialize(ObjectInputStream in) throws IOException { return null; }

    public void println(final String line) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                textarea.append(line+"\n");
            }
        });
    }

    public Console getConsole() {
        return console;
    }
}
