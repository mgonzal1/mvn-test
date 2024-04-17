//  $Id: NeverEvent.java,v 1.1 2023/10/04 19:13:42 kingc Exp $
package gov.fnal.controls.servers.dpm.drf3;

public class NeverEvent extends Event
{
    static NeverEvent parseNever(String str) throws EventFormatException
	{
        if (!"N".equalsIgnoreCase(str)) {
            throw new EventFormatException("Invalid never event: \"" + str + "\"");
        }
        return new NeverEvent();
    }

    public NeverEvent() {}

    @Override
    public int hashCode()
	{
        return 'N';
    }

    @Override
    public boolean equals(Object obj)
	{
        return (obj instanceof NeverEvent);
    }

    @Override
    public String toString()
	{
        return "N";
    }
}
