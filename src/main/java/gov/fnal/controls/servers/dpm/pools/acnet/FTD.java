package gov.fnal.controls.servers.dpm.pools.acnet;

import gov.fnal.controls.servers.dpm.drf3.Event;
import gov.fnal.controls.servers.dpm.drf3.PeriodicEvent;
import gov.fnal.controls.servers.dpm.drf3.ClockEvent;
import gov.fnal.controls.servers.dpm.drf3.NeverEvent;

public class FTD
{
	public static int forEvent(Event event)
	{
		if (event instanceof PeriodicEvent) {
			final long ftd = 60 * ((PeriodicEvent) event).getPeriodValue().getTimeMillis() / 1000;

			return (int) (ftd < 4 ? 4 : ftd);
		}

		if (event instanceof ClockEvent) {
			final ClockEvent tmp = (ClockEvent) event;

			return tmp.getDelayValue().getTimeMillis() == 0 ? (tmp.getEventNumber() | 0x8000) : 0;
		}

		if (event instanceof NeverEvent)
			return 2;

		return 0;
	}
}
