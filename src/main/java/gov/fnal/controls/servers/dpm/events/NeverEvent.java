// $Id: NeverEvent.java,v 1.2 2023/06/12 16:37:55 kingc Exp $
package gov.fnal.controls.servers.dpm.events;

public class NeverEvent implements DataEvent
{
	public String toString()
	{
		return "n";
	}

	public int ftd()
	{
		return 2;
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof NeverEvent;
	}
}

