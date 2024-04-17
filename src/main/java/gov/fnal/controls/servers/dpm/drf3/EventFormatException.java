//  $Id: EventFormatException.java,v 1.1 2023/10/04 19:13:42 kingc Exp $
package gov.fnal.controls.servers.dpm.drf3;

public class EventFormatException extends RequestFormatException
{
    public EventFormatException()
	{
        super();
    }

    public EventFormatException(String message)
	{
        super(message);
    }

    public EventFormatException(Throwable cause)
	{
        super( cause );
    }

    public EventFormatException(String message, Throwable cause)
	{
        super(message, cause);
    }
}
