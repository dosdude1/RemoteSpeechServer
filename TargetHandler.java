
package RemoteSpeechServer;

import java.net.*;
import java.io.*;
import java.util.Hashtable;
import java.util.Set;

/*
 * Handles the connection of a single client connected to the RemoteSpeechServer
 * server.
 */

public class TargetHandler extends Thread
{
    
    private Socket socket;
    private String ID;
    private BufferedReader input;
    private PrintWriter output;
    private String currentVoice;
    private RemoteSpeechServer main;
    private String voices;
    private int volume;
    private boolean updateIsAvaiable;
    
    private boolean receivingRawData = false;
    
    private DataTransferHandler dth;
    
    
    public TargetHandler(Socket clientSocket, BufferedReader in, PrintWriter out, String inID, RemoteSpeechServer inMain)
    {
        main=inMain;
        socket=clientSocket;
        ID=inID;
        input=in;
        output=out;
    }
    /*
     * Main EventLoop
     */
    public void run()
    {
        String read=null;
        Hashtable<String, User> users=main.getUsers();
        while (true)
        {
            try
            {
                //Waits for the next line of incoming data.
                read = input.readLine();
                if (read == null || read.equals("close-connection"))
                {
                    //Handle disconnect event.
                    closeSocket();
                    
                    
                    //Terminate EventLoop.
                    return;
                }
                else if (read.contains("voice:"))
                {
                    currentVoice=read.substring(read.indexOf(":")+1);
                }
                else if (read.contains("voices:"))
                {
                    voices=read.substring(read.indexOf(":")+1);
                }
                else if (read.contains("volume:"))
                {
                    volume=Integer.valueOf(read.substring(read.indexOf(":")+1));
                }
                else if (read.equals("update-client-info"))
                {
                    Set<String> keys = users.keySet();
                    for (String key : keys)
                    {
                        String [] targetIDs=users.get(key).getTargetIDs();
                        for (int j=0; j<targetIDs.length; j++)
                        {
                            if (ID.equals(targetIDs[j]))
                            {
                                main.sendMessageToClientsOfName(key, "target-status-changed");
                            }
                        }
                    }
                }
                else if (read.contains("update-status:"))
                {
                    String update=read.substring(read.indexOf(":")+1);
                    if (update.equals("yes"))
                    {
                        updateIsAvaiable=true;
                    }
                    else
                    {
                        updateIsAvaiable=false;
                    }
                }
                else if (read.contains("update-result:"))
                {
                    String result=read.substring(read.indexOf(":")+1);
                    Set<String> keys = users.keySet();
                    for (String key : keys)
                    {
                        String [] targetIDs=users.get(key).getTargetIDs();
                        for (int j=0; j<targetIDs.length; j++)
                        {
                            if (ID.equals(targetIDs[j]))
                            {
                                main.sendMessageToClientsOfName(key, "update-result:"+ID+";"+result);
                            }
                        }
                    }
                }
                else if (read.contains("ready-to-receive"))
                {
                    receivingRawData = true;
                    dth.start();
                }
            }
            catch (SocketTimeoutException e)
            {
                output.println("heartbeat");
                output.flush();
            }
            catch (IOException e)
            {
                System.out.println(e);
                System.out.println("TargetHandler Exception");
                closeSocket();
                return;
            }
        }
    }
    /*
     * ToString method for a Client object.
     */
    private void closeSocket()
    {
        try
        {
            input.close();
            output.close();
            socket.close();
            main.removeTarget(this);
        }
        catch (IOException e)
        {
            System.out.println(e);
        }
    }
    String getCurrentVoice()
    {
        return currentVoice;
    }
    String getVoicesList()
    {
        return voices;
    }
    public String toString()
    {
        return ID;
    }
    int getCurrentVolume()
    {
        sendMessage("send-volume");
        return volume;
    }
    void setVolume(int vol)
    {
        volume=vol;
        sendMessage("setvolume:"+vol);
    }
    boolean getUpdateStatus()
    {
        sendMessage("get-update-status");
        return updateIsAvaiable;
    }
    /*
     * Send message to the connected Client.
     */
    public boolean equals(TargetHandler t)
    {
        if (this==t)
        {
            return true;
        }
        return false;
    }
    void sendMessage(String msg)
    {
        if (!receivingRawData)
        {
            output.println(msg);
            output.flush();
        }
    }
    void prepareToReceiveAudio(String fileName, int expectedFileSize, Client requestingClient)
    {
        dth = new DataTransferHandler(requestingClient, this, expectedFileSize, fileName);
        sendMessage("receiving-audio:"+fileName+";"+expectedFileSize);
    }
    DataOutputStream getDataOutputStream()
    {
        try
        {
            return new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        }
        catch (IOException e)
        {
            System.err.println(e);
        }
        return null;
    }
    void audioSendingComplete()
    {
        receivingRawData = false;
    }
    boolean isReceivingAudio()
    {
        return receivingRawData;
    }
    void dataTransferSocketErrorOccurred()
    {
        System.out.println("TargetHandler Data I/O Exception");
        closeSocket();
    }
}
