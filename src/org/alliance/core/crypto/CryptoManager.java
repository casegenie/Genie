package org.alliance.core.crypto;

import org.alliance.core.CoreSubsystem;
import org.alliance.core.comm.networklayers.tcpnio.TCPNIONetworkLayer;
import org.alliance.core.comm.Connection;
import org.alliance.core.crypto.cryptolayers.SSLCryptoLayer;
import org.alliance.core.crypto.cryptolayers.TranslationCryptoLayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.*;
import java.util.HashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-aug-22
 * Time: 19:30:23
 * To change this template use File | Settings | File Templates.
 */
public class CryptoManager {
	/* Okay, so this is how the encryption scheme works:
	 * Let us assume Alice is running an alliance server and Bob want to be invited
	 * The conversation would look something like:
	 * 1) Alice creates invite code, sends it to Bob
	 * 2) Bob tries to connect to Alice with the code, he sends his RSA public key at 
	 *    the same time. 
	 * 3) The code is the "authentication" Alice needs to know that she should talk to Bob
	 * 4) Alice generates an AES private key, encodes it with Bob's RSA public key, and sends
	 *    it to Bob 
	 * 5) Bob decrypts the AES private key and now uses that key for all communication with Alice, Alice does the same but with Bob. **
	 * --RSA keys are persistent
	 * --AES keys are only persistent for a single session 
	 * Repeat sessions would skip part 1
	*/
	private Key RSAPrivateKey;
	private Key RSAPublicKey;
	private HashMap<Connection,SecretKey> AESKeys = new HashMap<Connection,SecretKey>();
	private CryptoLayer cryptoLayer;
    private CoreSubsystem core;

    public CryptoManager(CoreSubsystem core) throws Exception {
    	//Look to filesystem for keys
    	this.core = core;
    	getKeys(false);
    	if(T.t){
    		T.info("Found me a nice private and public key :-D");
    		//T.info(RSAprivateKey); On second thought this is probably a bad idea ...
    		T.info("Public key for all to see:\n "+RSAPublicKey);
    	}
    	//Done with crypto modifications
        switch (core.getSettings().getInternal().getEncryption()) {
            case 1:
                if (T.t) {
                    T.info("Launching SSL cryptolayer");
                }
                this.cryptoLayer = new SSLCryptoLayer(core);
                break;
            default:
                if (T.t) {
                    T.info("Launching translation cryptolayer");
                }
                this.cryptoLayer = new TranslationCryptoLayer(core);
                break;
        }

    }

//    private static String hexDump(byte[] buffer) {
//        StringBuilder builder=new StringBuilder();
//        for(byte b : buffer) {
//                builder.append(String.format("%02X ", b));
//        }
//        return builder.toString();
//    }
//    
    public CryptoLayer getCryptoLayer() {
        return cryptoLayer;
    }
    
    public void init() throws IOException, Exception {
        TCPNIONetworkLayer networkLayer = core.getNetworkManager().getNetworkLayer();
        cryptoLayer.setNetworkLayer(networkLayer);
        cryptoLayer.init();
    }
    
    //Tell me what Key I am using to talk to c
    //Note that if I don't have a key, I make one and store it
    public SecretKey requestAESPrivateKey(Connection c) {
    	if(!(AESKeys.containsKey(c))) {
    		if(T.t){
    			T.info("Generating a key for: "+ c);
    		}
    		KeyGenerator gen = null;
    		try {
    			gen = KeyGenerator.getInstance("AES");
    		} catch (NoSuchAlgorithmException e) {
    			System.err.println("Can't find the AES algorithm, something is wrong");
    		}
    		gen.init(128);
    		SecretKey skey = gen.generateKey();
    		AESKeys.put(c,skey);
    	}
    	return AESKeys.get(c);
    }
        
    //Used to change the key I use to talk to Connection c
    //Pass in null for the key if you want to remove the connection from the HashMap
    public void changeAESPrivateKey(Connection c, SecretKey key){
    	if(key == null){
    		if(T.t) {
    			T.info("Removing key for connection: "+c);
    		}
    		AESKeys.remove(c);
    	}
    	else {
    		if(T.t) {
    			T.info("Changing key for connection: "+c);
    		}
    		AESKeys.put(c,key);
    	}
    }
    
    //Uses our private RSA key to decrypt an offered AES key
    //After decrypting we add it to our HashMap
    public void decryptAESKey(Connection c, byte[] enc) throws Exception {
    	Cipher decrypt = Cipher.getInstance("RSA");
    	decrypt.init(Cipher.DECRYPT_MODE, RSAPrivateKey);
    	byte[] data = decrypt.doFinal(enc);
    	SecretKeySpec spec = new SecretKeySpec(data,"AES");
    	changeAESPrivateKey(c,(SecretKey)spec);
    }
    
    //Uses of public RSA key to encrypt some data (presumably an AES key)
    public byte[] getEncrypted(byte[] data) throws Exception {
    	Cipher encrypt = Cipher.getInstance("RSA");
    	encrypt.init(Cipher.ENCRYPT_MODE, RSAPublicKey);
    	byte[] enc = encrypt.doFinal(data);
    	return enc;
    }
    
    //forceGen==1 -> reset the keys, forceGen==0 -> use existing keys 
    private void getKeys(boolean forceGen){
    	String privateFile = core.getSettings().getInternal().getPrivatekeyfile();
    	String publicFile = core.getSettings().getInternal().getPublickeyfile();
    	File priv = new File(privateFile);
    	File pub = new File(publicFile);
    	if(priv.exists() && pub.exists() && !(forceGen)) {
    		try {
				this.RSAPrivateKey = getKeyFromFile(privateFile);
	    		this.RSAPublicKey = getKeyFromFile(publicFile);
			} catch (IOException e) {
				System.err.println("FAILED to read RSA keys from disk.");
				e.printStackTrace();
			}
    	}
    	else {
    		KeyPairGenerator gen = null;
			try {
				gen = KeyPairGenerator.getInstance("RSA");
			} catch (NoSuchAlgorithmException e) {
				System.err.println("Something is seriously wrong, can't find RSA algorithm");
				e.printStackTrace();
			}
			gen.initialize(2048); //This leads to slightly slower decryption, but I'm okay with that
    		KeyPair kp = gen.genKeyPair();
    		this.RSAPrivateKey = kp.getPrivate();
    		this.RSAPublicKey = kp.getPublic();
    		try {
				saveKeyToFile(privateFile, this.RSAPrivateKey);
	    		saveKeyToFile(publicFile, this.RSAPublicKey);
			} catch (IOException e) {
				System.err.println("FAILED to write RSA keys to disk.");
				e.printStackTrace();
			}
    	}
    }
    
    private void saveKeyToFile(String fileName, Key key) throws IOException {
    	ObjectOutputStream oout = new ObjectOutputStream(new FileOutputStream(fileName));
    	try {
    		oout.writeObject(key);
    	} catch (Exception e) {
    		throw new IOException("Error trying to write RSA key to disk", e);
    	} finally {
    		oout.close();
    	}
    }
    
    private Key getKeyFromFile(String fileName) throws IOException {
    	ObjectInputStream oin = new ObjectInputStream(new FileInputStream(fileName));
    	Key in;
    	try {
    		in = (Key)(oin.readObject());
    	} catch (Exception e) {
    		throw new IOException("Error trying to read RSA key to disk", e);
    	} finally {
    		oin.close();
    	}
		return in;
    }
    
}
