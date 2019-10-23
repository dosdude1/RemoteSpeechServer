
package RemoteSpeechServer;

import java.util.Scanner;
import java.io.*;
import java.util.ArrayList;

public class User
{

    private String password;
    private String username;
    private ArrayList<Target> targets;
    private FileWriter writer;
    private RemoteSpeechServer main;

    public User (String inUsername, String inPassword, RemoteSpeechServer inMain)
    {
        username=inUsername;
        password=inPassword;
        main=inMain;
        targets = new ArrayList<Target>();
        File dir = new File("users");
        if (!dir.exists())
        {
            dir.mkdir();
        }
    }
    void writeTargetsToFile()
    {
        try
        {
            writer = new FileWriter("users/"+username+".txt");
            for (int i = 0; i < targets.size(); i++)
            {
                String toWrite=targets.get(i).toString()+","+targets.get(i).getName();
                if (i<targets.size()-1)
                {
                    toWrite += "\n";
                }
                writer.write(toWrite);
            }
            writer.flush();
            writer.close();
        }
        catch (IOException e)
        {
            System.out.println(e);
        }
    }
    public String toString()
    {
        return username;
    }
    String getPassword()
    {
        return password;
    }
    void initTargets()
    {
        Scanner in = null;
        try
        {
            in = new Scanner(new File("users/"+username+".txt"));
            in.useDelimiter("\n|,");
            while (in.hasNext())
            {
                String targetID = in.next();
                String targetName = in.next();
                targets.add(new Target(targetID, targetName));
            }
            in.close();
        }
        catch (Exception e)
        {
            System.out.println("User \""+username+"\" has no targets.");
        }
    }
    void addTarget(String ID, String name)
    {
        targets.add(new Target(ID, name));
        writeTargetsToFile();
    }
    void removeTarget(String ID)
    {
        for (int i = 0; i < targets.size(); i++)
        {
            if (targets.get(i).toString().equals(ID))
            {
                targets.remove(i);
            }
        }
        writeTargetsToFile();
    }
    void renameTarget(String ID, String name)
    {
        for (int i = 0; i < targets.size(); i++)
        {
            if (targets.get(i).toString().equals(ID))
            {
                targets.get(i).setName(name);
            }
        }
        writeTargetsToFile();
    }
    String [] getTargets()
    {
        String [] targetList = new String [targets.size()];
        String targetName;
        String targetID;
        String selectedVoice;
        String onlineStatus;
        String updateStatus;
        for (int i = 0; i < targets.size(); i++)
        {
            targetName=targets.get(i).getName();
            targetID=targets.get(i).toString();
            selectedVoice=main.getVoiceOfTarget(targets.get(i));
            if (main.isTargetOnline(targets.get(i)))
            {
                onlineStatus="Online";
            }
            else
            {
                onlineStatus="Offline";
            }
            if (main.doesTargetHaveUpdate(targets.get(i)))
            {
                updateStatus="Update Available";
            }
            else
            {
                updateStatus="No";
            }
            targetList[i] = targetID + "," + targetName + "," + selectedVoice + "," + onlineStatus + "," + updateStatus;
        }
        return targetList;
    }
    String [] getTargetIDs()
    {
        String [] IDs = new String [targets.size()];
        for (int i = 0; i < targets.size(); i++)
        {
            IDs[i] = targets.get(i).toString();
        }
        return IDs;
    }
}
