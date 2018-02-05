
package RemoteSpeechServer;

import java.net.*;
import java.io.*;
import java.util.ArrayList;

public class ConnectionHandler extends Thread
{
    
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private RemoteSpeechServer main;
    
    public ConnectionHandler(Socket inSocket, BufferedReader in, PrintWriter out, RemoteSpeechServer inMain)
    {
        socket=inSocket;
        input=in;
        output=out;
        main=inMain;
    }
    public void run()
    {
        boolean isInitialized=false;
        while (!isInitialized)
        {
            try
            {
                input.ready();
                String read=input.readLine();
                if (read != null && read.contains("loginas:"))
                {
                    String username=read.substring(read.indexOf("loginas:")+8, read.indexOf(","));
                    String password=read.substring(read.indexOf(",")+1);
                    
                    if (main.isLoginValid(username, password))
                    {
                        //If username is not in use, finish initialization and add
                        //connecting Client to ArrayList.
                        output.println("login-valid");
                        output.flush();
                        Client c1=new Client(socket, input, output, username, main);
                        main.addClient(c1);
                        
                        //Start new Client handler on a second thread.
                        c1.start();
                        //Print event to console.
                        isInitialized=true;
                        return;
                    }
                    else
                    {
                        output.println("err-credentials-not-valid");
                        output.flush();
                    }
                }
                else if (read != null && read.contains("is-target:"))
                {
                    output.println("success");
                    output.flush();
                    
                    TargetHandler t1=new TargetHandler(socket, input, output, read.substring(read.indexOf(":")+1), main);
                    t1.start();
                    main.addTarget(t1);
                    
                    
                    //Print event to console.
                    isInitialized=true;
                    return;
                }
                else if (read != null && read.contains("newuser:"))
                {
                    
                    String username=read.substring(read.indexOf("newuser:")+8, read.indexOf(","));
                    String password=read.substring(read.indexOf(",")+1);
                    if (!main.isUsernameInUse(username))
                    {
                        output.println("user-created");
                        output.flush();
                        main.addNewUser(username, password);
                        
                        Client c1=new Client(socket, input, output, username, main);
                        main.addClient(c1);
                        
                        //Start new Client handler on a second thread.
                        c1.start();
                        
                        //Print event to console.
                        System.out.println("User \""+username+"\" created.");
                        isInitialized=true;
                        return;
                    }
                    else
                    {
                        output.println("err-un-in-use");
                        output.flush();
                    }
                }
                else
                {
                    System.out.println("Connection Terminated - No Init");
                    isInitialized=true;
                    return;
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
                closeSocket();
                return;
            }
        }
    }
    void closeSocket()
    {
        try
        {
            input.close();
            output.close();
            socket.close();
        }
        catch(IOException e)
        {
            System.out.println(e);
        }
    }
}