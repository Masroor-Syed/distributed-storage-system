/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package app;

import app.health_utils.HealthStat;
import app.health_utils.IndexFile;
import app.health_utils.Indexer;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class App {

    public static void main(String[] args) {

        DiscoveryManager DM = new DiscoveryManager(Module.HARM, 35);
        DM.start();

        int macID = NetworkUtils.getMacID();
        CommsHandler commLink = new CommsHandler();
        //initialize socket and input stream
        Socket STALKER_Client = null;
        ServerSocket HARM_server = null;
        // we can change this later to increase or decrease
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            //initializing harm server
            HARM_server = new ServerSocket(22222);

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(NetworkUtils.timeStamp(1) + "Waiting...");
        // TODO -  Add Periodic Health Checks (call HealthStat.healthCheck(IndexFile) for a check up and HealthStat.status() for a report)
        // Test codes used for debugging:
//        IndexFile ind = new IndexFile();
//        System.out.println("Spot 1");
//        HealthStat health = new HealthStat();
//        System.out.println("Spot 2");
//        ind.add("ABC","1337");
//        System.out.println("Spot 3");
//        health.healthCheck(ind);
//        System.out.println("Spot 4");
//        health.status();
//
//
//        Runnable periodic_health_check = new Runnable(){
//            public void run(){
//                System.out.println("Commencing periodic health check.");
//                health.healthCheck(ind);
//            }
//        };

        ScheduledExecutorService exec = Executors.newScheduledThreadPool(4);
        exec.scheduleAtFixedRate(periodic_health_check, 15, 30, TimeUnit.SECONDS);

        // will keep on listening for requests from STALKERs





        while (true) {
            try {
                STALKER_Client = HARM_server.accept();
                System.out.println(NetworkUtils.timeStamp(1) + "Accepted connection : " + STALKER_Client);
                //get packet from the link and handle it
                TcpPacket STALKER_Request = commLink.receivePacket(STALKER_Client);
                Handler h = new Handler(STALKER_Client, STALKER_Request, macID);
                h.run();

                // creating a runnable task for each request from the same socket connection
                //probably not even necessary
                //not working right now
                //executorService.execute();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    // waiting until all thread tasks are done before closing the resources

                    awaitTerminationAfterShutdown(executorService);
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
}
