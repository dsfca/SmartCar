package org.example.manufacturer;

import java.net.*;
import java.io.*;
import javax.net.ssl.*;
import javax.security.cert.X509Certificate;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.logging.Logger;

/*
 * This example shows how to set up a key manager to do client
 * authentication if required by server.
 *
 * This program assumes that the client is not inside a firewall.
 * The application can be modified to connect to a server outside
 * the firewall by following SSLSocketClientWithTunneling.java.
 */
public class SSLSocketClientWithClientAuth {

    private String host;
    private int port;
    SSLSocketFactory factory;
    SSLContext ctx;
    KeyManagerFactory kmf;
    KeyStore ks;
    SSLSocket socket;


    public SSLSocketClientWithClientAuth(String host, int port) {
        this.host = host;
        this.port = port;
        char[] passphrase = "sirs".toCharArray();
        try {
            ctx = SSLContext.getInstance("TLS");
            kmf = KeyManagerFactory.getInstance("SunX509");
            ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream("manufacturer.p12"), passphrase);
            kmf.init(ks, passphrase);
            ctx.init(kmf.getKeyManagers(), null, null);

            factory = ctx.getSocketFactory();
            socket = (SSLSocket) factory.createSocket(this.host, this.port);
            socket.startHandshake();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(byte[] message) {

        if(socket == null) {
            Logger logger = Logger.getLogger(getClass().getName());
            logger.severe("NO SOCKET");
        }
        try {
            //socket.addHandshakeCompletedListener();
            DataOutputStream dout = new DataOutputStream(
                                    socket.getOutputStream());
            dout.writeInt(message.length);
            dout.write(message);
            dout.flush();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String receiveMessage() {

        /* read response */
        BufferedReader in = null;
        try {
            in = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream()));

            String inputLine;
            StringBuilder sb = new StringBuilder();
            Logger logger = Logger.getLogger(getClass().getName());
            logger.severe("VOU RECEBER");
            while ((inputLine = in.readLine()) != null) {

                sb.append(inputLine);
                logger.severe(inputLine);
            }
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        SSLSocketClientWithClientAuth sss = new SSLSocketClientWithClientAuth("host.docker.internal", 65431);
        sss.sendMessage("ola".getBytes(StandardCharsets.UTF_8));
        System.out.println(sss.receiveMessage());
    }
}