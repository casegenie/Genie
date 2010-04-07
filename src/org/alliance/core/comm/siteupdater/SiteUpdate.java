package org.alliance.core.comm.siteupdater;

import org.alliance.Version;
import org.alliance.core.CoreSubsystem;
import org.alliance.core.file.FileManager;
import org.alliance.core.LanguageResource;
import org.alliance.core.comm.T;
import org.alliance.launchers.OSInfo;
import static org.alliance.core.CoreSubsystem.KB;
import static org.alliance.launchers.ui.DirectoryCheck.STARTED_JAR_NAME;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.jar.JarFile;

/**
 *
 * @author Bastvera
 */
public class SiteUpdate implements Runnable {

    private final CoreSubsystem core;
    private static final String JAR_URL = "http://alliancep2pbeta.googlecode.com/svn/updater/version/alliance.new";
    private static final String INFO_URL = "http://alliancep2pbeta.googlecode.com/svn/updater/version/build.info";
    private boolean alive = true;
    private String updateFilePath;
    private String orginalFilePath;
    private String siteVersion = Version.VERSION;
    private int siteBuild = Version.BUILD_NUMBER;
    private boolean updateAttemptHasBeenMade = false;

    public SiteUpdate(CoreSubsystem core) throws IOException {
        this.core = core;
        updateFilePath = core.getFileManager().getCache().getCompleteFilesFilePath().getCanonicalPath() + System.getProperty("file.separator") + FileManager.UPDATE_FILE_NAME;
        orginalFilePath = new File(STARTED_JAR_NAME).getCanonicalPath();
    }

    public int getSiteBuild() {
        return siteBuild;
    }

    public String getSiteVersion() {
        return siteVersion;
    }

    @Override
    public void run() {
        //Delayed start
        try {
            Thread.sleep(15 * 1000);
        } catch (InterruptedException ex) {
        }
        while (alive) {
            try {
                if (isNewVersionAvailable()) {
                    core.siteUpdateAvailable();
                } else {
                    core.getUICallback().statusMessage(LanguageResource.getLocalizedString(getClass(), "noupdates"), true);
                }
            } catch (IOException ex) {
                core.getUICallback().statusMessage(LanguageResource.getLocalizedString(getClass(), "updatecheckfailed"), true);
            }
            try {
                Thread.sleep(4 * 60 * 60 * 1000);//4h
            } catch (InterruptedException ex) {
            }
        }
    }

    public boolean isNewVersionAvailable() throws IOException {
        core.getUICallback().statusMessage(LanguageResource.getLocalizedString(getClass(), "checking"), true);
        URLConnection http = new URL(INFO_URL).openConnection();
        http.setConnectTimeout(1000 * 15);
        http.setReadTimeout(1000 * 15);
        BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream(), "UTF8"));
        String line;
        while ((line = in.readLine()) != null) {
            if (line.startsWith("version:")) {
                siteVersion = line.substring(8);
            } else if (line.startsWith("build:")) {
                siteBuild = Integer.parseInt(line.substring(6));
            }
        }
        in.close();
        if (siteBuild > Version.BUILD_NUMBER) {
            return true;
        } else {
            return false;
        }
    }

    public void beginDownload() {
        core.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    core.getUICallback().statusMessage(LanguageResource.getLocalizedString(getClass(), "downloadingstart"), true);
                    URLConnection http = new URL(JAR_URL).openConnection();
                    http.setConnectTimeout(1000 * 15);
                    http.setReadTimeout(1000 * 15);
                    InputStream in = http.getInputStream();
                    OutputStream out = new FileOutputStream(updateFilePath);
                    byte[] buf = new byte[32 * KB];
                    int read;
                    int readed = 0;
                    while ((read = in.read(buf)) > 0) {
                        readed += read;
                        core.getUICallback().statusMessage(LanguageResource.getLocalizedString(getClass(), "downloading",
                                Integer.toString(readed), Integer.toString(http.getContentLength())),
                                true);
                        out.write(buf, 0, read);
                    }
                    out.close();
                    in.close();
                    checkJarFile();
                    core.updateDownloaded();
                    core.getUICallback().statusMessage(LanguageResource.getLocalizedString(getClass(), "downloadok"), true);
                } catch (IOException ex) {
                    core.getUICallback().statusMessage(LanguageResource.getLocalizedString(getClass(), "downloadfailed"), true);
                } catch (CertificateException ex) {
                    core.getUICallback().statusMessage(LanguageResource.getLocalizedString(getClass(), "downloadcorrupt"), true);
                } catch (SecurityException ex) {
                    core.getUICallback().statusMessage(LanguageResource.getLocalizedString(getClass(), "downloadcorrupt"), true);
                }
            }
        });
    }

    private void checkJarFile() throws IOException, CertificateException {
        core.getUICallback().statusMessage(LanguageResource.getLocalizedString(getClass(), "downloadverify"), true);

        if (T.t) {
            T.info("Loading certificate");
        }
        InputStream inStream = core.getRl().getResourceStream("alliance.cer");
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);
        inStream.close();

        if (T.t) {
            T.info("Verifying jar");
        }
        JarVerifier jv = new JarVerifier(new X509Certificate[]{cert});
        jv.verify(new JarFile(updateFilePath, true));

        core.getUICallback().statusMessage(LanguageResource.getLocalizedString(getClass(), "downloadverifyok"));

        if (T.t) {
            T.info("Jar verified! Updating...");
        }
    }

    public void prepareUpdate() {
        try {
            if (updateAttemptHasBeenMade) {
                if (T.t) {
                    T.info("No need to try to upgrade to new version several times.");
                }
            }
            updateAttemptHasBeenMade = true;
            core.runUpdater(updateFilePath, orginalFilePath, siteVersion, siteBuild);
        } catch (Exception e) {
            core.getUICallback().statusMessage(LanguageResource.getLocalizedString(getClass(), "updatefailed"), true);
            if (!OSInfo.isWindows()) {
                core.getUICallback().statusMessage(LanguageResource.getLocalizedString(getClass(), "updatemanual"), true);
            }
        }
    }
}
