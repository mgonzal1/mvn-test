// $Id: ReArm.java,v 1.10 2024/09/27 18:26:16 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.StringTokenizer;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.drf3.Event;
import gov.fnal.controls.servers.dpm.drf3.PeriodicEvent;

class ReArm implements AcnetErrors
{
	final boolean enabled;
	final Event reArmDelayEvent;
	final int maxPerHour;
	final boolean reArmReady = true;

	long[] recents = null;
	long mostRecentCollection = 0;
	long earliestCollection = 0;

	ReArm(boolean enabled)
	{
		this.enabled = enabled;
		this.reArmDelayEvent = null;
		this.maxPerHour = -1;
	}

	ReArm(String rearmString) throws AcnetStatusException
	{
		try {
			final StringTokenizer tokens = new StringTokenizer(rearmString, ",=", false);

			this.enabled = new Boolean(tokens.nextToken()).booleanValue();

			if (!this.enabled) {
				this.reArmDelayEvent = null;
				this.maxPerHour = 1;
			} else {
				final StringBuffer delayString = new StringBuffer();

				tokens.nextToken(); // skip ,dly=

				String token = tokens.nextToken();

				while (!token.startsWith("nmhr")) {
					if (delayString.length() != 0)
						delayString.append(",");
					delayString.append(token);
					token = tokens.nextToken();
				}
				try {
					if (delayString.toString().equals("null"))
						this.reArmDelayEvent = null;
					else
						this.reArmDelayEvent = Event.parse(delayString.toString());
				} catch (Exception e) {
					throw new AcnetStatusException(DPM_BAD_EVENT, "ReArm, bad delay event " + delayString + ", " + e, e);
				}

				this.maxPerHour = Integer.parseInt(tokens.nextToken().trim());
			}
		} catch (Exception e) {
			throw new AcnetStatusException(DPM_BAD_EVENT, "Bad rearm event " + rearmString, e);
		}
	}

	boolean isEnabled()
	{
		return enabled;
	}

	boolean isReArmReady()
	{
		return reArmReady;
	}

	void markRecent()
	{
		final long now = System.currentTimeMillis();

		mostRecentCollection = now;

		if (maxPerHour <= 0)
			return;

		if (recents == null)
			recents = new long[maxPerHour];

		int index = 0;
		long oldestCollection = now;

		for (int ii = 0; ii < maxPerHour; ii++) {
			if (recents[ii] == 0) {
				index = ii;
				break;
			} else if (recents[ii] < oldestCollection) {
				oldestCollection = recents[ii];
				index = ii;
			}
		}
		recents[index] = now;
	}

	long getReArmTime(long now)
	{
		long reArmWait = 0;

		if (reArmDelayEvent != null && reArmDelayEvent instanceof PeriodicEvent)
			reArmWait = ((PeriodicEvent) reArmDelayEvent).getPeriod();

		if (!enabled || maxPerHour == 0) {
			earliestCollection = 0;
			return earliestCollection;
		}
		if (reArmWait == 0 && maxPerHour < 0) {
			earliestCollection = System.currentTimeMillis() - 500L;
			return earliestCollection;
		}
		long newestCollection = mostRecentCollection;
		if (maxPerHour > 0) {
			long oldestCollection = now;
			for (int ii = 0; ii < maxPerHour; ii++) {
				if (newestCollection == 0)
					newestCollection = recents[ii];
				else if (recents[ii] != 0 && recents[ii] > newestCollection)
					newestCollection = recents[ii];
				if (recents[ii] != 0 && recents[ii] < oldestCollection)
					oldestCollection = recents[ii];
			}
			long hourAgo = now - (60 * 60 * 1000);
			boolean maxedOut = true;
			for (int ii = 0; ii < maxPerHour; ii++) {
				if (recents[ii] == 0 || recents[ii] < hourAgo) {
					maxedOut = false;
					break;
				}
			}
			if (maxedOut) {
				long nextOldestCollection = now;
				for (int ii = 0; ii < maxPerHour; ii++) {
					if (recents[ii] > oldestCollection && recents[ii] < nextOldestCollection)
						nextOldestCollection = recents[ii];
				}
				if (nextOldestCollection - hourAgo > reArmWait) {
					earliestCollection = nextOldestCollection + (60 * 60 * 1000);
					return earliestCollection;
				}
			}
		}

		if (newestCollection == 0)
			earliestCollection = now;
		else
			earliestCollection = newestCollection + reArmWait;

		return earliestCollection;
	}

	@Override
	public String toString()
	{
		final StringBuilder buf = new StringBuilder();

		buf.append("rearm=" + enabled + ",dly=");

		if (reArmDelayEvent == null)
			buf.append("null");
		else
			buf.append(reArmDelayEvent);

		buf.append(",nmhr=" + maxPerHour);

		return buf.toString();
	}
}
