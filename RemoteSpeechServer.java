
package RemoteSpeechServer;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Hashtable;
import java.util.Set;

/*
 * RemoteSpeechServer is the server side of Remote Speech. It handles sending
 * data to and handling incoming data from Clients and Targets.
 */

public class RemoteSpeechServer
{

    private ServerSocket listner;
    private Socket socket;
    private int serverPort;
    private Hashtable<String, ArrayList<Client>> clients;
    private Hashtable<String, User> users;
    private Hashtable<String, TargetHandler> connectedTargets;
    private BufferedReader input;
    private PrintWriter output;
    private FileWriter writer;


    public RemoteSpeechServer()
    {
        serverPort = 5656;
        initialize();
        eventLoop();
    }
    public RemoteSpeechServer(int inPort)
    {
        serverPort = inPort;
        initialize();
        eventLoop();
    }
    public static void main(String args[])
    {
        RemoteSpeechServer one;
        if (args.length>0)
        {
            one = new RemoteSpeechServer(Integer.valueOf(args[0]));
        }
        else
        {
            one = new RemoteSpeechServer();
        }
    }
    /*
     * Initialize the clients ArrayList and the listener ServerSocket object.
     */
    private void initialize()
    {
        users = new Hashtable<String, User>();
        Scanner in = null;
        try
        {
            File f1 = new File("users.txt");
            if (!f1.exists())
            {
                writer = new FileWriter("users.txt");
            }
            else
            {
                writer = new FileWriter("users.txt", true);
            }
            in = new Scanner(f1);
        }
        catch (IOException e)
        {
            System.out.println(e);
        }
        in.useDelimiter("\n|,");
        while (in.hasNext())
        {
            User nuser = new User(in.next(), in.next(), this);
            System.out.println("User: "+nuser);
            nuser.initTargets();
            users.put(nuser.toString(), nuser);
        }
        in.close();
        clients = new Hashtable<String, ArrayList<Client>>();
        connectedTargets = new Hashtable<String, TargetHandler>();
        try
        {
            listner = new ServerSocket(serverPort);
        }
        catch (IOException e)
        {
            System.out.println(e);
        }

    }
    /*
     * Main EventLoop, handles all incoming connections.
     */
    private void eventLoop()
    {
        while(true)
        {
            try
            {
                //Wait for a new incoming connection (THIS HOLDS THE MAIN THREAD).
                socket = listner.accept();
                System.out.println("Connection received at: "+socket);
                //Once an incoming connection is present, attempt to initalize it.
                output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                socket.setKeepAlive(true);
                socket.setSoTimeout(60000);
                ConnectionHandler ch = new ConnectionHandler(socket, input, output, this);
                ch.start();
            }
            catch (IOException e)
            {
                System.out.println(e);
            }
        }
    }
    boolean isLoginValid(String username, String password)
    {
        if (users.containsKey(username))
        {
            return users.get(username).getPassword().equals(password);
        }
        return false;
    }
    boolean isUsernameInUse(String username)
    {
        return users.containsKey(username);
    }
    String [] getTargetsOfUser(String username)
    {
        if (users.containsKey(username))
        {
            return users.get(username).getTargets();
        }
        return null;
    }
    void sendMessageToTargets(String s[], String message)
    {
        for (int i = 0; i<s.length; i++)
        {
            if (connectedTargets.containsKey(s[i]))
            {
                connectedTargets.get(s[i]).sendMessage(message);
            }
        }
    }
    void setVolumeOfTarget(String targetID, int volume)
    {
        if (connectedTargets.containsKey(targetID))
        {
            connectedTargets.get(targetID).setVolume(volume);
        }
    }
    void sendMessageToClientsOfName(String name, String message)
    {
        if (clients.containsKey(name))
        {
            ArrayList<Client> clientsOfName=clients.get(name);
            for (int i = 0; i<clientsOfName.size(); i++)
            {
                clientsOfName.get(i).sendMessage(message);
            }
        }
    }
    String getVoicesOfTarget(String targetID)
    {
        if (connectedTargets.containsKey(targetID))
        {
            return connectedTargets.get(targetID).getVoicesList();
        }
        return "";
    }
    String getVoiceOfTarget(Target t)
    {
        if (connectedTargets.containsKey(t.toString()))
        {
            return connectedTargets.get(t.toString()).getCurrentVoice();
        }
        return "";
    }
    boolean isTargetOnline(Target t)
    {
        return connectedTargets.containsKey(t.toString());
    }
    boolean doesTargetHaveUpdate(Target t)
    {
        if (connectedTargets.containsKey(t.toString()))
        {
            return connectedTargets.get(t.toString()).getUpdateStatus();
        }
        return false;
    }
    Hashtable<String, User> getUsers()
    {
        return users;
    }
    int getVolumeOfTarget(String targetID)
    {
        if (connectedTargets.containsKey(targetID))
        {
            return connectedTargets.get(targetID).getCurrentVolume();
        }
        return 50;
    }
    /*
     * Returns the number of currently connected Clients.
     */
    int getNumClients()
    {
        return clients.size();
    }
    void addNewUser(String username, String password)
    {
        User nuser = new User(username, password, this);
        users.put(nuser.toString(), nuser);
        try
        {
            writer.append("\n"+username+","+password);
            writer.flush();
        }
        catch (Exception e)
        {
            System.out.println(e);
        }
    }
    User getUser(String name)
    {
        if (users.containsKey(name))
        {
            return users.get(name);
        }
        return null;
    }
    void addClient(Client toAdd)
    {
        ArrayList<Client> c = null;
        if (clients.containsKey(toAdd.toString()))
        {
            c = clients.get(toAdd.toString());
        }
        else
        {
            c = new ArrayList<Client>();
        }
        c.add(toAdd);
        clients.put(toAdd.toString(), c);
        System.out.println("User \""+toAdd+"\" connected.");
    }
    void addTarget (TargetHandler toAdd)
    {
        connectedTargets.put(toAdd.toString(), toAdd);
        System.out.println("Target \""+toAdd+"\" connected.");
    }
    void removeClient(Client toRemove)
    {
        //Print disconnect event to console.
        System.out.println("User \""+toRemove+"\" disconnected.");
        if (clients.containsKey(toRemove.toString()))
        {
            ArrayList<Client> c = clients.get(toRemove.toString());
            if (c.size()>1)
            {
                c.remove(toRemove);
            }
            else
            {
                clients.remove(toRemove.toString());
            }
        }
    }
    void removeTarget(TargetHandler toRemove)
    {
        System.out.println("Target \""+toRemove+"\" disconnected.");
        connectedTargets.remove(toRemove.toString());
        Set<String> keys = users.keySet();
        for (String key : keys)
        {
            String [] targetIDs=users.get(key).getTargetIDs();
            for (int j = 0; j<targetIDs.length; j++)
            {
                if (toRemove.toString().equals(targetIDs[j]))
                {
                    sendMessageToClientsOfName(users.get(key).toString(), "target-status-changed");
                }
            }
        }
    }
    boolean prepareToSendAudioToTarget(String targetID, String audioFileName, int expectedFileSize, Client requestingClient)
    {
        if (connectedTargets.containsKey(targetID))
        {
            TargetHandler t = connectedTargets.get(targetID);
            if (!t.isReceivingAudio())
            {
                t.prepareToReceiveAudio(audioFileName, expectedFileSize, requestingClient);
                return true;
            }
            return false;
        }
        return false;
    }
}
