/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package app;

import app.LeaderUtils.CRUDQueue;
import app.LeaderUtils.RequestAdministrator;
import app.chunk_utils.Indexer;
import app.chunk_utils.IndexFile;
import sun.nio.ch.Net;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class App {


    private static int leaderUuid = -1;
    private static volatile IndexFile ind;
    public int getLeaderUuid()
    {
        return leaderUuid;
    }
    private static String stalker_path;
    private static String harm_path;
    private static int disc_timeout;
    private static int debug_mode = 3;
    private static ConfigFile cfg;

    public static void main(String[] args) {
        //debugging modes: 0 - none; 1 - message only; 2 - stack traces only; 3 - stack and
        Debugger.setMode(3);
        Debugger.toggleFileMode();
        initStalker();

        ConfigManager.loadFromFile("config/config.cfg", "default", true);
        cfg = ConfigManager.getCurrent();
        loadConfig(cfg);
        ind = Indexer.loadFromFile();

        //starting listener thread for health check and leader election
        Thread listenerForHealth = new Thread( new ListenerThread(ind));
        listenerForHealth.start();

        //First thing to do is locate all other stalkers and print the stalkers to file
        //check the netDiscovery class to see where the file is being created
        Thread discManager = new Thread(new DiscoveryManager(Module.STALKER, disc_timeout, false));
        discManager.start();
        boolean connected = false;
        Debugger.log("Stalker Main: This Stalker's macID: " + NetworkUtils.getMacID() + "\n\n", null);
        Debugger.log("Stalker Main: Discovering nodes on network...", null);

        List<Integer> stalkerList = null;
        Map<Integer, NodeAttribute> harmlist = null;
        int attempts = 0;

        //wait for at least 2 connections
        while (!connected){
            //we will wait for network discovery to do its thing
            wait((disc_timeout * 1000) + 5000);
            stalkerList = NetworkUtils.getStalkerList(cfg.getStalker_list_path());
            harmlist = NetworkUtils.getNodeMap(cfg.getHarm_list_path());
            try{
                if (harmlist != null && !harmlist.isEmpty()){
                }
                else{
                    Debugger.log("Stalker Main: No HARM targets detected...", null);
                }

                if (stalkerList != null && stalkerList.size() >= 3){
                    connected = true;
                }
                else{
                    Debugger.log("Stalker Main: No STALKERs detected yet...", null);
                    Debugger.log("Stalker Main: Waiting for servers to become available...", null);
                }
            }
            catch(NullPointerException e){
                Debugger.log("", e);
            }
            attempts++;
        }
        Debugger.log("Stalker Main: System discovery complete!", null);
        int test = 0;
        //starting task for health checks on STALKERS and HARM targets
        Thread healthChecker = new Thread(new HealthChecker(Module.STALKER, null, false));
        healthChecker.start();
        //election based on networkDiscovery results
        //main loop
        while (true){
            // Leader election by asking for a leader
            //LeaderCheck leaderchecker = new LeaderCheck();
            //leaderchecker.election();
            //leaderUuid = LeaderCheck.getLeaderUuid();

            int role = ElectionUtils.identifyRole(stalkerList,leaderUuid);
            if (role != 0){
                //we kind of assume we'll get an indexfile from the leader
                getConfirmation(leaderUuid);
                cfg.setLeader_id(leaderUuid);
            }
            switch (role){
                case 0:
                    Debugger.log("<<<<<<<-----Leader Online----->>>>>>> \n\n", null);
                    //This means that this STK is the leader
                    //create a priority comparator for the Priority queue
                    CRUDQueue syncQueue = new CRUDQueue();
                    Thread t1 = new Thread(new StalkerRequestHandler(syncQueue, ind));
                    Thread t2 =  new Thread(new RequestAdministrator(syncQueue));
                    t1.start();
                    t2.start();
                    try{
                        t1.join();
                        t2.join();
                    }
                    catch(InterruptedException e){
                        Debugger.log("", e);
                    }
                    break;
                case 1:
                    Debugger.log("<<<<<<<-----Worker Online----->>>>>>>\n\n", null);
                    Thread jcpReq = new Thread(new JcpRequestHandler(ind));
//                JcpRequestHandler jcpRequestHandler = new JcpRequestHandler(ind);
//                jcpRequestHandler.run();
                    jcpReq.start();
                    try {
                        jcpReq.join();
                    }
                    catch(InterruptedException e){
                        Debugger.log("", e);
                    }
                    break;
                case 2:
                    Debugger.log("<<<<<<<-----Vice Leader Online----->>>>>>>\n\n", null);
                    Thread vice = new Thread(new JcpRequestHandler(ind));
//                JcpRequestHandler jcpRequestHandler = new JcpRequestHandler(ind);
//                jcpRequestHandler.run();
                    vice.start();
                    try {
                        vice.join();
                    }
                    catch(InterruptedException e){
                        Debugger.log("", e);
                    }
                    break;
            }
        }

    }

    public static void loadConfig(ConfigFile cfg){
        disc_timeout = cfg.getStalker_update_freq();
        debug_mode = cfg.getDebug_mode();
        harm_path = cfg.getHarm_list_path();
        stalker_path = cfg.getStalker_list_path();
        Indexer.init(cfg.getIndex_file_path());
        NetworkUtils.loadConfig(cfg);

    }

    public static void initStalker(){
        List<File> directories = new ArrayList<>();
        directories.add(new File("temp"));
        directories.add(new File("logs"));
        directories.add(new File("index"));
        directories.add(new File("config"));
        directories.add(new File("index/index_file"));
        directories.add(new File("index/lists"));
        directories.add(new File("temp/chunks"));
        directories.add(new File("temp/toChunk"));
        directories.add(new File("temp/reassembled"));
        NetworkUtils.initDirs(directories, true, 5);
    }
    //will block worker from doing anythin until the leader is confirmed
    public static boolean getConfirmation(int uuid) {
        CommsHandler commLink = new CommsHandler();
        boolean success = false;
        while (!success) {
            try {
                Socket leader = NetworkUtils.createConnection(NetworkUtils.getStalkerMap(stalker_path).get(uuid), cfg.getLeader_report());
                if (leader != null) {
                    //get confirmation from leader
                    if (commLink.sendPacket(leader, MessageType.CONFIRM, "", true) == MessageType.CONFIRM) {
                        //get the indexfile from the leader
                        IndexFile temp = Indexer.fromString(commLink.receivePacket(leader).getMessage());
                        if (temp != null){
                            ind = temp;
                            Indexer.saveToFile(ind);
                            Debugger.log("Stalker Main: IndexFile synced with leader.", null);
                            Debugger.log("Stalker Main: Leader has granted permission to start!", null);
                            leader.close();
                            success = true;
                            return true;
                        }
                        else{
                            return false;
                        }
                    }
                } else {
                    wait(5000);
                }
            } catch (IOException e) {
                wait(5000);
                Debugger.log("", e);
            }

        }
        return true;
    }

    public static void wait(int millis){
        try{
            //wait for a bit
            Thread.sleep((long)((millis)));
        }
        catch (InterruptedException ex){
            Debugger.log("", ex);
        }
    }



}

