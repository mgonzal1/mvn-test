// $Id: StateTransitionTrigger.java,v 1.8 2024/09/27 18:26:16 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.List;
import java.util.LinkedList;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

import gov.fnal.controls.servers.dpm.drf3.Event;
import gov.fnal.controls.servers.dpm.drf3.TimeFreq;
import gov.fnal.controls.servers.dpm.drf3.StateEvent;

class StateTransitionTrigger implements Trigger, AcnetErrors
{
	final StateEvent event;
	final long armDelay;
	final boolean armImmediately;

	StateTransitionTrigger(String reconstructionString) throws AcnetStatusException
	{
		try {
			final StateEvent tmp = (StateEvent) Event.parse(reconstructionString.substring(5)); // skip trig=

            this.armDelay = tmp.getDelay() * 1000;

            if (armDelay < 0) {
                this.armImmediately = true;
                this.event = new StateEvent(tmp.getDevice(), tmp.getValue(), new TimeFreq(0), tmp.getExpression());
            } else {
                this.armImmediately = false;
				this.event = tmp;
			}
		} catch (Exception e) {
			throw new AcnetStatusException(DPM_BAD_EVENT, "Bad StateTransitionEvent " + reconstructionString, e);
		}
	}

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

	@Override
	public String toString()
	{
		return "State: " + event;
	}

	@Override
	public List<Event> getArmingEvents()
	{
		final LinkedList<Event> events = new LinkedList<>();

		events.add(event);

		return events;
	}
}
