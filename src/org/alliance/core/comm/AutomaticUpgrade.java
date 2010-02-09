package org.alliance.core.comm;


import org.alliance.Version;
import org.alliance.core.CoreSubsystem;
import org.alliance.core.ResourceSingelton;

import static org.alliance.core.CoreSubsystem.KB;
import org.alliance.core.file.blockstorage.T;
import org.alliance.core.file.filedatabase.FileDescriptor;
import org.alliance.core.file.hash.Hash;
import org.alliance.core.node.Friend;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-feb-07
 * Time: 13:44:21
 */
/*
 * IMHO:
 * 	The AutomaticUpgrade class should handle only upgrading the local client,
 *  and we should have something else that is in charge of sending out upgrade notifications
 *  and copying files.
 *  
 * 	TODO: Should we just store the public key info in code?  I can't see why not.
 */
public class AutomaticUpgrade {

    public static final String UPGRADE_FILENAME = "alliance.upgrade";
    public static final File SOURCE_JAR = new File("alliance.dat");
    
    private CoreSubsystem core;
    private boolean upgradeAttemptHasBeenMade = false;
    private int newVersionNumber;
    private Hash binaryHash, signatureHash;

    private enum ValidStatus {
    	INVALID, INSUFFICIENT, VALID;
    }
    
    public AutomaticUpgrade(CoreSubsystem core) throws IOException {
        this.core = core;
    }

    public static void copyFile(File src, File dst) throws IOException {
        if (T.t) {
            T.info("Copying " + src + " -> " + dst + "...");
        }
        FileInputStream in = new FileInputStream(src);
        FileOutputStream out = new FileOutputStream(dst);
        byte buf[] = new byte[40 * KB];
        int read;
        while ((read = in.read(buf)) != -1) {
            out.write(buf, 0, read);
        }
        out.flush();
        out.close();
        in.close();
        if (T.t) {
            T.info("File copied.");
        }
    }
    
    
    //TODO: Make the automatic upgrade handle an upgrade with a single method call.
    public void beginDownloadAndUpgrade(int versionNumber, Friend remote, Hash binaryHash, Hash signatureHash) {
    	if(versionNumber <= Version.BUILD_NUMBER) return;
    	
    	this.newVersionNumber = versionNumber;
        this.binaryHash = binaryHash;
        this.signatureHash = signatureHash;
        
        if (core.getFileManager().getFileDatabase().contains(binaryHash) &&
        		core.getFileManager().getFileDatabase().contains(signatureHash)) {
            if (T.t) {
                T.info("Upgrade already in my share. Start upgrade.");
            }
            beginUpgrade();
        } else {
            ArrayList<Integer> al = new ArrayList<Integer>();
            al.add(remote.getGuid());
            try {
            	core.getNetworkManager().getDownloadManager().queueDownload(binaryHash, core.getFileManager().getCache(), "Alliance Upgrade", al, true);
            	core.getNetworkManager().getDownloadManager().queueDownload(signatureHash, core.getFileManager().getCache(), "Alliance Upgrade Signature", al, true);
            } catch(IOException e) 
            {
            	//WhyTF does a simple thing to queue a download give me an I/O error.
            	//TODO: Handle an error that shouldn't happen.
            }
            
        }
    }

    /*
     * Called by the CacheStorage object when a new download to the cache finishes
     * Calls verifyUpgrade to check the files and, if valid, does the upgrade.
     */
    public void beginUpgrade() {
    	FileDescriptor binaryFD;
    	FileDescriptor sigFD;
    	try {
    		binaryFD = core.getShareManager().getFileDatabase().getFd(binaryHash);
    		sigFD = core.getShareManager().getFileDatabase().getFd(signatureHash);
    	} catch(IOException e) {
    		//TODO: WTF happened? I really don't know.
    		return;
    	}
    	
    	if(binaryFD == null || sigFD == null) return;  //If we don't have both files, we're not ready to upgrade yet.
    	
    	File binaryFile = new File(binaryFD.getFullPath());
    	File sigFile = new File(sigFD.getFullPath());
    	
    	boolean legitimate = false;
    	
    	try {
    		legitimate = verifyUpgrade(newVersionNumber, binaryFile, sigFile);
    	} catch(Exception e) {
    		//TODO: Notify someone of something.
    	}
    	
    	if(legitimate) {
    		doUpgrade(binaryFile);
    	} else {
    		binaryFile.delete();
    		sigFile.delete();
    	}
    	
    	
    }
    
    private void doUpgrade(File binaryFile) {
    	if (upgradeAttemptHasBeenMade) {
            if (T.t) {
                T.info("No need to try to upgrade to new version several times.");
            }
        }
        upgradeAttemptHasBeenMade = true;
        
        File runningBinary = findRunningBinary();
        if(runningBinary == null || !runningBinary.canWrite())
        {
        	//TODO: Log failure to copy new binary. Inform the user?
        	return;
        }
        
        core.getUICallback().statusMessage("Upgrade verified! Restarting.");
        
        try {
        	copyFile(binaryFile, runningBinary);
        } catch(IOException e) {
        	//TODO: Log failure.  Inform user?
        	return;
        }
        
        try {
        	core.softRestart();
        } catch(IOException e) {
        	//TODO: Log failure and ask user to restart.
        }
    }
    
    /*
     * This method, given a build number, binaryFile and signatureFile
     * verifies that the signature reflects the binaryFile and build number advertised.
     */
    private static boolean verifyUpgrade(int build, File binaryFile, File signatureFile) throws IOException, SignatureException, InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException {
    	PublicKey pubKey = readPublicKey(0);

    	Signature s = Signature.getInstance("SHA256withRSA");

    	s.initVerify(pubKey);

    	//By doing this, we ensure that no replay attacks will happen.  Hopefully.
    	//This DataOutputStream method also allows one to verify other data about the binary that isn't actually in the binary
    	ByteArrayOutputStream versionStream = new ByteArrayOutputStream();
    	DataOutputStream outputStream = new DataOutputStream(versionStream);
    	
		outputStream.writeInt(build);
		outputStream.flush();

		s.update(versionStream.toByteArray());

		outputStream.close();

    	InputStream binary = new FileInputStream(binaryFile);

		byte[] binaryBytes = new byte[8192];
		int len;
		while((len = binary.read(binaryBytes)) != -1) {
			s.update(binaryBytes, 0, len);
		}

		DataInputStream dis = new DataInputStream(new FileInputStream(signatureFile));

		byte[] signature = new byte[(int)signatureFile.length()];
		dis.readFully(signature);
		dis.close();

		return s.verify(signature);
    }
    
    private static PublicKey readPublicKey(int i) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
    	InputStream is = ResourceSingelton.getRl().getResourceStream(String.format("pubkeys/%d.pub",i));
    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
    	
    	byte[] b = new byte[8192];
    	int len=0;
    	while((len = is.read(b, 0, 8192)) != -1) {
    		bos.write(b,0,len);
    	}
    	
    	is.close();
		return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bos.toByteArray()));
	}
    
    private static File findRunningBinary() {
		//I would use String.split, but I'm worried about escaping regexes.  This seems easier.
    	String path = System.getProperty("java.class.path");
        String separator = System.getProperty("path.separator");
        
        int separatorIndex = path.indexOf(separator);
        
        while(path.length()>0) {
        	String candidate;
        	if(separatorIndex!=-1) {
        		candidate = path.substring(0,separatorIndex-1);
        		path = path.substring(separatorIndex+1);
        	} else {
        		candidate = path;
        		path = "";
        	}
        	 
        	if(candidate.endsWith(".jar")) return new File(candidate);
        	
        	separatorIndex = path.indexOf(separator);
        }
        
        return null;
	}
}
