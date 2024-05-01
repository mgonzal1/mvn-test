// $Id: AcnetStatusException.java,v 1.1 2023/11/02 16:35:17 kingc Exp $
package gov.fnal.controls.servers.dpm.acnetlib;

public class AcnetStatusException extends java.lang.Exception
{
    public final int status;

    public AcnetStatusException(int status) 
	{
		this(status, null, null);
    }

	public AcnetStatusException(int status, String message)
	{
		this(status, message, null);
	}

	public AcnetStatusException(int status, Throwable cause)
	{
		this(status, null, cause);
	}

	public AcnetStatusException(int status, String message, Throwable cause)
	{
		super("AcnetStatus:" + 
				String.format("0x%04x", status & 0xffff) + 
				(message == null ? "" : (" - " + message)), 
				cause);
		this.status = status;	
	}
}
