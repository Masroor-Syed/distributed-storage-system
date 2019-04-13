package app;

import app.LeaderUtils.IndexManager;
import app.chunk_utils.IndexFile;
import app.chunk_utils.Indexer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ElectionListener implements Runnable{
    private int serverPort;
    private boolean running = true;
    private boolean verbose = true;
    private volatile IndexFile index;

    public ElectionListener(){}
    public ElectionListener(IndexFile ind){index = ind;}

    @Override
    public void run() {
        serverPort = ConfigManager.getCurrent().getElection_port();

        ServerSocket server = null;
        CommsHandler commLink = new CommsHandler();

        // we can change this later to increase or decrease
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        try {
            server = new ServerSocket(serverPort);
            //server.setReuseAddress(true);
        } catch (IOException e) {
            Debugger.log("", e);
        }
        Debugger.log("Election responder listening on port " + serverPort, null);
        // will keep on listening for requests
        while (running) {
            try {
                //accept connection from a JCP
                Socket client = server.accept();
                Debugger.log("Election Listener: Accepted connection : "  + client, null);
                // receive packet on the socket link
                TcpPacket req = commLink.receivePacket(client);
                //When a leader request is recieved

                if(req.getMessageType() == MessageType.LEADER){
                    // reply to with leader
                    executorService.submit(new LeaderResponder(client));
                }
                else if (req.getMessageType() == MessageType.REELECT){
                    Debugger.log("Re-election requested...", null);
                    ConfigManager.getCurrent().setReelection(true);
                    //executorService.submit(new LeaderResponder(client));
                }
                else if(req.getMessageType() == MessageType.UPDATE){
                    // Update the indexfile
                    Debugger.log("Update received from leader", null);
                    commLink.sendResponse(client, MessageType.ACK);
                    executorService.execute(new IndexManager(index, Indexer.deserializeUpdate(req.getMessage())));
                }
            } catch (IOException e) {

                Debugger.log("Listener: Socket timeout", e);
            }
        }

    }


}
