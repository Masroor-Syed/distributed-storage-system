/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package app;

import java.io.*;
import java.net.Socket;

public class App {

    public static void main(String[] args) {

        /*
        This is the main point of entry for JCP request
        The object is a singleton and will persist through the life of JCP
         */
        RequestSender requestSender = RequestSender.getInstance();
        Socket socket = requestSender.connect("127.0.0.1", 6555);

        // upload file request

        requestSender.sendFile("temp\\000_mp4_test.mp4");


        // should close socket from main calling method, otherwise threads giving null pointer exception
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
