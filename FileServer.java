/*
	@description File server is used to receive file from a send by TCP
	@author Trieu Hoang Nguyen
	@version 1.0
*/

import java.io.*;
import java.net.*;

public class FileServer implements Runnable
{    
    public void run()
    {
        ReceiveFile();
    }
    
    public void ReceiveFile()
    {
        try
        {
            //Open server socket
            ServerSocket serverSocket = new ServerSocket(4004);

            while(true)
            {
           
                //Open a socket that waits for connection to request
                Socket welcomeSocket = serverSocket.accept();

                //Create input stream to get file name and file size from sender
                DataInputStream inFromSender = new DataInputStream(welcomeSocket.getInputStream());
                String filename =  inFromSender.readUTF();
                long fileSize = inFromSender.readLong();
                
                //@see https://coderanch.com/t/556838/java/Transferring-file-file-data-socket
                //Create instream and outstream to read input from sender then write to specific directory with same file name
                byte[] byteArray = new byte[1024];
                InputStream inStream = welcomeSocket.getInputStream();
                FileOutputStream fos = new FileOutputStream("FilesReceived\\" + filename);
                BufferedOutputStream bufferOut = new BufferedOutputStream(fos);
                
                int bytesRead;
                //Write file to directory until file size equals 0
                while (fileSize > 0 && (bytesRead = inStream.read(byteArray, 0, (int)Math.min(byteArray.length, fileSize))) != -1)   
                {   
                    bufferOut.write(byteArray, 0, bytesRead);   
                    fileSize -= bytesRead;   
                }
                bufferOut.close();
                
                System.out.println("RECEIVED: " + filename);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}