package RemoteSpeechServer;

import java.net.*;
import java.io.*;
import java.util.Timer;
import java.util.TimerTask;

public class DataTransferHandler extends Thread
{
    
    private Client requestingClient;
    private TargetHandler receivingTarget;
    private int expectedFileSize;
    private String fileName;
    
    private Timer dataStreamChecker;
    private boolean dataStreamDied = false;
    private boolean continueSendingData = true;
    
    public DataTransferHandler(Client inRequestingClient, TargetHandler inReceivingTarget, int inExpectedFileSize, String inFileName)
    {
        requestingClient = inRequestingClient;
        receivingTarget = inReceivingTarget;
        expectedFileSize = inExpectedFileSize;
        fileName = inFileName;
    }
    public void run()
    {
        try
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
            DataInputStream in = requestingClient.getDataInputStream();
            DataOutputStream out = receivingTarget.getDataOutputStream();
            requestingClient.beginSendingAudioData();
            while ((totalSize < expectedFileSize) && continueSendingData)
            {
                count = in.read(buffer);
                totalSize += count;
                out.write(buffer, 0, count);
                out.flush();
                dataStreamDied = false;
            }
            dataStreamChecker.cancel();
            requestingClient.audioSendingComplete();
            receivingTarget.audioSendingComplete();
        }
        catch (IOException e)
        {
            requestingClient.audioSendingComplete();
            receivingTarget.audioSendingComplete();
            receivingTarget.dataTransferSocketErrorOccurred();
            dataStreamChecker.cancel();
            System.err.println(e);
        }
        return;
    }
    void checkDataStream()
    {
        if (dataStreamDied)
        {
            if (dataStreamChecker != null)
            {
                dataStreamChecker.cancel();
            }
            continueSendingData = false;
            requestingClient.audioSendingComplete();
            receivingTarget.audioSendingComplete();
        }
        dataStreamDied = true;
    }
}