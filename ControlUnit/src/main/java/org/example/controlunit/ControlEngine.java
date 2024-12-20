package org.example.controlunit;


import org.apache.commons.text.StringEscapeUtils;

import java.io.*;
import java.net.*;
import java.security.KeyStore;
import java.util.*;
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

public class ControlEngine implements Runnable {

    private ServerSocket server = null;
    private static final int DefaultServerPort = 2003;
    /**
     * Constructs a ClassServer based on <b>ss</b> and
     * obtains a file's bytecodes using the method <b>getBytes</b>.
     *
     */
    protected ControlEngine(ServerSocket ss)
    {
        server = ss;
        newListener();
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
               // request
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream()));
            String inputLine;
            while((inputLine = in.readLine()) != null) {
                System.out.println("inicio");
                Socket s = new Socket("localhost", 2004);
                OutputStream output = s.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);
                writer.println(inputLine);
                System.out.println(inputLine);
                System.out.println("fim");
            }
        } catch (Exception e) {
            e.printStackTrace();
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
}