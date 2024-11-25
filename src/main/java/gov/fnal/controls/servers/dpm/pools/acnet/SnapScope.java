// $Id: SnapScope.java,v 1.9 2024/09/27 18:26:16 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

class SnapScope implements AcnetErrors
{
	final static int NUM_SAMPLE_EVENTS = 4;

	final int rate;
	final double duration;
	final boolean ratePreferred; // = false;
	final boolean durationPreferred; // = false;
	final byte[] sampleEvents;
	final boolean sampleOnClockEvent;// = false;
	final boolean sampleOnDataSamplePeriod;// = false;
	final boolean sampleOnExternalSource;// = false;
	final int sampleTriggerModifier;

	int points = 0;

	SnapScope(String reconstructionString) throws AcnetStatusException
	{
		this.sampleEvents = new byte[NUM_SAMPLE_EVENTS];

		try {
			final StringTokenizer tok = new StringTokenizer(reconstructionString, ",=", false);
			tok.nextToken(); // skip rate=

			String token = tok.nextToken();

			this.rate = (int) Double.parseDouble(token); // allow double
			tok.nextToken(); // skip dur=
			token = tok.nextToken();
			this.duration = Double.parseDouble(token);
			tok.nextToken(); // skip npts=
			token = tok.nextToken();
			this.points = Integer.parseInt(token);
			tok.nextToken(); // skip pref=
			token = tok.nextToken();

			if (token.equals("both")) {
				this.ratePreferred = true;
				this.durationPreferred = true;
			} else if (token.equals("rate")) {
				this.ratePreferred = true;
				this.durationPreferred = false;
			} else if (token.equals("dur")) {
				this.durationPreferred = true;
				this.ratePreferred = false;
			} else {
				this.durationPreferred = false;
				this.ratePreferred = false;
			}

			tok.nextToken(); // skip smpl=
			token = tok.nextToken();

			if (token.startsWith("e")) {
				this.sampleOnClockEvent = true;
				this.sampleOnDataSamplePeriod = false;
				this.sampleOnExternalSource = false;
				this.sampleTriggerModifier = 0;
				for (int ii = 0; ii < NUM_SAMPLE_EVENTS; ii++) {
					this.sampleEvents[ii] = (byte) Integer.parseInt(tok.nextToken(), 16);
					if (!tok.hasMoreTokens())
						break;
				}
			} else if (token.startsWith("p")) {
				this.sampleOnExternalSource = false;
				this.sampleOnDataSamplePeriod = true;
				this.sampleOnClockEvent = false;
				this.sampleTriggerModifier = 0;
			} else {
				this.sampleOnExternalSource = true;
				this.sampleOnClockEvent = false;
				this.sampleOnDataSamplePeriod = false;
				tok.nextToken(); // skip mod=
				token = tok.nextToken();
				this.sampleTriggerModifier = Integer.parseInt(token);
			}
		} catch (Exception e) {
			throw new AcnetStatusException(DPM_BAD_EVENT);
		}
	}

	int getRate()
	{
		return rate;
	}

	double getDuration()
	{
		return duration;
	}


	int getNumberPoints(int maxPoints)
	{
		if (points != 0) {
			if (points > maxPoints)
				points = maxPoints;
			return points;
		}
		if (sampleOnClockEvent || sampleOnExternalSource) {
			points = maxPoints;
			return points;
		}
		if (ratePreferred || durationPreferred) {
			points = maxPoints;
			return points;
		}
		points = (int) (duration * rate);
		if (points > maxPoints)
			points = maxPoints;
		return points;
	}

	int getNumberPoints()
	{
		return points;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof SnapScope) {
			final SnapScope c = (SnapScope) obj;

			if (c.sampleOnClockEvent != sampleOnClockEvent
					|| c.sampleOnDataSamplePeriod != sampleOnDataSamplePeriod
					|| c.sampleOnExternalSource != sampleOnExternalSource) {
				return false;
			}
			
			if (sampleOnClockEvent)
				for (int ii = 0; ii < NUM_SAMPLE_EVENTS; ii++)
					if (c.sampleEvents[ii] != sampleEvents[ii])
						return false;
			
			if (sampleOnExternalSource)
				if (c.sampleTriggerModifier != sampleTriggerModifier)
					return false;

			if (ratePreferred) {
				if (c.ratePreferred && c.rate == rate)
					return true;
				return false;
			}

			if (durationPreferred) {
				if (c.durationPreferred && c.duration == duration)
					return true;
				return false;
			}

			if (points == 0 || c.points == points)
				return true;
		}

		return false;
	}

	@Override
	public String toString()
	{
		return "Rate=" + rate + ", Duration=" + duration + ", points=" + points;
	}

	boolean isDurationPreferred()
	{
		return durationPreferred;
	}
	
	protected byte[] getSamplingEvents()
	{ 
		return sampleEvents; 
	}
	
	boolean isSampleOnClockEvent()
	{
		return sampleOnClockEvent;
	}

	boolean isSampleOnDataSamplePeriod()
	{
		return sampleOnDataSamplePeriod;
	}

	boolean isSampleOnExternalSource()
	{
		return sampleOnExternalSource;
	}

	int getSampleTriggerModifier()
	{
		return sampleTriggerModifier;
	}

	static SnapScope getSnapScope(String scopeAndTriggerAndReArm) throws AcnetStatusException
	{
		try {
			final String stripPrefix = scopeAndTriggerAndReArm.substring(12); // strip
			// f,type=snp,
			if (stripPrefix.equals("null"))
				return null;

			final StringTokenizer tok = new StringTokenizer(stripPrefix, ";", false);

			return new SnapScope(tok.nextToken());
		} catch (Exception e) {
			throw new AcnetStatusException(DPM_BAD_EVENT, "Invalid scope: " + scopeAndTriggerAndReArm, e);
		}
	}

	static Trigger getTrigger(String scopeAndTriggerAndReArm) throws AcnetStatusException
	{
		try {
			final String stripPrefix = scopeAndTriggerAndReArm.substring(12); // strip f,type=snap,
			final StringTokenizer tok = new StringTokenizer(stripPrefix, ";", false);

			tok.nextToken(); // skip SnapScope
			final String triggerString = tok.nextToken();

			if (triggerString.startsWith("trig=d"))
				return new ExternalTrigger(triggerString);
			else if (triggerString.startsWith("trig=x"))
				return new ExternalTrigger(triggerString);
			else if (triggerString.startsWith("trig=s"))
				return new StateTransitionTrigger(triggerString);
			else if (triggerString.startsWith("trig=e"))
				return new ClockTrigger(triggerString);
			else
				return null;
		} catch (Exception e) {
			throw new AcnetStatusException(DPM_BAD_EVENT, "Invalid trigger: " + scopeAndTriggerAndReArm, e);
		}
	}

	static ReArm getReArm(String scopeAndTriggerAndReArm) throws AcnetStatusException
	{
		try {
			final String prefix = scopeAndTriggerAndReArm.substring(12); // strip
			// f,type=snap,
			final StringTokenizer tokens = new StringTokenizer(prefix, ";", false);

			tokens.nextToken(); // skip SnapScope
			tokens.nextToken(); // skip Trigger

			final String next = tokens.nextToken();
			if (!next.equals("null"))
				return new ReArm(next);
		} catch (Exception e) {
			throw new AcnetStatusException(DPM_BAD_EVENT, "Invalid ReArm: " + scopeAndTriggerAndReArm, e);
		}

		return null;
	}
}
