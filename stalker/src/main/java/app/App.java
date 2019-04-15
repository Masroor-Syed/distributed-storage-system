/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package app;

import app.LeaderUtils.CRUDQueue;
import app.LeaderUtils.LeaderCheck;
import app.LeaderUtils.RequestAdministrator;
import app.chunk_util.Indexer;
import app.chunk_util.IndexFile;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

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
    private static boolean connected = false;
    private static  boolean running = false;

    public static void main(String[] args) {
        //debugging modes: 0 - none; 1 - message only; 2 - stack traces only; 3 - stack and message
        Debugger.setMode(3);

        //Debugger.toggleFileMode();
        //make sure the directories are available
        initStalker();
        ConfigManager.loadFromFile("config/config.cfg", "default", true);
        cfg = ConfigManager.getCurrent();
        loadConfig(cfg);
        ind = Indexer.loadFromFile();

        List<Thread> tohandle = new ArrayList<>();
        //start the election listener
        tohandle.add(new Thread(new ElectionListener()));
        //health listener listens for health checks
        tohandle.add(new Thread(new HealthListener()));
        //First thing to do is locate all other stalkers and print the stalkers to file
        //check the netDiscovery class to see where the file is being created
        tohandle.add(new Thread(new DiscoveryManager(Module.STALKER, disc_timeout, false)));
        //start the threads and make sure they are interrupted when the program exits
        ShutDown sd = new ShutDown(tohandle);
        Runtime.getRuntime().addShutdownHook(new ShutdownHandler(sd));



        Debugger.log("Stalker Main: This Stalker's macID: " + NetworkUtils.getMacID() + "\n\n", null);
        Debugger.log("Stalker Main: This IP: " + NetworkUtils.getIP() + "\n\n", null);

        //try and connect to the servers
        connectToServers();
        if (!running && !NetworkUtils.shouldShutDown()){
            LeaderCheck l = new LeaderCheck();
            l.election(0);
        }

        while (!NetworkUtils.shouldShutDown()){
            //reelect
            //starting task for health checks on STALKERS and HARM targets
            HashMap<Integer, String> stalkermap = NetworkUtils.getStalkerMap(cfg.getStalker_list_path());
            stalkermap.remove(leaderUuid);
            int role = ElectionUtils.identifyRole(NetworkUtils.mapToSList(stalkermap),leaderUuid);
            cfg.setRole(role);
            ConfigManager.saveToFile(cfg);
            if (role != 0 && !running){
                //we kind of assume we'll get an indexfile from the leader
                while (true){
                    wait(3000);
                    if(getConfirmation(leaderUuid)){
                        break;
                    }
                }
            }
            Thread healthChecker = new Thread( new HealthChecker(Module.STALKER, new AtomicLong(0), false));
            healthChecker.start();
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
                    //while no reelection is called
                    while(!ConfigManager.getCurrent().isReelection()){
                        //wait 10 seconds
                        wait(10000);
                    }
                    try{
                        t1.interrupt();
                        t2.interrupt();
                        t1.join();
                        t2.join();
                    }
                    catch(InterruptedException e){
                        Debugger.log("", e);
                    }
                    break;
                default:
                    Debugger.log("<<<<<<<-----Worker Online----->>>>>>>\n\n", null);
                    Thread jcpReq = new Thread(new JcpRequestHandler(ind));
                    jcpReq.start();
                    //while no reelection is called
                    while(!ConfigManager.getCurrent().isReelection()){
                        //wait 1 seconds
                        wait(1000);
                    }
                    //interrupt any workers
                    jcpReq.interrupt();
                    Debugger.log("Worker Interrupted", null);
                    try {
                        healthChecker.interrupt();
                        healthChecker.join();
                        jcpReq.join();
                    }
                    catch(InterruptedException e){
                        Debugger.log("", e);
                    }
                    break;
//                case 2:
//                    Debugger.log("<<<<<<<-----Vice Online----->>>>>>>\n\n", null);
//                    Thread vice = new Thread(new JcpRequestHandler(ind));
//                    vice.start();
//                    //while no reelection called
//                    while(!ConfigManager.getCurrent().isReelection()){
//                        //wait 10 seconds
//                        wait(1000);
//                    }
//                    try {
//                        //interrupt vice leader
//                        vice.interrupt();
//                        Debugger.log("Worker Interrupted", null);
//                        healthChecker.interrupt();
//                        healthChecker.join();
//                        vice.join();
//                    }
//                    catch(InterruptedException e){
//                        Debugger.log("", e);
//                    }
//                    break;
            }

            if (!NetworkUtils.shouldShutDown()){
                wait(3000);
                LeaderCheck leaderchecker = new LeaderCheck();
                leaderchecker.election(1);
                cfg = ConfigManager.getCurrent();
                leaderUuid = cfg.getLeader_id();
                cfg.setReelection(false);
                running = false;
                // Leader election by asking for a leader
            }

        }
        Debugger.log("Shudown of STALKER was successful!", null);
        NetworkUtils.wait(10000);

    }




    public static void connectToServers(){
        Debugger.log("Stalker Main: Discovering nodes on network...", null);
        List<Integer> stalkerList = null;
        Map<Integer, NodeAttribute> harmlist = null;
        int attempts = 0;
        boolean found[] = {false, false};
        //wait for at least 2 connections
        while (!connected && !NetworkUtils.shouldShutDown()){
            //we will wait for network discovery to do its thing
            wait((disc_timeout * 1000) + 1000);
            try{
                stalkerList = NetworkUtils.getStalkerList(cfg.getStalker_list_path());
                harmlist = NetworkUtils.getNodeMap(cfg.getHarm_list_path());
            }
            catch (Exception e){
                //Debugger.log("", null);
            }
            try{
                if (harmlist != null && !harmlist.isEmpty()){
                    if (!found[1]){
                        Debugger.log("Harms discovered on network!...", null);
                        found[1] = true;
                    }

                }
                else{
                    Debugger.log("Stalker Main: No HARM targets detected...", null);
                }
                LeaderCheck leaderchecker = new LeaderCheck();
                if (stalkerList != null && stalkerList.size() > 0){

                    if (!found[0]){
                        found[0] = true;
                        Debugger.log("Stalker Main: Stalkers have been discovered on the network!", null);
                        Debugger.log("Stalker Main: Waiting for a leader or election threshold to be met...", null);

                    }
                    if(leaderchecker.tryLeader()){
                        connected = true;
                        running = true;
                        cfg = ConfigManager.getCurrent();
                        ind = Indexer.loadFromFile();
                        Debugger.log("Leader uuid = " + cfg.getLeader_id(), null);
                    }
                }
                else{
                    Debugger.log("Stalker Main: No STALKERs detected yet...", null);
                }

                if (stalkerList != null && stalkerList.size() >= cfg.getElection_threshold_s()){

                    if(harmlist.size() >= 1){
                        connected = true;
                        Debugger.log("Threshold for initiation met...", null);
                    }

                }
                else{
                    Debugger.log("Stalker Main: Waiting for more servers to become available...", null);
                }
            }
            catch(NullPointerException e){
                Debugger.log("", e);
            }

            attempts++;
        }
        Debugger.log("Stalker Main: System discovery complete!", null);
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
        Map<Integer, String> stalkerMap = NetworkUtils.getStalkerMap(ConfigManager.getCurrent().getStalker_list_path());
        boolean success = false;
        while (!success) {
            try {
                Debugger.log("Stalker main: Asking leader for permission to start...", null);
                Debugger.log("leader: " + cfg.getLeader_id(), null);
                Socket leader = NetworkUtils.createConnection(stalkerMap.get(cfg.getLeader_id()), cfg.getLeader_report());

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
                Debugger.log("Stalker main: Attempt failed when connecting to leader...", null);
                wait(5000);
                Debugger.log("Stalker main: Trying again...", null);
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

