/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package app;

import java.io.*;

public class App {

    public static void main(String[] args) {

        //
        RequestSender requestSender = RequestSender.getInstance();
		
		//File file = new File("test.txt");
		//String path = file.getParent();

        requestSender.sendFile("test.txt");

    }
}
