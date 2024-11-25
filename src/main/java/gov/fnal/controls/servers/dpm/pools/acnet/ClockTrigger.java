// $Id: ClockTrigger.java,v 1.7 2024/09/27 18:26:16 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.drf3.TimeFreq;
import gov.fnal.controls.servers.dpm.drf3.ClockType;
import gov.fnal.controls.servers.dpm.drf3.ClockEvent;
import gov.fnal.controls.servers.dpm.drf3.Event;

class ClockTrigger implements Trigger, AcnetErrors
{
	final static int NUM_ARM_EVENTS = 8;
	final static int NULL_EVENT = 0xff;

	final int[] armEvents;
	final long armDelay;
	final boolean armImmediately;

	ClockTrigger(String reconstructionString) throws AcnetStatusException
	{
		final String[] eventsAndDelay = new String[NUM_ARM_EVENTS + 1];

		try {
			final StringTokenizer tok = new StringTokenizer(reconstructionString.substring(5), ",", false); // skip trig=

			tok.nextToken();

			int ii = 0;
			while (tok.hasMoreTokens()) {
				eventsAndDelay[ii++] = tok.nextToken();
			}

			this.armDelay = Integer.parseInt(eventsAndDelay[ii - 1], 10) * 1000;
			this.armImmediately = this.armDelay < 0;
			this.armEvents = new int[NUM_ARM_EVENTS];

			for (int jj = 0; jj < ii - 1; jj++)
				this.armEvents[jj] = Integer.parseInt(eventsAndDelay[jj], 16);
		} catch (Exception e) {
			throw new AcnetStatusException(DPM_BAD_EVENT, "ClockTrigger.reconstructionString, " + reconstructionString, e);
		}
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == this)
			return true;

		if (obj instanceof ClockTrigger) {
			final ClockTrigger t = (ClockTrigger) obj;

			return t.armDelay == armDelay && t.armImmediately == t.armImmediately &&
						Arrays.equals(t.armEvents, armEvents);
		}

		return false;
	}

	@Override
	public String toString()
	{
		return getArmEvents();
	}

	@Override
	public List<Event> getArmingEvents()
	{
		final LinkedList<Event> armingEvents = new LinkedList<>();
		final int triggerArmDelay = armImmediately ? 0 : (int) (armDelay / 1000);

		for (int ii = 0; ii < NUM_ARM_EVENTS; ii++) {
			if (armEvents[ii] != NULL_EVENT) {
				ClockEvent clockEvent = new ClockEvent(armEvents[ii] & 0xff, ClockType.EITHER, new TimeFreq(triggerArmDelay));
				armingEvents.add(clockEvent);
			}
		}

		return armingEvents;
	}

	String getArmEvents()
	{
		final StringBuilder buf = new StringBuilder("e,");

		for (int ii = 0; ii < armEvents.length; ii++) {
			if (armEvents[ii] != NULL_EVENT) {
				buf.append(Integer.toHexString(armEvents[ii] & 0xff));
				buf.append(",");
			}
		}

		return buf.toString();
	}
}
