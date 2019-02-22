/*
	@description File sender is used to send specific file to an IP through TCP, accept parameters: ip address and file name
	@author Trieu Hoang Nguyen
	@version 1.0
*/

import java.io.*;
import java.net.*;

public class FileSender implements Runnable
{
    private InetAddress _ip;
    private String _filename;
    private int _serverPort = 4004;
    
    public FileSender(String ip, String filename)
    {
        try
        {
            _ip = InetAddress.getByName(ip);
        }
        catch (UnknownHostException e)
        {
            e.printStackTrace();
        }
        _filename = filename;
    }
    
    public void run()
    {
        SendFile(_ip, _filename);
    }
    
    //@see https://coderanch.com/t/556838/java/Transferring-file-file-data-socket
    //Send file using TCP
    public void SendFile(InetAddress ip, String filename)
    {
        try
        {
            Socket connectionSocket = new Socket(ip, _serverPort);
            
            //Get file and initialize with length with file size
            File fileToSend = new File("H:\\" + filename);
            byte[] fileByteArray = new byte[(int) fileToSend.length()];
            
            //Get output stream to send file name and file size to receiver
            DataOutputStream outToReceiver = new DataOutputStream(connectionSocket.getOutputStream());
            outToReceiver.writeUTF(filename);
            outToReceiver.writeLong(fileByteArray.length);
            outToReceiver.flush();
            
            //Read to input stream and write to output stream then send to receiver
            BufferedInputStream bufferIn = new BufferedInputStream(new FileInputStream(fileToSend));
            bufferIn.read(fileByteArray, 0, fileByteArray.length);
            OutputStream outStream = connectionSocket.getOutputStream();
            outStream.write(fileByteArray, 0, fileByteArray.length);
            outStream.flush();
            
            connectionSocket.close();
        }
        catch (UnknownHostException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}