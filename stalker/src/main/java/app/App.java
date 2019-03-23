/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package app;

import app.LeaderUtils.QueueEntry;
import app.chunk_utils.Indexer;
import app.chunk_utils.IndexFile;
import org.apache.commons.io.FilenameUtils;
import java.io.*;
import java.util.PriorityQueue;
import java.util.Comparator;

public class App {

    public static void main(String[] args) {
        /*
        try {
            Thread listener = new Thread(new DiscoveryReply(Module.STALKER));
            listener.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        */


        int test = 0;
        initStalker();
        IndexFile ind = Indexer.loadFromFile();
        ind.summary();
        System.out.println("Stalker Online");

        //testing
        Tester tester = new Tester();
        tester.test();



        //networkDiscovery
        int role = getRole();

        if (test == 0){
            return;
        }
        switch (role){
            case 0:
                //create a priority comparator for the queue
                Comparator<QueueEntry> entryPriorityComparator = new Comparator<QueueEntry>() {
                    @Override
                    public int compare(QueueEntry q1, QueueEntry q2) {
                        return q1.getPriority() - q2.getPriority();
                    }
                };
                PriorityQueue<QueueEntry> syncQueue = new PriorityQueue<>(entryPriorityComparator);
                StalkerRequestHandler stalkerCoordinator = new StalkerRequestHandler(syncQueue);
                stalkerCoordinator.run();
                break;
            case 1:
                JcpRequestHandler jcpRequestHandler = new JcpRequestHandler(ind);
                jcpRequestHandler.run();
                break;
            case 2:
                break;
        }

    }

    public static int getRole(){
        return(1);
    }

    public static void initStalker(){
        //clear chunk folder
        File chunk_folder = new File("temp/chunks/");
        File[] chunk_folder_contents = chunk_folder.listFiles();

        File temp_folder = new File("temp/toChunk/");
        File[] temp_folder_contents = temp_folder.listFiles();

        for(File f : chunk_folder_contents){
            if (!FilenameUtils.getExtension(f.getName()).equals("empty")){
                f.delete();
            }
        }
        for(File f : temp_folder_contents){
            if (!FilenameUtils.getExtension(f.getName()).equals("empty")){
                f.delete();
            }
        }

    }


}

