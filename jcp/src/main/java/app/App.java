/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package app;

import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.awt.*;

public class App {

    // to capture total disk space in the system and will be updated with each health check
    static JFrame mainFrame = new JFrame("KRATOS");
    static JList listOfFiles = new JList();
    static JTextArea consoleOutput = new JTextArea();
    static DefaultListModel listModel = new DefaultListModel();
    static RequestSender requestSender;
    static boolean connected;

    //jcp main
    public static void main(String[] args) {

        //debugging modes: 0 - none; 1 - message only; 2 - stack traces only; 3 - stack and
        Debugger.setMode(3);
        Debugger.toggleFileMode();
        initJCP();
        initJFrame();
        int discoveryTimeout = 5;

        // to capture total disk space in the system and will be updated with each health check
        // AtomicLong is already synchronized
        // value in bytes
        AtomicLong totalDiskSpace = new AtomicLong(0);

        Debugger.log("JCP Main: JCP online", null);

        //make a discovery manager and start it, prints results to file
        //this beast will be running at all times
        Thread discManager = new Thread(new DiscoveryManager(Module.JCP, discoveryTimeout, false));
        discManager.start();
        List<Integer> stalkerList;
        Map<Integer, NodeAttribute> harmlist;

        int attempts = 0;
        //wait for at least 1 connection
        while (!connected){
            //we will wait for network discovery to do its thing
            wait((discoveryTimeout * 1000) + 5000);
            stalkerList = NetworkUtils.getStalkerList("config/stalkers.list");
            try{
                if (stalkerList != null && stalkerList.size() >= 1){
                    connected = true;
                }
                else{
                    Debugger.log("JCP Main: No STALKERs detected yet...", null);
                    Debugger.log( "JCP Main: Waiting for servers to become available...", null);
                }

            }
            catch(NullPointerException e){
                Debugger.log("JCP Main: Problem during network discovery...", e);
            }
            attempts++;
        }
        Debugger.log("JCP Main: System ready to take requests!", null);

        requestSender = RequestSender.getInstance();
        //starting health checker tasks for each stalker in the stalker list
        Thread healthChecker= new Thread(new HealthChecker(Module.JCP, totalDiskSpace, true));
        healthChecker.start();
        retrieveFiles();


        //ip of stalker we'll just use the one at index 1 for now
        while(true){
            try{
                Thread.sleep((10000));
            }
            catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }

    public static void initJCP(){
        List<File> dirs = new ArrayList();
        dirs.add(new File("logs"));
        dirs.add(new File("config"));
        NetworkUtils.initDirs(dirs, true, 1);
    }

    //round robin through the stalkers and try to get a connection
    public static Socket connectToStalker(){
        int port = 11111;
        HashMap<Integer, String> m =  NetworkUtils.mapFromJson(NetworkUtils.fileToString("config/stalkers.list"));
        List<Integer> s_list = NetworkUtils.mapToSList(m);
        Socket stalker = null;
        for (Integer id : s_list){
            String stalkerip =  m.get(id);
            stalker = requestSender.connect(stalkerip, port);
            if (stalker != null){
                break;
            }
        }
        return(stalker);
    }
//
//    //load a config (stalker ip) from file while we get network discovery working
    public static void retrieveFiles() {

        if (connected){
            Socket connection = connectToStalker();
            //uncomment this:
            listModel.clear();
            List<String> fileList = requestSender.getFileList();
            for (int i=0; i < fileList.size(); i++) {
                listModel.addElement(fileList.get(i));
            }
            //remove this:
            listOfFiles.setModel(listModel);
            consoleOutput.append("Listed files.\n");
            Debugger.log("JCP Main: File list operation complete", null);
            try{ connection.close();}
            catch(IOException e){ Debugger.log("", e);;}
        }
        else{
            consoleOutput.append("JCP Main: Connecting to server, please wait.\n");
        }

    }

    public static void chooseFile() {
        if (connected){
            Socket connection = connectToStalker();
            JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
            int returnValue = jfc.showOpenDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = jfc.getSelectedFile();

                String name = FilenameUtils.separatorsToUnix(selectedFile.getAbsolutePath());
                System.out.println(name);
                //remove this:
                listModel.addElement(selectedFile.getName());
                //uncomment this:
                requestSender.sendFile(name);
                consoleOutput.append("Uploaded " + selectedFile + "\n");
                Debugger.log("JCP Main: Uploaded " + selectedFile, null);
            }
            try{ connection.close();}
            catch(IOException e){ Debugger.log("", e);}
            retrieveFiles();
        }
        else{
            consoleOutput.append("Connecting to server, please wait.\n");
        }



    }

    public static void deleteFile() {
        if (connected){
            Socket connection = connectToStalker();
            int index = listOfFiles.getSelectedIndex();
            Object selectedFilename = listOfFiles.getSelectedValue();
            //remove this:
            listModel.removeElement(selectedFilename);
            //remove this:
            listOfFiles.setModel(listModel);
            //uncomment this:
            requestSender.deleteFile(selectedFilename.toString());
            consoleOutput.append("Deleted " + selectedFilename.toString() + "\n");

            Debugger.log("JCP Main: Deleted " + selectedFilename.toString(), null);
            try{ connection.close();}
            catch(IOException e){ Debugger.log("", e);}
            retrieveFiles();
        }
        else{
            consoleOutput.append("Connecting to server, please wait.\n");
        }


    }

    public static void downloadFile() {
        if (connected){
            Socket connection = connectToStalker();
            String selectedFilename = listOfFiles.getSelectedValue().toString();
            //remove this?:
            JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
            jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnValue = jfc.showOpenDialog(null);
            //
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = jfc.getSelectedFile();
                //uncomment this:
                requestSender.getFile(selectedFile + "/" + selectedFilename);
                consoleOutput.append("Downloaded " + selectedFilename + " to " + selectedFile + "\n");
                Debugger.log("JCP Main: Downloaded " + selectedFilename + " to " + selectedFile, null);
            }
            try{ connection.close();}
            catch(IOException e){Debugger.log("", e);}
        }
        else{
            consoleOutput.append("Connecting to server, please wait.\n");
        }

    }
    public static void wait(int millis){
        try{
            //wait for a bit
            Thread.sleep((long)((millis)));
        }
        catch (InterruptedException ex){
            ex.printStackTrace();
        }
    }
    public static void initJFrame(){
        String request = null;
        String filename = null;

        //set up the gui
		JButton uploadButton = new JButton("Upload");
		JButton listButton = new JButton("List Files");
		JButton downloadButton = new JButton("Download");
		JButton deleteButton = new JButton("Delete");
		JScrollPane scrollableList = new JScrollPane(listOfFiles);
		listOfFiles.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane scrollableConsole = new JScrollPane(consoleOutput);
		consoleOutput.setEditable(false);
		
		//mainFrame.getContentPane().getLayout().setHgap(50);
		//mainFrame.getContentPane().getLayout().setVgap(50);
		
		GridBagConstraints c = new GridBagConstraints();
		
		mainFrame.getContentPane().setLayout(new GridBagLayout());
		
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0;
		c.weighty = 0;
		c.insets.top = 10;
		c.insets.left = 10;
		c.insets.right = 10;
		c.insets.bottom = 10;
		
		c.gridx = 1;
		c.gridy = 0;
		mainFrame.getContentPane().add(uploadButton, c);
		
		c.gridx = 2;
		c.gridy = 0;
		mainFrame.getContentPane().add(listButton, c);
		
		c.gridx = 1;
		c.gridy = 2;
		mainFrame.getContentPane().add(downloadButton, c);
		
		c.gridx = 2;
		c.gridy = 2;
		mainFrame.getContentPane().add(deleteButton, c);
		
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 0.7;
		c.weighty = 0.7;
		
		c.gridx = 0;
		c.gridy = 0;
		scrollableConsole.setPreferredSize(new Dimension(50, 30));
		mainFrame.getContentPane().add(scrollableConsole, c);
		
		c.weightx = 1;
		c.weighty = 1;
		
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 3;
		scrollableList.setPreferredSize(new Dimension(500, 300));
		mainFrame.getContentPane().add(scrollableList, c);

        //set up listeners
        UploadListener uploadListener = new UploadListener();
        uploadButton.addActionListener(uploadListener);
        ListListener listListener = new ListListener();
        listButton.addActionListener(listListener);
        DownloadListener downloadListener = new DownloadListener();
        downloadButton.addActionListener(downloadListener);
        DeleteListener deleteListener = new DeleteListener();
        deleteButton.addActionListener(deleteListener);

        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setPreferredSize(new Dimension(500,400));
		mainFrame.pack();
		mainFrame.setVisible(true);

        //bring window to front
        mainFrame.setAlwaysOnTop(true);
        mainFrame.setAlwaysOnTop(false);
    }

}
