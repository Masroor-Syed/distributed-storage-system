package app;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


public class NetDiscovery implements Runnable{
    private static DatagramSocket socket = null;
    private static int STALKERPORT = 10000;
    private static int JCPPORT = 11000;

    private String sender;
    private String target;
    private static int discovery_timeout = 0;

    public NetDiscovery(Module sender, Module target, int discovery_timeout) {
        this.sender = sender.name();
        this.target = target.name();
        this.discovery_timeout = discovery_timeout;
    }


    @Override
    public void run() {
        HashMap<Integer,InetAddress> listOfAddrs =  null;
        try {
            listOfAddrs = broadcast(MessageType.DISCOVER,sender,target);
            NetworkUtils.toFile("config/stalkers.list", listOfAddrs);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Takes in RequestType and target as arguments
     * broadcasts a UDP packet over a LAN network
     * @return list of address of the modules that repllied
     */
    public static HashMap<Integer, InetAddress> broadcast(MessageType request, String sender,String target) throws IOException {
        // To broadcast change this to 255.255.255.255
        InetAddress address = InetAddress.getByName("127.0.0.1");       // broadcast address

        //ArrayList<InetAddress> listOfAddrs = new ArrayList<>();        // list of the ip of the modules that replied
        //we want a map of MAC -> ip
        HashMap<Integer, InetAddress> stalkerMap = new HashMap<>();

        socket = new DatagramSocket();                                  // socket to broadcast request
        DatagramSocket receiverSocket = new DatagramSocket(JCPPORT);    // socket to receive replys


        // create a discover request packet and broadcast it
        DiscoverPkt discovery = new DiscoverPkt(request, sender,target, NetworkUtils.getIP());
        ObjectMapper mapper = new ObjectMapper();
        System.out.println(mapper.writeValueAsString(discovery));
        byte[] req = mapper.writeValueAsString(discovery).getBytes();
        DatagramPacket packet = new DatagramPacket(req, req.length, address, STALKERPORT);
        socket.send(packet);

        // waits for 5 sec to get response from the LAN
        long t= System.currentTimeMillis();
        long end = t+ (discovery_timeout*1000);
        while(System.currentTimeMillis() < end) {
            byte[] buf = new byte[1024];
            packet = new DatagramPacket(buf, buf.length);
            // set socket timeout to 1 sec
            try {
                receiverSocket.setSoTimeout(1000);
                receiverSocket.receive(packet);
            }
            catch (SocketTimeoutException e)
            {
                continue;
            }
            String received = new String(packet.getData(), 0, packet.getLength());
            System.out.println(received);

            // parse the packet content
            JsonNode discoverReply = mapper.readTree(received);
            String module = discoverReply.get("sender").textValue();
            String uuid = discoverReply.get("uuid").textValue();
            InetAddress replyAddress =  InetAddress.getByName(discoverReply.get("address").textValue());
            stalkerMap.put(Integer.valueOf(uuid), replyAddress);
        }
        socket.close();
        return stalkerMap;
    }


}
