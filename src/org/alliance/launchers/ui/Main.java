package org.alliance.launchers.ui;

import com.stendahls.nif.util.SimpleTimer;
import org.alliance.Subsystem;
import org.alliance.Version;
import org.alliance.core.ResourceSingelton;
import org.alliance.core.T;
import org.alliance.launchers.OSInfo;
import org.alliance.launchers.StartupProgressListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-05
 * Time: 10:00:56
 * To change this template use File | Settings | File Templates.
 */
public class Main {

    private static final int STARTED_SIGNAL_PORT = 56345;

    public static void main(String[] args) {
        try {
            System.out.println("Launching Alliance v" + Version.VERSION + " build " + Version.BUILD_NUMBER);
            System.setProperty("alliance.build", "" + Version.BUILD_NUMBER);

            boolean allowMultipleInstances = argsContain(args, "/allowMultipleInstances") || new File("allowMultipleInstances").exists();
            boolean runMinimized = argsContain(args, "/min");

            if (!allowMultipleInstances) {
                checkIfAlreadyRunning(!runMinimized);
            }

            Runnable r = null;
            if (!runMinimized) {
                r = (Runnable) Class.forName("org.alliance.launchers.SplashWindow").newInstance();
            }

            String s = getSettingsFile();
            for (int i = 0; i < args.length; i++) {
                if (!args[i].startsWith("/")) {
                    s = args[i];
                }
            }
            Subsystem core = initCore(s, (StartupProgressListener) r);
            if (core == null) {
                return; //oops. core crashed. Error message has been displayd. just bail.
            }
            Subsystem tray = null;
            try {
                tray = initTrayIcon(core);
                OSInfo.setSupportsTrayIcon(true);
            } catch (Throwable t) {
                OSInfo.setSupportsTrayIcon(false);
            }

            if (OSInfo.supportsTrayIcon()) {
                if (!runMinimized) {
                    ((Runnable) tray).run(); //open ui
                    if (r != null) {
                        r.run(); //close splashwindow
                    }
                }

                if (!allowMultipleInstances) {
                    startStartSignalThread(tray);
                }
            } else {
                initUI(core);
                if (r != null) {
                    r.run();
                }
            }
        } catch (Throwable e) {
            try {
                new File("logs").mkdirs();
                PrintWriter writer = new PrintWriter("logs/crash.log");
                e.printStackTrace(writer);
                writer.close();
            } catch (FileNotFoundException e2) {
                e.printStackTrace();
            }
        }
    }

    private static String getSettingsFile() {
        String s = "data/settings.xml";

        String home = System.getProperty("user.home");
        String system = System.getProperty("os.name");
        if (OSInfo.isLinux()) {
            home = home + "/.alliance/";
        } else {
            home = "";
        }

        return home + s;
    }

    private static boolean argsContain(String[] args, String pattern) {
        if (args != null) {
            for (String s : args) {
                if (pattern.equalsIgnoreCase(s)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void checkIfAlreadyRunning(boolean startUI) {
        try {
            Socket s = new Socket("127.0.0.1", STARTED_SIGNAL_PORT);
            OutputStream o = s.getOutputStream();
            o.write(startUI ? 1 : 0);
            o.flush();
            Thread.sleep(1000);
            System.out.println("Program already running. Closing this program instance.");
            System.exit(0);
        } catch (Exception e) {
            System.out.println("Program does not seem to be running. Starting.");
        }
    }
    private static Thread signalThread;
    private static ServerSocket signalServerSocket;

    private static void startStartSignalThread(final Subsystem tray) {
        signalThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    signalServerSocket = new ServerSocket(STARTED_SIGNAL_PORT, 0, InetAddress.getByName("127.0.0.1"));
                    while (true) {
                        try {
                            Socket s = signalServerSocket.accept(); //connection is made on this port if user wants to open the ui
                            if (signalThread == null) {
                                return;
                            }
                            int b = s.getInputStream().read();
                            s.close();
                            if (b == 1) {
                                ((Runnable) tray).run(); //open ui
                            }
                        } catch (IOException e) {
                            if (signalThread == null) {
                                return;
                            }
                            e.printStackTrace();
                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException e1) {
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        signalThread.setDaemon(true);
        signalThread.start();
    }

    public static void stopStartSignalThread() {
        if (signalThread != null) {
            Thread t = signalThread;
            signalThread = null;
            try {
                signalServerSocket.close();
            } catch (Exception e) {
            }
            try {
                t.join();
            } catch (InterruptedException e) {
            }
        }
    }

    private static Subsystem initTrayIcon(Subsystem core) throws Throwable {
        try {
            if (T.t) {
                T.info("Starting Java 6 tray icon...");
            }
            Subsystem tray = (Subsystem) Class.forName("org.alliance.launchers.ui.Java6TrayIconSubsystem").newInstance();
            tray.init(ResourceSingelton.getRl(), core);
            return tray;
        } catch (Throwable e) {
            if (T.t) {
                T.warn("Java 6 tray icon not supported. Falling back to old code.");
            }
            Subsystem tray = (Subsystem) Class.forName("org.alliance.launchers.ui.JDesktopTrayIconSubsystem").newInstance();
            tray.init(ResourceSingelton.getRl(), core);
            return tray;
        }
    }

    private static Subsystem initCore(String settings, StartupProgressListener l) {
        try {
            SimpleTimer s = new SimpleTimer();
            Subsystem core = (Subsystem) Class.forName("org.alliance.core.CoreSubsystem").newInstance();
            core.init(ResourceSingelton.getRl(), settings, l);
            if (T.t) {
                T.info("" +
                        "Subsystem CORE started in " + s.getTime());
            }
            return core;
        } catch (Throwable t) {
            reportError(t);
            System.err.println(t);
            return null;
        }
    }

    public static void reportError(Throwable t) {
        try {
            t.printStackTrace();
            //report error. Use reflection to init dialogs because we want NO references to UI stuff in this
            //class - we want this class to load fast (ie load minimal amount of classes)
            Object errorDialog = Class.forName("com.stendahls.ui.ErrorDialog").newInstance();
            Method m = errorDialog.getClass().getMethod("init", Throwable.class, boolean.class);
            m.invoke(errorDialog, t, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void initUI(Subsystem core) {
        try {
            System.out.println("starting ui");
            SimpleTimer s = new SimpleTimer();
            Subsystem ui = (Subsystem) Class.forName("org.alliance.ui.UISubsystem").newInstance();
            ui.init(ResourceSingelton.getRl(), core);
            if (T.t) {
                T.trace("Subsystem UI started in " + s.getTime());
            }
        } catch (Exception t) {
            reportError(t);
        }
    }
}
