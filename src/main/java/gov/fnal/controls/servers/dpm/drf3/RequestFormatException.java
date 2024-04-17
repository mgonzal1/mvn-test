//  $Id: RequestFormatException.java,v 1.1 2023/10/04 19:13:42 kingc Exp $
package gov.fnal.controls.servers.dpm.drf3;

public class RequestFormatException extends IllegalArgumentException
{
    public RequestFormatException()
	{
        super();
    }

    public RequestFormatException(String message)
	{
        super(message);
    }

    public RequestFormatException(Throwable cause)
	{
        super(cause);
    }

    public RequestFormatException(String message, Throwable cause)
	{
        super(message, cause);
    }
}
