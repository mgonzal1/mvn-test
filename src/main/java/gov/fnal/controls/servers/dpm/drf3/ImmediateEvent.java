//  $Id: ImmediateEvent.java,v 1.1 2023/10/04 19:13:42 kingc Exp $
package gov.fnal.controls.servers.dpm.drf3;

public class ImmediateEvent extends Event
{
    static ImmediateEvent parseImmediate(String str)  throws EventFormatException
	{
        if (!"I".equalsIgnoreCase(str))
            throw new EventFormatException( "Invalid immediate event: \"" + str + "\"" );
        
        return new ImmediateEvent();
    }

    public ImmediateEvent() {}

    @Override
    public int hashCode()
	{
        return 'I';
    }

    @Override
    public boolean equals(Object obj)
	{
        return (obj instanceof ImmediateEvent);
    }

    @Override
    public String toString()
	{
        return "I";
    }

}
