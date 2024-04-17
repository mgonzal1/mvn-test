// $Id: LoggerEvent.java,v 1.1 2023/12/13 17:04:49 kingc Exp $
package gov.fnal.controls.servers.dpm.pools;

public class LoggerEvent
{
	final public long t1, t2;
	final public String event;

	public LoggerEvent(long t1, long t2, String event)
	{
		this.t1 = t1;
		this.t2 = t2;
		this.event = event;
	}
}
