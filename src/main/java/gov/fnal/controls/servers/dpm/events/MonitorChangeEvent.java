// $Id: MonitorChangeEvent.java,v 1.2 2023/06/12 16:37:55 kingc Exp $
package gov.fnal.controls.servers.dpm.events;

public class MonitorChangeEvent extends DeltaTimeEvent
{
    public MonitorChangeEvent(long m)
    {
        super(m);
    }

    public MonitorChangeEvent(long m, boolean immediate)
    {
        super(m, immediate);
    }

	public boolean sendImmediate()
	{
		return immediate;
	}
}
