// $Id: DPMProtocolReplier.java,v 1.7 2023/11/02 16:36:15 kingc Exp $

package gov.fnal.controls.servers.dpm;

import java.util.Collection;
import java.io.IOException;
import java.nio.ByteBuffer;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

import gov.fnal.controls.servers.dpm.scaling.DPMAnalogAlarmScaling;
import gov.fnal.controls.servers.dpm.scaling.DPMDigitalAlarmScaling;
import gov.fnal.controls.servers.dpm.scaling.DPMBasicStatusScaling;

import gov.fnal.controls.servers.dpm.pools.WhatDaq;

public interface DPMProtocolReplier extends TimeNow
{
	default public void sendStatus(int status) throws InterruptedException, IOException, AcnetStatusException
	{
		sendReply(0L, status, now(), 0L);
	}

	default public void sendStatusNoEx(int status)
	{
		try {
			sendReply(0L, status, now(), 0L);
		} catch (Exception ignore) { }
	}

	// List status reply

	default public void sendReply(ListId id, int status) throws InterruptedException, IOException, AcnetStatusException
	{
		new IOException("unimplemented method in " + this.getClass().getName());
	}

	// Kerberos authentication reply

	default public void sendReply(AuthenticateReply authenticateReply) throws InterruptedException, IOException, AcnetStatusException
	{
		new IOException("unimplemented method in " + this.getClass().getName());
	}

	// Status reply

	default public void sendReply(long refId, int status, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		new IOException("unimplemented method in " + this.getClass().getName());
	}

	default public void sendReplyNoEx(long refId, int status, long timestamp, long cycle)
	{
		try {
			sendReply(refId, status, timestamp, cycle);
		} catch (Exception ignore) { }
	}

	// Device information

	default public void sendReply(WhatDaq whatDaq) throws InterruptedException, IOException, AcnetStatusException
	{
		new IOException("unimplemented method in " + this.getClass().getName());
	}

	// Data replies

	default public void sendReply(WhatDaq whatDaq, ByteBuffer data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		new IOException("unimplemented method in " + this.getClass().getName());
	}

	default public void sendReply(WhatDaq whatDaq, byte[] data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		new IOException("unimplemented method in " + this.getClass().getName());
	}

	default public void sendReply(WhatDaq whatDaq, boolean data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		new IOException("unimplemented method in " + this.getClass().getName());
	}

	default public void sendReply(WhatDaq whatDaq, double data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		new IOException("unimplemented method in " + this.getClass().getName());
	}

	default public void sendReply(WhatDaq whatDaq, double[] data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		new IOException("unimplemented method in " + this.getClass().getName());
	}

	default public void sendReply(WhatDaq whatDaq, double[] values, long[] micros, long seqNo) throws InterruptedException, IOException, AcnetStatusException
	{
		new IOException("unimplemented method in " + this.getClass().getName());
	}

	default public void sendReply(WhatDaq whatDaq, String data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		new IOException("unimplemented method in " + this.getClass().getName());
	}

	default public void sendReply(WhatDaq whatDaq, String[] data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		new IOException("unimplemented method in " + this.getClass().getName());
	}

	default public void sendReply(WhatDaq whatDaq, DPMAnalogAlarmScaling data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		new IOException("unimplemented method in " + this.getClass().getName());
	}

	default public void sendReply(WhatDaq whatDaq, DPMDigitalAlarmScaling data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		new IOException("unimplemented method in " + this.getClass().getName());
	}

	default public void sendReply(WhatDaq whatDaq, DPMBasicStatusScaling data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		new IOException("unimplemented method in " + this.getClass().getName());
	}

	// Setting reply

	default public void sendReply(Collection<SettingStatus> settingStatus) throws InterruptedException, IOException, AcnetStatusException
	{
		new IOException("unimplemented method in " + this.getClass().getName());
	}
}
