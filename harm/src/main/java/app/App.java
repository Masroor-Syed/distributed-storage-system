/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package app;


import app.health_utils.HealthStat;
import app.health_utils.HashIndexer;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class App {

    private static ConfigFile cfg;
    public static void main(String[] args) {
        //debugging modes: 0 - none; 1 - message only; 2 - stack traces only; 3 - stack and message
        Debugger.setMode(3);
        Debugger.toggleFileMode();
        initHarm();
        ConfigManager.loadFromFile("config/config.cfg", "default", true);
        cfg = ConfigManager.getCurrent();
        NetworkUtils.loadConfig(cfg);
        Debugger.setMode(cfg.getDebug_mode());


        List<Thread> toHandle = new ArrayList<>();
        //this will always be running
        //discovery manager
        toHandle.add(new Thread(new DiscoveryManager(Module.HARM, cfg.getHarm_update_freq(), true)));
        //harm listener health thread
        toHandle.add(new Thread(new ListenerThread(true)));
        //Add threads to shutdown handler (will start the threads)
        ShutDown shutDownThread = new ShutDown(toHandle);
        Runtime.getRuntime().addShutdownHook(new ShutdownHandler(shutDownThread));


        // Let's cook some magic
        // Hiring security guards
        Runnable periodic_health_check = new Runnable(){
            public void run(){
                // tell the guards to patrol
                Debugger.log("Commencing periodic self diagnosis.",null);
                HealthStat.getInstance().healthCheck(HashIndexer.loadFromFile());
                // tell them to write reports
                Debugger.log("Health status post check:", null);
                Debugger.log(HealthStat.getInstance().status(),null);
            }
        };
        ScheduledExecutorService exec = Executors.newScheduledThreadPool(4);
        // bug them every 30s
        exec.scheduleAtFixedRate(periodic_health_check, 15, 30, TimeUnit.SECONDS);

        int macID = NetworkUtils.getMacID();
        CommsHandler commLink = new CommsHandler();
        //initialize socket and input stream
        Socket STALKER_Client = null;
        ServerSocket HARM_server = null;
        // we can change this later to increase or decrease
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            //initializing harm server
            HARM_server = new ServerSocket(cfg.getHarm_listen());
        } catch (IOException e) {
            Debugger.log("", e);
        }
        Debugger.log("Harm Main: Waiting for requests...", null);


        System.out.println(NetworkUtils.timeStamp(1) + "Waiting...");

        while (!NetworkUtils.shouldShutDown()) {
            try {
                STALKER_Client = HARM_server.accept();
                Debugger.log("Harm Main: Accepted connection : ", null);
                //get packet from the link and handle it
                TcpPacket STALKER_Request = commLink.receivePacket(STALKER_Client);
               // Handler h = new Handler(STALKER_Client, STALKER_Request, macID);
               // h.run();

               Thread t =  new Thread(new Handler(STALKER_Client, STALKER_Request, macID));
               t.start();
//                executorService.submit();
//                // creating a runnable task for each request from the same socket connection
//                //probably not even necessary
//                //not working right now
//                //executorService.execute();
            } catch (FileNotFoundException e) {
                Debugger.log("", e);
            } catch (IOException e) {
                Debugger.log("", e);
            } finally {
                try {
                    // waiting until all thread tasks are done before closing the resources

                    awaitTerminationAfterShutdown(executorService);
                } catch (Exception i) {
                    Debugger.log("", i);
                }
            }
        }
        Debugger.log("Harm Exited safely.", null);

    }

    public static void initHarm(){
        List<File> dirs = new ArrayList();
        dirs.add(new File("storage"));
        dirs.add(new File("config"));
        dirs.add(new File("logs"));
        dirs.add(new File("temp"));
        NetworkUtils.initDirs(dirs, false, 1);
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
            Debugger.log("", ex);
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

    }
}
