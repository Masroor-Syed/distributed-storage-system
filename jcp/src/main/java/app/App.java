/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package app;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {

    // to capture total disk space in the system and will be updated with each health check
    public volatile int TotalDiskSpace = 0 ;
    //jcp main
    public static void main(String[] args) {
        int test  = 0;

        int discoveryTimeout = 20;

        System.out.println(NetworkUtils.timeStamp(1) + "JCP online");
        //make a discovery manager and start it, prints results to file
        //this beast will be running at all times
        Thread discManager = new Thread(new DiscoveryManager(Module.JCP, discoveryTimeout, false));
        discManager.start();
        System.out.println(NetworkUtils.timeStamp(1) + "Waiting for stalker list to update");
        try{
            Thread.sleep(5000);
        }
        catch (InterruptedException e){
            e.printStackTrace();
        }
        System.out.println(NetworkUtils.timeStamp(1) + "List updated!");


        //get the stalkers from file
        HashMap<Integer, String> m =  NetworkUtils.mapFromJson(NetworkUtils.fileToString("config/stalkers.list"));
        //get sorted list from targets
        List<Integer> s_list = NetworkUtils.mapToSList(m);

        //HealthChecker checker = new HealthChecker();
        //checker.start(m);

        System.out.println(" Ip ids" + (s_list));
//        if (test == 0){
//            return;
//        }
        for (Integer key : m.keySet()){

        }
        RequestSender requestSender = RequestSender.getInstance();
        //ip of stalker we'll just use the one at index 1 for now
        String i =  m.get(s_list.get(0));
        String stalkerip =  m.get(s_list.get(1));

        //port to connect to
        int port = 11111;
        Socket socket = requestSender.connect(stalkerip, port);
        String req = "upload";
        switch (req){
            case("upload"):
                requestSender.sendFile("temp\\003_txt_test.txt");
                break;
            case("download"):
                requestSender.getFile("temp\\003_txt_test.txt");
                break;
        }
        // should close socket from main calling method, otherwise threads giving null pointer exception
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


//
//    //load a config (stalker ip) from file while we get network discovery working

}
