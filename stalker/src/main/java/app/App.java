/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package app;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;

public class App {

    public static void main(String[] args) {

        JcpRequestHandler jcpRequestHandler = new JcpRequestHandler();
        jcpRequestHandler.run();
    }

}


