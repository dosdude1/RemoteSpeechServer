

package RemoteSpeechServer;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/*
 * Handles the connection of a single client connected to the RemoteSpeechServer
 * server.
 */

public class Client extends Thread
{
    
    private Socket socket;
    private String name;
    private BufferedReader input;
    private PrintWriter output;

    private RemoteSpeechServer main;
    
    private final int TARGET_ID_SIZE=6;
    
    private boolean receivingRawData = false;
    private int expectedFileSize = 0;
    private String targetIDToSendAudio;
    
    private Timer dataStreamChecker;
    private boolean dataStreamDied = false;
    
    public Client(Socket clientSocket, BufferedReader in, PrintWriter out, String inName, RemoteSpeechServer inMain)
    {
        main=inMain;
        socket=clientSocket;
        name=inName;
        input=in;
        output=out;
    }
    /*
     * Main EventLoop
     */
    public void run()
    {
        String read;
        while (true)
        {
            try
            {
                if (!receivingRawData)
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
                    else if (read.equals("send-targets"))
                    {
                        String [] targets=main.getTargetsOfUser(name);
                        String toSend="targets:\n";
                        for (int i=0; i<targets.length; i++)
                        {
                            toSend+=targets[i]+"\n";
                        }
                        sendMessage(toSend);
                    }
                    else if (read.contains("sendmessage:"))
                    {
                        ArrayList<String> targetsToSendTo=new ArrayList<String>();
                        String temp=read.substring(read.indexOf(":")+1, read.indexOf(";"));
                        if (temp.contains(","))
                        {
                            targetsToSendTo.add(temp.substring(0, temp.indexOf(",")));
                            temp=temp.substring(temp.indexOf(","));
                        }
                        else
                        {
                            targetsToSendTo.add(temp.substring(0, TARGET_ID_SIZE));
                        }
                        while (temp.charAt(0)==',')
                        {
                            temp=temp.substring(1);
                            if (temp.contains(","))
                            {
                                targetsToSendTo.add(temp.substring(0, temp.indexOf(",")));
                                temp=temp.substring(temp.indexOf(","));
                            }
                            else
                            {
                                targetsToSendTo.add(temp.substring(0, TARGET_ID_SIZE));
                            }
                        }
                        String messageToSend=read.substring(read.indexOf(";")+1);
                        String [] targetsArray=new String [targetsToSendTo.size()];
                        for (int i=0; i<targetsToSendTo.size(); i++)
                        {
                            targetsArray[i]=targetsToSendTo.get(i);
                        }
                        main.sendMessageToTargets(targetsArray, messageToSend);
                    }
                    else if (read.contains("add-target:"))
                    {
                        String targetID=read.substring(read.indexOf(":")+1, read.indexOf(","));
                        String targetName=read.substring(read.indexOf(",")+1);
                        main.getUser(name).addTarget(targetID, targetName);
                        sendTargets();
                    }
                    else if (read.contains("delete-target:"))
                    {
                        String IDToRemove=read.substring(read.indexOf(":")+1);
                        main.getUser(name).removeTarget(IDToRemove);
                        sendTargets();
                    }
                    else if (read.contains("send-voices-list:"))
                    {
                        String voices=main.getVoicesOfTarget(read.substring(read.indexOf(":")+1));
                        sendMessage("voices:"+voices);
                    }
                    else if (read.contains("send-current-volume:"))
                    {
                        String volume=Integer.toString(main.getVolumeOfTarget(read.substring(read.indexOf(":")+1)));
                        sendMessage("volume:"+volume);
                    }
                    else if (read.contains("setvolume:"))
                    {
                        String targetID=read.substring(read.indexOf(":")+1, read.indexOf(";"));
                        main.setVolumeOfTarget(targetID, Integer.valueOf(read.substring(read.indexOf(";")+1)));
                    }
                    else if (read.contains("setvoice:"))
                    {
                        String [] targets=new String [1];
                        targets[0]=read.substring(read.indexOf(":")+1, read.indexOf(";"));
                        main.sendMessageToTargets(targets, "setvoice:"+read.substring(read.indexOf(";")+1));
                    }
                    else if (read.contains("rename-target:"))
                    {
                        String targetID=read.substring(read.indexOf(":")+1, read.indexOf(";"));
                        String nameToSet=read.substring(read.indexOf(";")+1);
                        main.getUser(name).renameTarget(targetID, nameToSet);
                        sendTargets();
                    }
                    else if (read.contains("send-audio-to-target"))
                    {
                        String targetID=read.substring(read.indexOf(":")+1, read.indexOf(";"));
                        targetIDToSendAudio = targetID;
                        String temp = read.substring(read.indexOf(";")+1);
                        expectedFileSize = Integer.valueOf(temp.substring(0, temp.indexOf(";")));
                        String fileName = temp.substring(temp.indexOf(";")+1);
                        receivingRawData = main.prepareToSendAudioToTarget(targetID, fileName, expectedFileSize, this);
                    }
                }
                else
                {
                    dataStreamDied = false;
                    TimerTask timerTask = new TimerTask() {
                        
                        @Override
                        public void run() {
                            checkDataStream();
                        }
                    };
                    dataStreamChecker = new Timer();
                    dataStreamChecker.scheduleAtFixedRate(timerTask, 3000, 3000);
                    int count = 0;
                    int totalSize = 0;
                    byte[] buffer = new byte[8192];
                    DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                    DataOutputStream out = main.getTargetOutputStream(targetIDToSendAudio);
                    while ((totalSize < expectedFileSize) && receivingRawData)
                    {
                        count = in.read(buffer);
                        totalSize += count;
                        out.write(buffer, 0, count);
                        out.flush();
                        dataStreamDied = false;
                    }
                    dataStreamChecker.cancel();
                    receivingRawData = false;
                }
            }
            catch (SocketTimeoutException e)
            {
                output.println("heartbeat");
                output.flush();
            }
            catch(IOException e)
            {
                System.out.println(e);
                System.out.println("ClientHandler Exception - User: "+name);
                dataStreamChecker.cancel();
                closeSocket();
                return;
            }
        }
    }
    private void closeSocket()
    {
        try
        {
            input.close();
            output.close();
            socket.close();
            main.removeClient(this);
        }
        catch (IOException e)
        {
            System.out.println(e);
        }
    }
    void sendTargets()
    {
        String [] targets=main.getTargetsOfUser(name);
        String toSend="targets:\n";
        for (int i=0; i<targets.length; i++)
        {
            toSend+=targets[i]+"\n";
        }
        main.sendMessageToClientsOfName(name, toSend);
    }
    void beginSendingAudioData()
    {
        sendMessage("send-audio-data");
    }
    /*
     * ToString method for a Client object.
     */
    public String toString()
    {
        return name;
    }
    public boolean equals(Client c)
    {
        if (this==c)
        {
            return true;
        }
        return false;
    }
    /*
     * Send message to the connected Client.
     */
    void sendMessage(String msg)
    {
        output.println(msg);
        output.flush();
    }
    void checkDataStream()
    {
        if (dataStreamDied)
        {
            receivingRawData = false;
            dataStreamChecker.cancel();
        }
        dataStreamDied = true;
    }
}