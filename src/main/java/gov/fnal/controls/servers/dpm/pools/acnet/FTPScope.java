// $Id: FTPScope.java,v 1.5 2024/02/22 16:32:14 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.StringTokenizer;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.events.DeltaTimeEvent;

class FTPScope implements AcnetErrors
{
	// 0 => none, 1 => MR resets, 2 => TEV resets
	final int groupEventCode;
	final int clockEventNumber;
	final boolean onGroupEventCode;
	final double duration;
	final double rate;

	FTPScope(double userRate)
	{
		this(userRate, (byte) 0, true, Double.MAX_VALUE);
	}

	FTPScope(double rate, byte eventCode, boolean onGroup, double duration)
	{
		this.rate = rate;

		if (onGroup) {
			this.onGroupEventCode = true;
			this.clockEventNumber = 0xfe;
			this.groupEventCode = eventCode;
		} else {
			this.onGroupEventCode = false;
			this.clockEventNumber = eventCode;
			this.groupEventCode = 0;
		}

		this.duration = duration;
	}

	FTPScope(String reconstructionString)
	{
		final StringTokenizer tok = new StringTokenizer(reconstructionString, ",=", false);

		this.groupEventCode = 0;
		this.clockEventNumber = 0xfe;
		this.onGroupEventCode = true;

		tok.nextToken(); // skip rate=
		this.rate = Double.parseDouble(tok.nextToken());
		tok.nextToken(); // skip dur=
		this.duration = Double.parseDouble(tok.nextToken());
	}

	double getRate()
	{
		return rate;
	}

	//@Override
	//public Object clone()
	//{
	//	try {
	//		return super.clone();
	//	} catch (CloneNotSupportedException e) {
	//		System.out.println("Cannot clone FTPScope" + e);
	//	}
		
	//	return null;
	//}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == this)
			return true;

		if (obj instanceof FTPScope) {
			final FTPScope compare = (FTPScope) obj;

			if (compare.rate == rate && compare.duration == duration && compare.onGroupEventCode == onGroupEventCode) {
				if (onGroupEventCode)
					return compare.groupEventCode == groupEventCode;
				else
					return compare.clockEventNumber == clockEventNumber;
			}
		}
		return false;
	}

	@Override
	public String toString()
	{
		if (onGroupEventCode && groupEventCode == 0)
			return "FTPScope, rate " + rate + ", duration: " + duration;
		else
			return "FTPScope event 0x" + Integer.toHexString(clockEventNumber & 0xff)
						+ ", groupEventCode " + groupEventCode + ", is on group: " + onGroupEventCode + ", duration: " + duration;
	}

	int getGroupEventCode()
	{
		return groupEventCode;
	}

	double getDuration()
	{
		return duration;
	}

/*
	void setDuration(double duration)
	{
		this.duration = duration;
	}
*/

	DeltaTimeEvent ftpCollectionEvent(int maxRate)
	{
		double plotRate = maxRate;

		if (rate < plotRate)
			plotRate = rate;

		long milliSecs = (long) ((1000. / plotRate) + 0.5);

		return new DeltaTimeEvent(milliSecs);
	}

	//String getReconstructionString()
	//{
	//	return "rate=" + rate + ",dur=" + duration;
	//}

	static FTPScope getFTPScope(String scopeAndTriggerAndReArm) throws AcnetStatusException
	{
		try {
			final String stripPrefix = scopeAndTriggerAndReArm.substring(12); // strip
																		// f,type=ftp,
			if (stripPrefix.equals("null"))
				return null;

			final StringTokenizer tok = new StringTokenizer(stripPrefix, ";", false);

			return new FTPScope(tok.nextToken());
		} catch (Exception e) {
			throw new AcnetStatusException(DPM_BAD_EVENT, "Invalid scope: " + scopeAndTriggerAndReArm, e);
		}
	}
}
