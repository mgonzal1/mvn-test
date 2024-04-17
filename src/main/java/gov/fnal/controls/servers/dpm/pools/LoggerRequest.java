// $Id: LoggerRequest.java,v 1.1 2023/12/13 17:04:49 kingc Exp $
package gov.fnal.controls.servers.dpm.pools;

public class LoggerRequest
{
	public final String loggedName;
	public final ReceiveData callback;

	public LoggerRequest(String loggedName, ReceiveData callback)
	{
		this.loggedName = loggedName;
		this.callback = callback;
	}

	@Override
	public String toString()
	{
		return getClass().getName() + loggedName;
	}
}
