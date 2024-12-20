package org.example.controlunit;


import org.apache.commons.text.StringEscapeUtils;

import java.io.*;
import java.net.*;
import java.security.KeyStore;
import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import javax.naming.ldap.Control;
import javax.net.*;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

/*
 * ClassServer.java -- a simple file server that can serve
 * Http get request in both clear and secure channel
 */

public class ControlUnit implements Runnable {

    private ServerSocket server = null;
    private ServerSocket teeServer = null;
    private static final int DefaultServerPort = 65431;
    private int temperature;
    private boolean doorOpen;
    private int fuelLevel;
    private double[] tirePressure;
    /**
     * Constructs a ClassServer based on <b>ss</b> and
     * obtains a file's bytecodes using the method <b>getBytes</b>.
     *
     */
    protected ControlUnit(ServerSocket ss)
    {
        temperature = 16;
        fuelLevel = 100;
        doorOpen = false;
        server = ss;
        tirePressure = new double[]{2, 2, 2, 2};
        Timer t = new Timer();
        TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                fuelLevel--;
            };
        };
        t.schedule(tt, 0, 2000);

        Timer t2 = new Timer();
        TimerTask tt2 = new TimerTask() {
            @Override
            public void run() {
                for(int i = 0; i < tirePressure.length; i++) {
                    Random r = new Random();
                    tirePressure[i] = 1.5 + r.nextDouble() * 1.5;
                }
            };
        };
        t2.schedule(tt2, 0, 2000);
        newListener();
    }

    @Override
    public String toString() {
        return "ControlUnit{" +
                "temperature=" + temperature +
                ", doorOpen=" + doorOpen +
                ", fuelLevel=" + fuelLevel +
                ", tires=" + tirePressure[0] + "," + tirePressure[1] + "," + tirePressure[2] + "," + tirePressure[3] +
                '}';
    }

    /**
     * The "listen" thread that accepts a connection to the
     * server, parses the header to obtain the file name
     * and sends back the bytes for the file (or error
     * if the file is not found or the response was malformed).
     */
    public void run()
    {
        Socket socket;

        // accept a connection
        try {
            socket = server.accept();
        } catch (IOException e) {
            System.out.println("Class Server died: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // create a new thread to accept the next connection
        newListener();

        try {
            OutputStream rawOut = socket.getOutputStream();

            PrintWriter out = new PrintWriter(
                    new BufferedWriter(
                            new OutputStreamWriter(
                                    rawOut)));
            try {

                // request
                DataInputStream din = new DataInputStream(socket.getInputStream());
                String line;
                int length = din.readInt();
                if(length>0) {
                    byte[] message = new byte[length];
                    din.readFully(message, 0, message.length); // read the message
                    String cmd = new String(message);
                    System.out.println(cmd);
                    String[] words = cmd.split(" ");
                    switch(words[0]) {
                        case "get-ac":
                            int t = temperature;
                            out.print("" + temperature);
                            out.flush();
                            break;
                        case "set-ac":
                            if(words.length == 2) {
                                try {
                                    temperature = Integer.parseInt(words[1]);
                                    out.print("ok");
                                    out.flush();
                                }
                                catch (NumberFormatException e) {
                                    out.print("wrong number");
                                    out.flush();
                                }
                            }
                            break;
                        case "open-door":
                            doorOpen = true;
                            out.print("ok");
                            out.flush();
                            break;
                        case "close-door":
                            doorOpen = false;
                            out.print("ok");
                            out.flush();
                        case "get-fuel":
                            int f = fuelLevel;
                            out.print("" + f);
                            out.flush();
                            break;
                        case "get-tires":
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < tirePressure.length; i++) {
                                sb.append(tirePressure[i]);
                                sb.append(",");
                            }
                            System.out.println(sb.toString());
                            String tires = sb.toString();
                            out.print(tires);
                            out.flush();
                            break;
                        case "binary":
                            int size = din.readInt();
                            byte[] bytes = new byte[size];
                            din.readFully(bytes, 0, bytes.length); // read the binary
                            size = din.readInt();
                            byte[] signature = new byte[size];
                            din.readFully(signature, 0, signature.length); // read the binary
                            System.out.println(new String(signature));
                            System.out.println("file size: " + bytes.length);
                            Socket tees = new Socket("localhost", 6677);
                            DataOutputStream dout = new DataOutputStream(
                                    tees.getOutputStream());
                            dout.writeInt(bytes.length);
                            dout.write(bytes);
                            dout.writeInt(signature.length);
                            dout.write(signature);
                            dout.flush();
                            out.print("ok");
                            out.flush();
                            break;
                        default:
                            out.print("what");
                            out.flush();
                            break;
                    }
                }
                else {
                    out.print("what");
                    out.flush();
                }
            } catch (Exception e) {
                e.printStackTrace();
                // write out error response
                out.print("EXCEPTION");
                out.flush();
            }

        } catch (IOException ex) {
            // eat exception (could log error to log file, but
            // write out to stdout for now).
            System.out.println("error writing response: " + ex.getMessage());
            ex.printStackTrace();

        } finally {
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * Create a new thread to listen.
     */
    private void newListener()
    {
        (new Thread(this)).start();
    }

    /**
     * Returns the path to the file obtained from
     * parsing the HTML header.
     */

    public static void main(String args[])
    {
        int port = DefaultServerPort;


        try {
            ServerSocketFactory ssf = ControlUnit.getServerSocketFactory();
            assert ssf != null : "ServerSocketFactory is null";
            ServerSocket ss = ssf.createServerSocket(port);
            ((SSLServerSocket)ss).setNeedClientAuth(true);



            ControlUnit CU = new ControlUnit(ss);
            ControlEngine CE = new ControlEngine(new ServerSocket(2003));

            Timer t = new Timer();
            TimerTask tt = new TimerTask() {
                @Override
                public void run() {
                    System.out.println("-----------SYSTEM STATE--------");
                    System.out.println(CU);
                };
            };
            t.schedule(tt, 0, 5000);


        } catch (IOException e) {
            System.out.println("Unable to start ClassServer: " +
                    e.getMessage());
            e.printStackTrace();
        }
    }

    private static ServerSocketFactory getServerSocketFactory() {
            SSLServerSocketFactory ssf = null;
            try {
                // set up key manager to do server authentication
                SSLContext ctx;
                KeyManagerFactory kmf;
                KeyStore ks;
                char[] passphrase = "sirs".toCharArray();

                ctx = SSLContext.getInstance("TLS");
                kmf = KeyManagerFactory.getInstance("SunX509");
                ks = KeyStore.getInstance("JKS");

                ks.load(new FileInputStream("src/main/resources/controlunit.p12"), passphrase);
                kmf.init(ks, passphrase);
                ctx.init(kmf.getKeyManagers(), null, null);

                ssf = ctx.getServerSocketFactory();
                return ssf;
            } catch (Exception e) {
                e.printStackTrace();
            }
        return null;
    }
}