// $Id: StatSet.java,v 1.1 2024/02/22 16:29:45 kingc Exp $
package gov.fnal.controls.servers.dpm.acnetlib;

import java.nio.ByteBuffer;

class StatSet
{
	private int stats[] = new int[6];

	StatSet()
	{
		clear();
	}

	synchronized void clear()
	{
		for (int ii = 0; ii < stats.length; ii++)
			stats[ii] = 0;
	}

	synchronized void messageSent()
	{
		++stats[0];
	}

	synchronized void requestSent()
	{
		++stats[1];
	}

	synchronized void replySent()
	{
		++stats[2];
	}

	synchronized void messageReceived()
	{
		++stats[3];
	}

	synchronized void requestReceived()
	{
		++stats[4];
	}

	synchronized void replyReceived()
	{
		++stats[5];
	}

	synchronized ByteBuffer putStats(ByteBuffer buf)
	{
		for (int val : stats)
			buf.putShort((short) (val > 0xffff ? 0xffff : val));

		return buf;
	}
}
