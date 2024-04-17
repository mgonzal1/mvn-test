//  $Id: DefaultEvent.java,v 1.1 2023/10/04 19:13:42 kingc Exp $
package gov.fnal.controls.servers.dpm.drf3;

public class DefaultEvent extends Event
{
    static DefaultEvent parseDefault(String str) throws EventFormatException
	{
        if (!"U".equalsIgnoreCase( str )) {
            throw new EventFormatException( "Invalid default event: \"" + str + "\"" );
        }
        return new DefaultEvent();
    }

    public DefaultEvent() {}

    @Override
    public int hashCode()
	{
        return 'U';
    }

    @Override
    public boolean equals(Object obj)
	{
        return (obj instanceof DefaultEvent);
    }

    @Override
    public String toString()
	{
        return "U";
    }

}
