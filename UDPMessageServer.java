/*
	@description UDP Server to send message by UDP datagram packet, accept parameters: ip of receiver, port number or receiver and message
	@author Trieu Hoang Nguyen
	@version 1.0
*/

import java.io.*;
import java.net.*;

//Based on the simple UDP server and client structure
//@see https://systembash.com/a-simple-java-udp-server-and-udp-client/

public class UDPMessageServer implements Runnable
{
    private InetAddress _sendTo;
    private String _message;
    private int _port;
    
    public UDPMessageServer(InetAddress sendTo, int port, String message)
    {
        _sendTo = sendTo;
        _port = port;
        _message = message;
    }
    
    //Send message with UDP
    public void SendMessage(InetAddress sendTo, int port, String message)
    {
        try
        {
            //Create a socket
            DatagramSocket sendSocket = new DatagramSocket();
            byte[] sendData = new byte[1024];
            //Initialize send data with a message
            sendData = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, sendTo, port);
            sendSocket.send(sendPacket);
            sendSocket.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }     
    }
    
    public void run()
    {
        SendMessage(_sendTo, _port, _message);
    }
}