/*
	@description Investigate the network by scanning other hosts in subnet and send them a token request
	@author Trieu Hoang Nguyen
	@version 1.0
*/

import java.io.*;
import java.net.*;

public class Investigator implements Runnable
{    
    public void run()
    {
        //Get IP address as string and bytes
        //@see http://stackoverflow.com/questions/2984601/how-to-get-a-byte-representation-from-a-ip-in-string-form-in-java
        try 
        {
            InetAddress localHost = InetAddress.getLocalHost();
            String stringIP = localHost.getHostAddress();
            InetAddress ip = InetAddress.getByName(stringIP);
            byte[] byteIP = ip.getAddress();
            
            //Cast the third byte to int to get host number
            int hostNumber = byteIP[3] &0xff;

            //Getting the prefix length for calculate subnet mask
            //@see http://stackoverflow.com/questions/1221517/how-to-get-subnet-mask-of-local-system-using-java
            NetworkInterface netInterface = NetworkInterface.getByInetAddress(localHost);
            int prefix= netInterface.getInterfaceAddresses().get(0).getNetworkPrefixLength();

            //Calculate subnet mask from prefix length
            int value = 0xffffffff << (32 - prefix);
            int val1 = ((byte) ((value &0xff000000)>>24)) & 0xff;
            int val2 = ((byte) ((value &0x00ff0000)>>16)) & 0xff;
            int val3 = ((byte) ((value &0x0000ff00)>>8)) & 0xff;
            int val4 = ((byte) (value &0x000000ff)) & 0xff;
            String subnetMask = val1 + "." + val2 + "." +val3 + "." + val4;

            //Display network status to user
            System.out.print("Your IP address: " + stringIP + "\t");
            System.out.print("Your host number: " + hostNumber + "\t");
            System.out.println("Your subnet mask: " + subnetMask);

            //Loop through the host number -10 and +10 hosts
            //Loop through 10 hosts backward
            System.out.println("Finding hosts...");
            int timeout = 5000;
            int port = 4000;
            if (hostNumber > 10)
            {
                for (int i = (hostNumber - 10); i < hostNumber; i++) //eg. 23 - 10 = 13 -> loop from 10
                {
                    byteIP[3] = (byte)i; 
                    InetAddress address = InetAddress.getByAddress(byteIP);
                    Thread t = new Thread(new CheckIP(address, port, timeout));
                    t.start();
                }
            }
            else
            {
                for (int i = 1; i < hostNumber; i++) // current host is 8 => loop from 1 to 7
                {
                    byteIP[3] = (byte)i; 
                    InetAddress address = InetAddress.getByAddress(byteIP);
                    Thread t = new Thread(new CheckIP(address, port, timeout));
                    t.start();
                }
            }


            //Loop through 10 hosts forward
            int checkHost = 254 - hostNumber; //eg 254 - 252 = 2
            if (checkHost >= 10)
            {
                for (int i = hostNumber + 1; i <= (hostNumber + 10); i++) //eg. host 161 => loop from 162 to 171
                {
                        byteIP[3] = (byte)i; 
                        InetAddress address = InetAddress.getByAddress(byteIP);
                        Thread t = new Thread(new CheckIP(address, port, timeout));
                        t.start();
                }
            }
            else
            {
                for (int i = hostNumber + 1; i <= 254; i++) //eg. host 252 => loop from 253 to 254
                {
                        byteIP[3] = (byte)i; 
                        InetAddress address = InetAddress.getByAddress(byteIP);
                        Thread t = new Thread(new CheckIP(address, port, timeout));
                        t.start();
                }
            }
        }
        catch (UnknownHostException e)
        {
            e.printStackTrace();
        }
        catch (SocketException e)
        {
            e.printStackTrace();
        }
    }
    
    //A thread to run individually with a timeout to reduce the waiting time for the scan process
    public static class CheckIP implements Runnable
    {
        private InetAddress _ip;
        private int _timeout;
        private int _port;
        private String _token = "1996";
        
        public CheckIP(InetAddress ip, int port, int timeout)
        {
            _ip = ip;
            _port = port;
            _timeout = timeout;
        }
        
        public void run()
        {
            try
            {
                if (_ip.isReachable(_timeout)) //Check if ip is reachable within a specific timeout then send a token request message
                {
                    String output = _ip.toString().substring(1);
                    System.out.println(output + " is on the network");
                    Thread t = new Thread(new UDPMessageServer(_ip, _port, _token));
                    t.start();
                }  
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}