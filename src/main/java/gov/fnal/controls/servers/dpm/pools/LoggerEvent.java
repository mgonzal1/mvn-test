// $Id: LoggerEvent.java,v 1.2 2024/06/24 19:54:51 kingc Exp $
package gov.fnal.controls.servers.dpm.pools;

public class LoggerEvent
{
	final public long t1, t2;
	final public String event;
	final public int id;

	public LoggerEvent(long t1, long t2, int id, String event)
	{
		this.t1 = t1;
		this.t2 = t2;
		this.id = id;
		this.event = event;
	}

	public LoggerEvent(long t1, long t2)
	{
		this(t1, t2, -1, null);
	}
}
