/*
	@description Message Process Server to receive message from different ports for different purposes
	@author Trieu Hoang Nguyen
	@version 1.0
*/

import java.io.*;
import java.net.*;
import java.util.*;

//Based on the simple UDP server and client structure
//@see https://systembash.com/a-simple-java-udp-server-and-udp-client/
public class MessageProcessServer implements Runnable
{
    private int _tokenPort = 4000;
    private int _namePort = 4001;
    private int _messagePort = 4002;
    private int _disconnectPort = 4003;
    private String _token = "1996";
    private String _name; //name of the current host
    private Map<String, String> _peers = new HashMap<String, String> (); //Hashmap to match other peers names with ip addresses
    private ArrayList<InetAddress> _checkList = new ArrayList<InetAddress>(); //Arraylist is used to check if an IP is in list or not, more clarify later
    
    public MessageProcessServer(String name)
    {
        _name = name;
    }
    
    public void run()
    {
        Thread t = new Thread(new ReceiveToken());
        t.start();
        Thread t2 = new Thread(new ReceiveName());
        t2.start();
        Thread t3 = new Thread(new ReceiveMessage());
        t3.start();
        Thread t4 = new Thread(new ReceiveDisconnect());
        t4.start();
    }
    
    //Display the online peers
    public void ConnectedPeers()
    {
        System.out.println("Connected peers:");
        for (Map.Entry<String, String> entry : _peers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            System.out.println(value + " - " + key.substring(1));
        }
    }
    
    //Return the list of peers (_checkList) for sending purposes in main method
    public ArrayList<InetAddress> getPeers()
    {
        if (_checkList.size() == 0)
        {
            System.out.println("List is null");
        }
        return _checkList;
    }
    
    
    
    //Open socket with port 4000 for receiving token
    public class ReceiveToken implements Runnable
    {
        public void run()
        {
            try
            {
                DatagramSocket receiveSocket = new DatagramSocket(_tokenPort);
                while (true)
                {
                    byte[] receiveData = new byte[1024];
                    //Open socket to receive datagram packet
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    receiveSocket.receive(receivePacket);
                    //Trim the receive message to get rid of empty bytes in array
                    String receiveMessage = new String(receivePacket.getData()).trim();

                    //Check identity of sender
                    InetAddress senderIP = receivePacket.getAddress();
                    boolean ipInList = _checkList.contains(senderIP); 

                    //Check token and sender IP in list or not, if meet conditions, add sender to list and send token back
                    if ((receiveMessage.equals(_token)) && (ipInList == false))
                    {
                        Thread t = new Thread(new UDPMessageServer(senderIP, _tokenPort, _token));
                        t.start();
                        _checkList.add(senderIP);
                    }
                    else if ((receiveMessage.equals(_token)) && (ipInList == true)) //if the sender IP is alrealy in _checkList, then does not add again as well as not send token back anymore. Instead, send a name request message to ask for name of that peer 
                    {
                        Thread t = new Thread(new UDPMessageServer(senderIP, _namePort, _name));
                        t.start();
                    }
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
    
    //Open socket with port 4001 for receiving name
    public class ReceiveName implements Runnable
    {
        public void run()
        {
            try
            {
                DatagramSocket receiveSocket = new DatagramSocket(_namePort);
                while (true)
                {
                    byte[] receiveData = new byte[1024];
                    //Open socket to receive datagram packet
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    receiveSocket.receive(receivePacket);

                    //Get identity of sender
                    String senderName = new String(receivePacket.getData()).trim(); //Trim the receive message to get rid of empty bytes in array
                    InetAddress senderIP = receivePacket.getAddress();

                    //Check if ip not in hashmap then add to hashmap ip and name matching the ip. Then send current host name back
                    if (_peers.containsKey(senderIP.toString()) == false)
                    {
                        _peers.put(senderIP.toString(), senderName);
                        System.out.println(_peers.get(senderIP.toString()) + " connected to group");
                        Thread t = new Thread(new UDPMessageServer(senderIP, _namePort, _name));
                        t.start();
                        //Display current connected peers every time a new peer join
                        ConnectedPeers();
                    }
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
    
    //Open socket with port 4002 for receiving messages
    public class ReceiveMessage implements Runnable
    {
        //Open socket for receiving message
        public void run()
        {
            try
            {
                DatagramSocket receiveSocket = new DatagramSocket(_messagePort);
                while (true)
                {
                    byte[] receiveData = new byte[1024];
                    //Open socket to receive datagram packet
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    receiveSocket.receive(receivePacket);
                    //Trim the receive message to get rid of empty bytes in array
                    String receiveMessage = new String(receivePacket.getData()).trim();

                    //Check identity of sender
                    InetAddress senderIP = receivePacket.getAddress();
                    //Display received message along with sender name and IP address
                    System.out.println(_peers.get(senderIP.toString()) + " - " + senderIP.toString().substring(1) + ": " + receiveMessage);
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
    
    //Open socket with port 4003 to receive a <BYE> message when a peer left and remove them from list as well as hashmap
    public class ReceiveDisconnect implements Runnable
    {
        //Open socket for receiving disconnected message from a peer
        public void run()
        {
            try
            {
                DatagramSocket receiveSocket = new DatagramSocket(_disconnectPort);
                while (true)
                {
                    byte[] receiveData = new byte[1024];
                    //Open socket to receive datagram packet
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    receiveSocket.receive(receivePacket);
                    String receiveMessage = new String(receivePacket.getData()).trim();

                    //Check identity of sender
                    InetAddress senderIP = receivePacket.getAddress();
                    System.out.println(_peers.get(senderIP.toString()) + "-" + senderIP.toString().substring(1) + ": " + receiveMessage);
                    if(receiveMessage.equals("<BYE>"))
                    {
                        _peers.remove(senderIP.toString());
                        _checkList.remove(senderIP);
                        ConnectedPeers(); //Display list of online peers when a peer left
                    }
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}