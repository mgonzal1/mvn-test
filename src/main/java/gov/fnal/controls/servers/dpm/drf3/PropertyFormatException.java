//  $Id: PropertyFormatException.java,v 1.2 2023/10/04 19:38:05 kingc Exp $
package gov.fnal.controls.servers.dpm.drf3;

public class PropertyFormatException extends RequestFormatException
{
    public PropertyFormatException()
	{
        super();
    }

    public PropertyFormatException(String message)
	{
        super(message);
    }

    public PropertyFormatException(Throwable cause)
	{
        super(cause);
    }

    public PropertyFormatException(String message, Throwable cause)
	{
        super(message, cause);
    }
}
