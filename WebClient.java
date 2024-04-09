
/**
 * WebClient Class to connect to remote server.  Run with the ClientDriver
 * 
 * Example ways to run: Java ClientDriver https://www.pearsonhighered.com/assets/samplechapter/0/1/3/0/0130322202.pdf
 * 
 * CPSC 441
 * Assignment 2
 * 
 * @author 	Matteo Cusanelli
 * @version	2024
 *
 */

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.*;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class WebClient {

	private static final Logger logger = Logger.getLogger("WebClient"); // global logger

    /**
     * Default no-arg constructor
     */
	public WebClient() {
		// nothing to do!
	}
	
    /**
     * Downloads the object specified by the parameter url.
	 *
     * @param url	URL of the object to be downloaded. It is a fully qualified URL.
     */
	public void getObject(String url){
        // Parameters for the different parts of the URL
        int port = 0;
        String protocol;
        String pathname;
        String hostname;
        String filename;

        // Variables for the Streams and sockets
        Socket socket = null;
		OutputStream outputStream = null;
        FileOutputStream fileOutputStream = null;
		InputStream inputStream = null;
        SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket sslSocket = null;

        // Buffersize to receive file
        int BufferSize = 4096;

        // Splits the URL by : to separate the protocol from the rest of the URL and saves the output to an array
        String[] arrOfURL = url.split(":", 2);
        protocol = arrOfURL[0]; // get the protocol of the URL

        // Removes the // from the remaining part of the URL and splits the URL by / once separating the pathname from the host and port
        String RemainingURL = arrOfURL[1].substring(2);
        String[] arrOfRemaningURL = RemainingURL.split("/", 2);

        pathname = arrOfRemaningURL[1]; // Get the pathname of the URL

        boolean TestForPort = arrOfRemaningURL[0].contains(":"); // checks if the remaining part of the URL contains a port (has a :)

        if (!TestForPort){ // If there is no port
            hostname = arrOfRemaningURL[0]; // Get the Hostname of the URL
            if (protocol.equalsIgnoreCase("http")){
                port = 80;
            } else if (protocol.equalsIgnoreCase("https")){
                port = 443;
            }else{ // If the protocol is not http or https
                System.err.println("Error, protocol is not HTTP or HTTPS");
                System.exit(1);
            }
        } 
        
        else{ // If there is a port
            String[] arrOfHostPort = arrOfRemaningURL[0].split(":", 2); // Separate the port from the hostname
            hostname = arrOfHostPort[0]; // Get the Hostname of the URL

            try{ // Changes the port to an integer, catching an exception if it isnt
                port = Integer.valueOf(arrOfHostPort[1]);
            }catch (Exception e){
                System.err.println("Error, port is not an integer");
                System.exit(1);
            }
        }

        // Get the name of the file
        String[]arrOfPath = pathname.split("/", -1); // Split the file for every / and saves the output to an array
        filename = arrOfPath[arrOfPath.length - 1]; // Gets the filename which is the last index of the array

        try{

            /**
             * Sets the sockets of the file to either SSL socket or Socket depending on if the protocol is http or https
             */
            if (protocol.equalsIgnoreCase("http")){ // If protocol is HTTP
            
                // Setting streams for input and output to socket
                socket = new Socket(hostname, port);
                outputStream = socket.getOutputStream();
                inputStream = socket.getInputStream();
            }
            
            else if (protocol.equalsIgnoreCase("https")){ // If protocol is HTTPS

                // Setting streams for input and output to sslSocket
                sslSocket = (SSLSocket) sslsocketfactory.createSocket(hostname, port);
                outputStream = sslSocket.getOutputStream();
                inputStream = sslSocket.getInputStream();
            }

            /**
             * Sending the HTTP get request below
             */
            System.out.println("\nSENDING HTTP GET REQUEST");

            // Change the request lines from ASCII to bytes
            String Getrequest = ("GET /"+pathname + " HTTP/1.1\r\n");
            byte[] GetrequestBytes = Getrequest.getBytes("US-ASCII");

            String HostRequest = ("Host: " + hostname + "\r\n");
            byte[] GetHostBytes = HostRequest.getBytes("US-ASCII");

            String CloseRequest = ("Connection: close\r\n");
            byte[] CloseRequestByte = CloseRequest.getBytes("US-ASCII");

            String EndOfHead = ("\r\n");
            byte[] EndOfHeadByte = EndOfHead.getBytes("US-ASCII");

            // Send request lines to output stream
            outputStream.write(GetrequestBytes);
            outputStream.flush();
            System.out.println("GET /"+pathname + " HTTP/1.1"); // prints the request line minus the \r\n after sending

            outputStream.write(GetHostBytes);
            outputStream.flush();
            System.out.println("Host: " + hostname); // prints the request line minus the \r\n after sending

            outputStream.write(CloseRequestByte);
            outputStream.flush();
            System.out.println("Connection: close"); // prints the request line minus the \r\n after sending

            outputStream.write(EndOfHeadByte); // Write to the outputstream the end of byte header (\r\n)
            outputStream.flush();


            /**
             * Receiving the HTTP request below
             */
            System.out.println("\nRECEIVING HTTP RESPONSE");
            ArrayList<String> line = new ArrayList<String>(); // Create an empty arraylist for the characters from the headers

            // Initial variables 
            String CharResponse = "\r\n";
            boolean containsSequence = false;
            boolean EndOfHeader = false;

            /**
             * Loops throughout the input stream until the header ends
             */
            while(!EndOfHeader){ 
                int dataInt = inputStream.read(); // gets the first int from the inputstream
                byte dataByte = (byte) dataInt; // Converts the integer obtained into a byte
                byte[] dataArray = {dataByte}; // Saves the byte to an array of size 1
                String ByteChar = new String(dataArray); // Converts the array to a string
                line.add(ByteChar); // Add the string to the arraylist

                // Combines all the characters in the arraylist into one string
                String lineString = String.join("", line);

                // Checks if the string contans the sequence of characters  \r\n
                containsSequence = lineString.contains(CharResponse);

                if (containsSequence){ // If the line is a header line or a status line (contains \r\n)
                    if (line.size() == 2){ // If the line signals the end of the header lines
                        EndOfHeader = true;
                    }else{ // If line is not the end of the header lines

                        // Loop through list and remove the \r\n from the lines before printing
                        for (int i = 0; i < 2; i++){ 
                            int index = line.size() - 1;
                            line.remove(index);
                        }
                        lineString = String.join("", line); // Create a new line without the \r\n
                        System.out.println(lineString);
                        line.clear(); // Clear line arraylist
                    }
                }
            }


            // Create the new file to save the input from the server to
            fileOutputStream = new FileOutputStream(filename);

            // Create a buffer of bytes
            byte[] buffer = new byte[BufferSize];
            int count;
            
            // Loop through writing to the output file
            while ((count = inputStream.read(buffer)) > 0){
                fileOutputStream.write(buffer, 0, count);
                fileOutputStream.flush();
            }

            // Close streams as well as the socket
            outputStream.close();
            inputStream.close();
            if (protocol.equalsIgnoreCase("http")){ // If the protocol is http, close the socket
                socket.close();
            } else{ // If the protocol is https colose the sslSocket
                sslSocket.close();
            }
        }
        catch (IOException e) {
            System.out.println("IoException Error");
        } 
    }
}
