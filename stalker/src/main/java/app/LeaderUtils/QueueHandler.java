package app.LeaderUtils;

import app.CommsHandler;
import app.MessageType;
import app.NetworkUtils;
import app.TcpPacket;

import java.io.IOException;
import java.net.Socket;
import java.util.PriorityQueue;

public class QueueHandler implements  Runnable {

    private static final Object lock1 = new Object();
    private static final Object lock2 = new Object();

    private int mode;
    private QueueEntry q;
    private CRUDQueue pQueue;
    public QueueHandler(int mode, QueueEntry q, CRUDQueue pQueue){
        this.pQueue = pQueue;
        this.mode = mode;
        this.q = q;
    }

    @Override
    public void run(){
        switch(mode){
            case 0:
                queueJob();
                break;
            case 1:
                getJob();
                if (!processJob()){
                    //if it fails we'll put it back in the queue
                    queueJob();
                }
                break;
        }
    }
    public boolean processJob(){
        CommsHandler commLink = new CommsHandler();
        Socket worker;
        try{
            System.out.println(NetworkUtils.timeStamp(1) + " Processing job...");
            worker = NetworkUtils.createConnection(q.getInetAddr().getHostAddress(), 11113);
            //connect and grant permission to edit file
            MessageType response = commLink.sendPacket(worker, MessageType.START, "", true);
            //get ack that job is done
            //TcpPacket response = commLink.receivePacket(worker);
            if (response == MessageType.DONE){
                //send permission to worker
                commLink.sendPacket(worker, MessageType.ACK, "", false);
                System.out.println(NetworkUtils.timeStamp(1) + " job complete");
            }


        }
        catch (IOException e){
            e.printStackTrace();
            return(false);
        }


        return true;
    }
    //put an entry into the queue
    public void queueJob(){
        System.out.println(NetworkUtils.timeStamp(1) + "Queuing job.");
        pQueue.add(q);
        System.out.println(NetworkUtils.timeStamp(1) + "Job Queued");

    }
    //remove entry from queue
    public void getJob(){
        q = pQueue.remove();
    }

}
