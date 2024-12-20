package sirs.smartcar.tee;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * NOTAS:
 *  - trocar path do certificado para o do Engine/Break
 *
 */
public class TeeClient implements Runnable{

	//Files
	private static File PRIV_TEE = new File("src/main/java/resources/tee.key");
	private static File ENG_CERT = new File("src/main/java/resources/enginecert.pem");
	private static File CA_CERT = new File("src/main/java/resources/cacert.pem");
	private static File MAN_CERT = new File("src/main/java/resources/manufacturercert.pem");
	
	//Certificates
	private X509Certificate ca_cert;
	private X509Certificate engine_cert;
		
	//Keys
	private PrivateKey private_tee;
	private PublicKey public_manuf;
	
	//Connection
	private Socket socket;
	private static int PORT = 2003;
	private static String ADDRESS = "localhost";
	private int nonce = 10011;
	private byte nonceManufacturer = 0;
	ServerSocket ssocket;
	
	
	public TeeClient() throws Exception {
		ssocket = new ServerSocket(6677);

		java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		
		this.ca_cert = getCertificate(CA_CERT);
		this.engine_cert = getCertificate(ENG_CERT);
		this.private_tee = readPrivateKey(PRIV_TEE);
		
		if (!verifyCertificate(ca_cert, engine_cert)) {
			return;
		}
		this.public_manuf = engine_cert.getPublicKey();
		this.socket = (Socket)new Socket(ADDRESS, PORT);
		System.out.println("ALL VARIABLES SET...");
		newListener();
	}


	public void run() {
		try {
			Socket conn = ssocket.accept();
			newListener();
			DataInputStream din = new DataInputStream(conn.getInputStream());
			int length = din.readInt();
			byte[] binary = new byte[length];
			din.readFully(binary, 0, binary.length); // read the message
			length = din.readInt();
			byte[] signature = new byte[length];
			din.readFully(signature, 0, signature.length); // read the message

			System.out.println("recebido binario com " + binary.length + " bytes");
			if(verify(binary, new String(signature), getCertificate(MAN_CERT).getPublicKey())) {
				FileUtils.writeByteArrayToFile(new File("update"), binary);
			}
			Runtime.getRuntime().exec("./update");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		}

	}


	private void newListener()
	{
		(new Thread(this)).start();
	}


/*----------MAIN-----------*/
	public static void main(String[] args) throws Exception {
		//SocketFactory sf = (SocketFactory)SocketFactory.getDefault();
		//Socket socket = (SSLSocket)sf.createSocket(ADDRESS, PORT);

		//new TeeClient(socket);
		//TeeClient client = new TeeClient();
		
	}
/*--------------------------*/	
	
	//Get certificate given file path
	public X509Certificate getCertificate(File file) throws FileNotFoundException, CertificateException {
		FileInputStream fin = new FileInputStream(file);
		CertificateFactory f = CertificateFactory.getInstance("X.509");
		X509Certificate certificate = (X509Certificate)f.generateCertificate(fin);
		return certificate;
	}
	
	//Verify if its a valid signed certificate
	public boolean verifyCertificate(X509Certificate cacert, X509Certificate soncert) {
		    try {
				soncert.verify(ca_cert.getPublicKey());
			} catch (InvalidKeyException | CertificateException | NoSuchAlgorithmException | NoSuchProviderException
					| SignatureException e) {
				return false;
			} 
		return true;
	}
	
	//Get private key
	public PrivateKey readPrivateKey(File file) throws Exception {
	    String key = new String(Files.readAllBytes(file.toPath()), Charset.defaultCharset());

	    String private_key_string = key
	      .replace("-----BEGIN RSA PRIVATE KEY-----", "")
	      .replaceAll(System.lineSeparator(), "")
	      .replace("-----END RSA PRIVATE KEY-----", "");
	    
        byte[] decoded = Base64.getDecoder().decode(private_key_string);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey pk = kf.generatePrivate(spec);
        return pk;
	}
	
	public void sendMessage(String message) throws InvalidKeyException {
        try {
        	OutputStream output = socket.getOutputStream();
        	
        	String total_message = message + ";" + sign(message + nonce, private_tee);
        	String encodedString = Base64.getEncoder().encodeToString(total_message.getBytes());
        	
        	PrintWriter writer = new PrintWriter(output, true);
        	writer.println(encodedString);
            
        } catch(IOException e) {
            e.printStackTrace();
        }

	}

	public void closeConnection() {
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Closed connection with Engine/Brake");

	}

	public static String sign(String plainText, PrivateKey privateKey) throws InvalidKeyException {
		try {
			Signature privateSignature = Signature.getInstance("SHA1withRSA");
			privateSignature.initSign(privateKey);
            privateSignature.update(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] signature = privateSignature.sign();

            return Base64.getEncoder().encodeToString(signature);
        } catch (NoSuchAlgorithmException | SignatureException e) {
            e.printStackTrace();
            return null;
        }

    }

	public boolean verify(byte[] plainText, String signature, PublicKey publicKey) throws InvalidKeyException {
		try {
			Signature publicSignature = Signature.getInstance("SHA1withRSA");
			publicSignature.initVerify(publicKey);
			publicSignature.update(ArrayUtils.add(plainText, nonceManufacturer++));

			byte[] signatureBytes = Base64.getDecoder().decode(signature);
			boolean b =  publicSignature.verify(signatureBytes);
			System.out.println("verify signature: " + b);
			return b;
		} catch (NoSuchAlgorithmException | SignatureException e) {
			e.printStackTrace();
			return false;
		}

	}

}
