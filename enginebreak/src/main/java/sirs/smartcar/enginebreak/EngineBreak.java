package sirs.smartcar.enginebreak;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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

public class EngineBreak {
	
		//Files
		private static File PRIV_ENG = new File("src/main/java/resources/engine.key");
		private static File ENG_CERT = new File("src/main/java/resources/enginecert.pem");
		private static File CA_CERT = new File("src/main/java/resources/cacert.pem");
		private static File TEE_CERT = new File("src/main/java/resources/teecert.pem");
		
		//Certificates
		private X509Certificate ca_cert;
		private X509Certificate engine_cert;
		private X509Certificate tee_cert;
			
		//Keys
		private PublicKey public_tee;
		private PrivateKey private_engine;
		
		//Connection
		private ServerSocket socket;
		private static int PORT = 2004;
		private static String ADDRESS = "localhost";
		private int nonce = 10011;
		

	public EngineBreak() throws Exception {
		java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		this.tee_cert = getCertificate(TEE_CERT);
		this.ca_cert = getCertificate(CA_CERT);
		this.engine_cert = getCertificate(ENG_CERT);
		this.private_engine = readPrivateKey(PRIV_ENG);
		if (!verifyCertificate(ca_cert, tee_cert)) {
			return;
		}
		this.public_tee = tee_cert.getPublicKey();
		
		receiveMessage();
	}
	
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
			e.printStackTrace();
			return false;
		} 
		return true;
	}


	public void receiveMessage() throws InvalidKeyException, IOException {
		this.socket = (ServerSocket)new ServerSocket(PORT);
		Socket socket_temp;
		System.out.println("Server listening...");
		socket_temp = this.socket.accept();
        try {
        	InputStream input = socket_temp.getInputStream();
        	BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        	
        	while(true) {
        		//System.out.println(msg);
        		String line = reader.readLine();
        		String decodedString = new String(Base64.getDecoder().decode(line));
        		String received = decodedString.substring(0, decodedString.indexOf(";")) + nonce;
        		String received_tovalidate = decodedString.substring(decodedString.indexOf(";") + 1);
        		if(verify(received, received_tovalidate, public_tee)) {
        			System.out.println("MESSAGE: " + received.substring(0, 5) + " RECEIVED SUCCESSFULLY");
        		}
        	}
        		
        	//FALTA INCLUIR MENSAGEM DE CONFIRMACAO

        } catch(IOException e) {
        	e.printStackTrace();
        	//return null;
        }

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
	
	public static boolean verify(String plainText, String signature, PublicKey publicKey) throws InvalidKeyException {
        try {
            Signature publicSignature = Signature.getInstance("SHA1withRSA");
            publicSignature.initVerify(publicKey);
            publicSignature.update(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            boolean b =  publicSignature.verify(signatureBytes);
            //System.out.println("verify signature: " + b);
            return b;
        } catch (NoSuchAlgorithmException | SignatureException e) {
            e.printStackTrace();
            return false;
        }

    }
	
	public static void main(String[] args) throws Exception {
		EngineBreak cu = new EngineBreak();
	}

}
