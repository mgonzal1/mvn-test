//  $Id: DeviceFormatException.java,v 1.1 2023/10/04 19:38:05 kingc Exp $
package gov.fnal.controls.servers.dpm.drf3;

public class DeviceFormatException extends RequestFormatException 
{
    public DeviceFormatException() 
	{
        super();
    }

    public DeviceFormatException(String message) 
	{
        super(message);
    }

    public DeviceFormatException(Throwable cause) 
	{
        super(cause);
    }

    public DeviceFormatException(String message, Throwable cause) 
	{
        super(message, cause);
    }
}
