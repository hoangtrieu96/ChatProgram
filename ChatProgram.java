/*
	@description Chat program act as a controller and interface, manage the flow of the program
	@author Trieu Hoang Nguyen
	@version 1.0
*/

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatProgram
{
    public static void main(String args[])
    {   
        boolean isContinue = true; //boolean value to determine wether user want to continue or not
        int messagePort = 4002; //send user text message through this port
        int disconnectPort = 4003; //when a user logged out, send message through this port
        
        try
        {
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Enter your chat name: ");
            String name = userInput.readLine();
            
            //Create MessageProcessServer thread to wait for messages
            MessageProcessServer mps = new MessageProcessServer(name);
            Thread t = new Thread(mps);
            t.start();
            
            //Create Investigator thread to scan other hosts and send token request
            Thread t2 = new Thread(new Investigator());
            t2.start();
            
            //Create FileServer thread to wait for receiving file transfer
            Thread t6 = new Thread(new FileServer());
            t6.start();
            
            //Display all commands available in this program
            DisplayInstruction();
            
            while(isContinue)
            {
                String message = userInput.readLine(); //get user command

                //Check if the list of peers is not empty and the user command must be not empty as well, otherwise do nothing
                if(mps.getPeers().size() > 0 && !message.trim().isEmpty())
                {
                    ArrayList<InetAddress> receiverList = mps.getPeers(); //When list is not empty, assign list to a specific variable
                    if (message.equals("quit")) //Check user want to continue or not
                    {
                        //When user quit, send a message to everyone in chat group through disconnectPort for removing that user from peers list
                        for (InetAddress ip: receiverList)
                        {
                            Thread t3 = new Thread(new UDPMessageServer(ip, disconnectPort, "<BYE>"));
                            t3.start();
                        }
                        System.out.println("Application closed");
                        isContinue = false; //Stop while loop
                    }
                    else if (message.charAt(0) == '<' && message.charAt(message.length() - 1) == '>') //Check user send file command
                    {
                        System.out.println("Preparing to send file");
                        
                        //Command for sending file to particular peers after remove "<>"
                        String sendFileCommand = message.substring(1, message.length() - 1); //eg. <File,10.1.7.19,image.png> would be File,10.1.7.19,image.png
                        
                        //Get necessary information to send file
                        //Notice that if the command is missing of statements (3 statements), then the split string will return "error" instead of user command
                        //Therefore, unless enter enough 3 statements with "," between them, the validation will keep say error
                        String command = SplitPart(sendFileCommand, 0);
                        String receiverIP = SplitPart(sendFileCommand, 1);
                        String fileName = SplitPart(sendFileCommand, 2);
                        
                        //Validate command, ip and file name
                        if (CommandValidation(command, receiverIP, fileName))
                        {
                            //Send file if ip in peers list, else print out a message that is Unknonw host
                            if(receiverList.contains(InetAddress.getByName(receiverIP)))
                            {
                                Thread t5 = new Thread(new FileSender(receiverIP, fileName));
                                t5.start();
                                System.out.println("SENT");  
                            }
                            else
                                System.out.println("Unknown host - host is not online");
                            
                        }
                    }
                    else //If nothing above then send as normal message for everyone in group chat
                    {
                        for (InetAddress ip: receiverList)
                        {
                            Thread t4 = new Thread(new UDPMessageServer(ip, messagePort, message));
                            t4.start();
                        }
                    } 
                }
                //This condition use for when there is not one online, the only thing user can do is to type exit command, the others won't work
                else if (mps.getPeers().size() == 0 && !message.trim().isEmpty())
                {
                    if (message.equals("quit")) //Check user want to continue or not
                    {
                        System.out.println("Application closed");
                        isContinue = false;
                    }
                }
            } 
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    //Display all commands in the program
    public static void DisplayInstruction()
    {
        System.out.println("Type any message to send to group.");
        System.out.println("Type <File,IP,name> to send file to a peer");
        System.out.println("Type quit to exit the program");
    }
    
    //Split string with specific delimiter and put in an array, get a particular array element by index parameter
    public static String SplitPart(String str, int index)
    {
        String[] parts = str.split(",");
        //Whenever the length of array not 3 then print out error message and return "error" instead of the split string
        if(parts.length < 3)
        {
            System.out.println("Missing statements");
            return "error";
        }
        else if (parts.length > 3)
        {
            System.out.println("Too many statements");
            return "error";
        }
        else
        {
            return parts[index];
        }
    }
    
    //Validate send file command
    public static boolean CommandValidation(String command, String ip, String file)
    {
        boolean result = true;
        //Validate each parameter
        if(!command.equals("File")) //Validate initial command
        {
            System.out.println("Wrong command - Must write File for the first statement");
            result = false;
        }
        if (!IpValidation(ip)) //Validate IP address format
        {
            result = false;
        }
        if (!FileValidation(file)) //Validate file existence
        {
            result = false;
        }
        return result;
    }
    
    //Check is ip address
    //@see http://stackoverflow.com/questions/5667371/validate-ipv4-address-in-java
    private static boolean IpValidation(String str)
    {
        final Pattern pattern = Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
        if (!pattern.matcher(str).matches())
        {
            System.out.println("Invalid IP address");
            return false;
        }
        else
            return true;
    }
    
    //Check file exists
    private static boolean FileValidation(String str)
    {
        File file = new File("H:\\" + str);
        if (!file.exists())
        {
            System.out.println("File is not exist");
            return false;
        }
        else
            return true;
    }
}