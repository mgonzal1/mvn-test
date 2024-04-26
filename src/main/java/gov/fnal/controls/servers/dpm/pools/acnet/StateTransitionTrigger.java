// $Id: StateTransitionTrigger.java,v 1.6 2024/02/22 16:32:14 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.List;
import java.util.LinkedList;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

import gov.fnal.controls.servers.dpm.events.DataEvent;
import gov.fnal.controls.servers.dpm.events.DataEventFactory;
import gov.fnal.controls.servers.dpm.events.StateEvent;

class StateTransitionTrigger implements Trigger, AcnetErrors
{
	final StateEvent event;
	final long armDelay;
	final boolean armImmediately;

	/**
	 * Constructs a StateTransitionTrigger object representing an external
	 * arming event.
	 * 
	 * @param stateEvent
	 *            state event trigger.
	 * @param immediate
	 *            arm immediately when true.
	 */
	//StateTransitionTrigger(StateEvent stateEvent, boolean immediate) {
	//	if (stateEvent == null) // when building from reconstruction string
	//	{
	//		event = null;
	//		armDelay = 0;
	//		armImmediately = false;
	//		return;
	//	}
	//	armImmediately = immediate;
	//	armDelay = (int) stateEvent.getDelay() * 1000;
	//	if (armDelay < 0) {
	//		armImmediately = true;
	//		event = new StateEvent(stateEvent.di,
	//				stateEvent.state, 0, stateEvent.flag);
	//	} else
	//		event = stateEvent;
//
//	}

	/**
	 * Constructs a StateTransitionTrigger object representing an external
	 * arming event.
     * 
	 * @param stateEvent a state event
	 */
//	StateTransitionTrigger(StateEvent stateEvent) {
//		this(stateEvent, false);
//	}

	/**
	 * Constructs a StateTransitionTrigger object from a database saved string.
	 * 
	 * @param reconstructionString
	 *            string returned by getReconstructionString().
	 * @throws AcnetStatusException
	 *             if the reconstructionString is invalid
	 */
	StateTransitionTrigger(String reconstructionString) throws AcnetStatusException
	{
		//this((StateEvent) null);

		try {
			final StateEvent tmp = (StateEvent) DataEventFactory.stringToEvent(reconstructionString.substring(5)); // skip trig=

            this.armDelay = tmp.getDelay() * 1000;

            if (armDelay < 0) {
                this.armImmediately = true;
                this.event = new StateEvent(tmp.di, tmp.state, 0, tmp.flag);
            } else {
                this.armImmediately = false;
				this.event = tmp;
			}
		} catch (Exception e) {
			//System.out.println("StateTransitionEvent, DataEventFactory: " + e);
			//e.printStackTrace();
			throw new AcnetStatusException(DPM_BAD_EVENT, "Bad StateTransitionEvent " + reconstructionString, e);
		}
	}

	/**
	 * Compare a StateTransitionTrigger for equality.
	 * 
	 * @return true when the state triggers represents the same arming
	 *         conditions
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;

		if (obj instanceof StateTransitionTrigger) {
			final StateTransitionTrigger t = (StateTransitionTrigger) obj;

			return t.event.toString().equals(event.toString());
		}

		return false;
	}

	/**
	 * Return a clone of a StateTransitionTrigger.
	 * 
	 * @return a clone of a StateTransitionTrigger
	 * 
	 */
	//public Object clone() {
	//	try {
	//		return super.clone();
	//	} catch (CloneNotSupportedException e) {
	//		System.out.println("Cannot clone StateTransitionTrigger" + e);
	//	}
	//	return null;
	//}

	/**
	 * Return a string representing this StateTransitionTrigger.
	 * 
	 * @return a string representing this StateTransitionTrigger
	 * 
	 */
	@Override
	public String toString()
	{
		return "State: " + event.toString();
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
		final LinkedList<DataEvent> armingEvents = new LinkedList<DataEvent>();

		armingEvents.add(event);

		return armingEvents;
	}

	/**
	 * Return the state event.
	 * 
	 * @return the state event
	 * 
	 */
	//StateEvent getStateEvent()
	//{
	//	return event;
	//}

	/**
	 * Inquire if arm immediately.
	 * 
	 * @return true if arm immediately
	 * 
	 */
	//boolean isArmImmediately()
	//{
	//	return armImmediately;
	//}

	/**
	 * Get the delay from arming events.
	 * 
	 * @return the delay from arming events in microseconds
	 * 
	 */
	//@Override
	//public int getArmingDelay()
	//{
	//	return armDelay;
	//}

	/**
	 * Return the arming events.
	 * 
	 * @return String listing arming events
	  */
	//String getArmEvents()
	//{
	//	return null;
	//}

	/**
	 * Return a String useful for reconstructing the StateTransitionTrigger of
	 * the form 'StateEvent: stateEvent.toString()'.
	 * 
	 * @return String describing this StateTransitionTrigger
	 */
	//public String getReconstructionString() {
	//	return "trig=" + event.toString();
	//}
}
