/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package app;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class App {
    //jcp main
    public static void main(String[] args) {

        /*
        try {
            Thread broadcaster = new Thread(new NetDiscovery(Module.STALKER,Module.STALKER));
            broadcaster.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        */

        /*
        This is the main point of entry for JCP request
        The object is a singleton and will persist through the life of JCP
         */

        String config_file = "config/stalkers.list";
        List<String> s_list = NetworkUtils.listFromJson(NetworkUtils.fileToString(config_file));

        RequestSender requestSender = RequestSender.getInstance();
        //ip of stalker
        String stalkerip = s_list.get(0);
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
