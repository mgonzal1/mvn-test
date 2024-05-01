// $Id: ClockTrigger.java,v 1.5 2024/02/22 16:32:14 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;

import gov.fnal.controls.servers.dpm.events.ClockEvent;
import gov.fnal.controls.servers.dpm.events.DataEvent;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

class ClockTrigger implements Trigger, AcnetErrors
{
	final static int NUM_ARM_EVENTS = 8;
	final static int NULL_EVENT = 0xff;

	final int[] armEvents;
	final long armDelay;
	final boolean armImmediately;

	//List<DataEvent> armingEvents = null;

/*
	ClockTrigger()
	{
		this(null, 0, true);
	}

	ClockTrigger(byte event, int delay)
	{
		this.armEvents = new byte[NUM_ARM_EVENTS];

		for (int ii = 0; ii < NUM_ARM_EVENTS; ii++)
			this.armEvents[ii] = NULL_EVENT;

		this.armEvents[0] = event;
		this.armDelay = delay;
		this.armImmediately = armDelay < 0;
	}

	ClockTrigger(byte[] events, int delay)
	{
		this(events, delay, false);
	}
	*/

	/**
	 * Constructs a ClassTrigger object from a database saved string. Of the
	 * form '0x2b,2a,0',1000 where 2b, 2a, and 0 are Tevatron clock events and
	 * 1000 is the delay (measured in milliseconds)
	 * 
	 * @param reconstructionString
	 *            string returned by getReconstructionString().
	 * @throws AcnetStatusException
	 *             if the reconstructionString is invalid
	 */
	ClockTrigger(String reconstructionString) throws AcnetStatusException
	{
		//this(null, 0, false);

		final String[] eventsAndDelay = new String[NUM_ARM_EVENTS + 1];

		// get events and delay
		try {
			final StringTokenizer tok = new StringTokenizer(reconstructionString.substring(5), ",", false); // skip trig=

			tok.nextToken(); // skip e,

			int ii = 0;
			while (tok.hasMoreTokens()) {
				eventsAndDelay[ii++] = tok.nextToken();
			}

			this.armDelay = Integer.parseInt(eventsAndDelay[ii - 1], 10) * 1000;
			this.armImmediately = this.armDelay < 0;
			this.armEvents = new int[NUM_ARM_EVENTS];

			// --ii; there is no 'e, s, or h" -- skip 'e,s, or h'

			for (int jj = 0; jj < ii - 1; jj++)
				this.armEvents[jj] = Integer.parseInt(eventsAndDelay[jj], 16);
		} catch (Exception e) {
			throw new AcnetStatusException(DPM_BAD_EVENT, "ClockTrigger.reconstructionString, " + reconstructionString, e);
		}
	}

/*
	ClockTrigger(byte[] events, int delay, boolean immediately)
	{
		armEvents = new byte[NUM_ARM_EVENTS];

		for (int ii = 0; ii < NUM_ARM_EVENTS; ii++) {
			if (events != null && events.length > ii)
				armEvents[ii] = events[ii];
			else
				armEvents[ii] = NULL_EVENT;
		}
		armDelay = delay;
		armImmediately = immediately;
        if (armDelay < 0) 
			armImmediately = true;
	}
*/

	@Override
	public boolean equals(Object obj)
	{
		if (obj == this)
			return true;

		// System.out.println("ClockTrigger compare");
		if (obj instanceof ClockTrigger) {
			final ClockTrigger t = (ClockTrigger) obj;

			return t.armDelay == armDelay && t.armImmediately == t.armImmediately &&
						Arrays.equals(t.armEvents, armEvents);
					
			//if (compare.armDelay != armDelay)
			//	return false;
			
			//if (compare.armImmediately != armImmediately)
			//	return false;
			

			//for (int ii = 0; ii < NUM_ARM_EVENTS; ii++)
			//	if (compare.armEvents[ii] != armEvents[ii])
			//		return false;

			//return true;
		}

		return false;
	}

	/**
	 * Return a clone of a ClockTrigger.
	 * 
	 * @return a clone of a ClockTrigger
	 * 
	 */
	//@Override
	//public Object clone() {
	//	try {
	//		return super.clone();
	//	} catch (CloneNotSupportedException e) {
	//		System.out.println("Cannot clone ClockTrigger" + e);
	//	}
	//	;
	//	return null;
	//}

	/**
	 * Return a string representing this ClockTrigger.
	 * 
	 * @return a string representing this ClockTrigger
	 * 
	 */
	@Override
	public String toString()
	{
		return getArmEvents();
	}

	/**
	 * Return the arming events.
	 * 
	 * @return the arming events
	 * 
	 */
	@Override
	public List<DataEvent> getArmingEvents()
	{
		//if (armingEvents == null) {
			final LinkedList<DataEvent> armingEvents = new LinkedList<>();
			final int triggerArmDelay = armImmediately ? 0 : (int) (armDelay / 1000);

			//if (armImmediately)
			//	triggerArmDelay = 0; // delay is microseconds or samples till
										// stop collection
			for (int ii = 0; ii < NUM_ARM_EVENTS; ii++) {
				if (armEvents[ii] != NULL_EVENT) {
					ClockEvent clockEvent = new ClockEvent(armEvents[ii] & 0xff, true, triggerArmDelay, true);
					armingEvents.add(clockEvent);
				}
			}
		//}

		return armingEvents;
	}

	//@Override
	//public boolean isArmImmediately()
	//{
	//	return armImmediately;
	//}

	//@Override
	//public int getArmingDelay()
	//{
	//	return armDelay;
	//}
	
	//byte[] getArmingEventArray()
	//{	
	//	return armEvents;
	//}

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

	/**
	 * Return a String useful for reconstructing the ClockTrigger of the form
	 * '0x2b,2a,0',1000
	 * 
	 * @return String describing this ClockTrigger
	 */
	//public String getReconstructionString()
	//{
	////	return "trig=" + getArmEvents() + armDelay/1000;
	//}
}
