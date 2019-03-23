/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package app;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.*;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class App {

    public static void main(String[] args) {
        int macID = NetworkUtils.getMacID();
        System.out.println("" + macID);

        CommsHandler commLink = new CommsHandler();
        boolean cont = false;
        if (!cont){
            return;
        }
        //initialize socket and input stream
        Socket socket = null;
        ServerSocket server = null;
        DataInputStream in = null;
        DataOutputStream out = null;

        // we can change this later to increase or decrease
        ExecutorService executorService = Executors.newFixedThreadPool(10);


        try {
            //initializing harm server  // add a modifier from the args
            //currently only supports modifiers 0 - 4 SOOOOORRy
            //in the future the port will stay constant and IP will change
            server = new ServerSocket(22222);

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Waiting...");

        // will keep on listening for requests from STALKERs
        while (true) {

            try {
                socket = server.accept();
                System.out.println("Accepted connection : " + socket);


                TcpPacket packet = commLink.recievePacket(socket);


                System.out.println(socket.isClosed());
                Handler h = new Handler(socket, packet, macID);
                h.run();
                // creating a runnable task for each request from the same socket connection
                //executorService.execute();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {

                try {

                    // waiting until all thread tasks are done before closing the resources
                    awaitTerminationAfterShutdown(executorService);
                    //WARNING: Closing the in and out put stream also closes the socket, therefore can't do it here
//                    in.close();
//                    out.close();
//                    socket.close();   // closing the socket from here, may be should be closed from STALKER after request is completed?
                } catch (Exception i) {
                    i.printStackTrace();
                }
            }

        }
    }

    /**
     * Method to wait until all threads are done
     * @param threadPool
     */
    public static void awaitTerminationAfterShutdown(ExecutorService threadPool) {
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException ex) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

    }
    /**
     *This execute handshake between STALKER and HARM target
     *
     * @param in DatainputStream
     * @param out DataoutputStream
     * @return Optional<TcpPacket> that has been received from STALKER
     * @throws IOException
     */
    private static Optional<TcpPacket> executeHandshake(DataInputStream in, DataOutputStream out) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Optional<TcpPacket> receivedPacket = Optional.empty();

        try {
            String rec = in.readUTF();
            receivedPacket = Optional.of(mapper.readValue(rec, TcpPacket.class));

        } catch (EOFException e) {
        }

        //TO:Do need actual logic here if the HARM server is busy or available depending on the type of Request

        TcpPacket sendAvail = new TcpPacket(MessageType.ACK, "AVAIL");

        //writing as json string
        String jsonInString = mapper.writeValueAsString(sendAvail);
        System.out.println(jsonInString);
        out.writeUTF(jsonInString);


        return receivedPacket;
    }
}
