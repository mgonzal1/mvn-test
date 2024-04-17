//  $Id: RangeFormatException.java,v 1.1 2023/10/04 19:38:05 kingc Exp $
package gov.fnal.controls.servers.dpm.drf3;

public class RangeFormatException extends RequestFormatException 
{
    public RangeFormatException() 
	{
        super();
    }

    public RangeFormatException(String message)
	{
        super(message);
    }

    public RangeFormatException(Throwable cause)
	{
        super(cause);
    }

    public RangeFormatException(String message, Throwable cause) 
	{
        super(message, cause);
    }
}
