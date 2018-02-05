
package RemoteSpeechServer;

public class Target
{
    
    private String ID;
    private String name;
    
    public Target(String inTargetID, String inName)
    {
        ID=inTargetID;
        name=inName;
    }
    public String toString()
    {
        return ID;
    }
    public String getName()
    {
        return name;
    }
    void setName(String s)
    {
        name=s;
    }
}