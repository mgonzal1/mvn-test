// $Id: DPMProtocolReplierPC.java,v 1.8 2023/11/02 16:36:15 kingc Exp $
package gov.fnal.controls.servers.dpm.protocols.acnet.pc;
 
import static gov.fnal.controls.servers.dpm.DPMServer.logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

import gov.fnal.controls.service.proto.DPM;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.ListId;
import gov.fnal.controls.servers.dpm.SettingStatus;
import gov.fnal.controls.servers.dpm.AuthenticateReply;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.DPMProtocolReplier;
import gov.fnal.controls.servers.dpm.scaling.DPMBasicStatusScaling;
import gov.fnal.controls.servers.dpm.scaling.DPMAnalogAlarmScaling;
import gov.fnal.controls.servers.dpm.scaling.DPMDigitalAlarmScaling;

abstract public class DPMProtocolReplierPC implements DPMProtocolReplier
{
	abstract public void sendReply(DPM.Reply reply) throws InterruptedException, IOException, AcnetStatusException;

	@Override
    public void sendReply(long refId, int status, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		final DPM.Reply.Status reply = new DPM.Reply.Status();

		reply.ref_id = refId;
		reply.status = (short) status;
		reply.timestamp = timestamp;
		reply.cycle = cycle;

		sendReply(reply);
	}

	@Override
	synchronized public void sendReply(ListId id, int status) throws InterruptedException, IOException, AcnetStatusException
	{
		final DPM.Reply.ListStatus reply = new DPM.Reply.ListStatus();

		reply.list_id = id.value();
		reply.status = (short) status;

		sendReply(reply);
	}

	@Override
    public void sendReply(WhatDaq whatDaq) throws InterruptedException, IOException, AcnetStatusException
    {
        final DPM.Reply.DeviceInfo reply = new DPM.Reply.DeviceInfo();

        reply.ref_id = whatDaq.refId;
        reply.di = whatDaq.dInfo.di;
        reply.name = whatDaq.dInfo.name;
        reply.description = whatDaq.dInfo.description;
        reply.units = whatDaq.getUnits();

		sendReply(reply);
    }

	@Override
	public void sendReply(AuthenticateReply authenticateReply) throws InterruptedException, IOException, AcnetStatusException
	{
		final DPM.Reply.Authenticate reply = new DPM.Reply.Authenticate();

		reply.serviceName = authenticateReply.serviceName;

		if (reply.token != null)
			reply.token = authenticateReply.token;

		sendReply(reply);
	}

	@Override
	public void sendReply(WhatDaq whatDaq, byte[] data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		final DPM.Reply.Raw m = new DPM.Reply.Raw();

		m.ref_id = whatDaq.refId;
		m.timestamp = timestamp;
		m.cycle = cycle;
		m.data = data;

		sendReply(m);
	}

	@Override
	public void sendReply(WhatDaq whatDaq, ByteBuffer data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		final DPM.Reply.Raw m = new DPM.Reply.Raw();

		m.ref_id = whatDaq.refId;
		m.timestamp = timestamp;
		m.cycle = cycle;
		m.data = new byte[whatDaq.length()];
		data.get(m.data);

		sendReply(m);
	}

	@Override
	public void sendReply(WhatDaq whatDaq, boolean data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		sendReply(whatDaq, data ? 1.0 : 0.0, timestamp, cycle);
	}

	@Override
	public void sendReply(WhatDaq whatDaq, double data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		final DPM.Reply.Scalar m = new DPM.Reply.Scalar();

		m.ref_id = whatDaq.refId;
		m.timestamp = timestamp;
		m.cycle = cycle;
		m.data = data;

		sendReply(m);
	}

	@Override
	public void sendReply(WhatDaq whatDaq, double[] data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		final DPM.Reply.ScalarArray m = new DPM.Reply.ScalarArray();	

		m.ref_id = whatDaq.refId;
		m.timestamp = timestamp;
		m.cycle = cycle;
		m.status = 0;
		m.data = data;

		sendReply(m);
	}

	@Override
	public void sendReply(WhatDaq whatDaq, double[] values, long[] micros, long seqNo) throws InterruptedException, IOException, AcnetStatusException
	{
		final DPM.Reply.TimedScalarArray m = new DPM.Reply.TimedScalarArray();  

		m.ref_id = whatDaq.refId;
		m.cycle = seqNo;
		m.status = 0;
		m.timestamp = System.currentTimeMillis();

		if (values != null) {
			m.data = values;
			m.micros = micros;
		}

		sendReply(m);
	}

	@Override
	public void sendReply(WhatDaq whatDaq, String data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		final DPM.Reply.Text m = new DPM.Reply.Text();

		m.ref_id = whatDaq.refId;
		m.timestamp = timestamp;
		m.cycle = cycle;
		m.status = 0;
		m.data = data;

		sendReply(m);
	}

	@Override
	public void sendReply(WhatDaq whatDaq, String[] data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		final DPM.Reply.TextArray m = new DPM.Reply.TextArray();	

		m.ref_id = whatDaq.refId;
		m.timestamp = timestamp;
		m.cycle = cycle;
		m.status = 0;
		m.data = data;

		sendReply(m);
	}

	@Override
	public void sendReply(WhatDaq whatDaq, DPMAnalogAlarmScaling data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		final DPM.Reply.AnalogAlarm m = new DPM.Reply.AnalogAlarm();

		m.ref_id = whatDaq.refId;
		m.timestamp = timestamp;
		m.cycle = cycle;

		m.minimum = data.min();
		m.maximum = data.max();
		m.alarm_enable = data.enabled();
		m.alarm_status = data.inAlarm();
		m.abort = data.abortCapable();
		m.abort_inhibit = data.abortInhibited();
		m.tries_needed = data.triesNeeded();
		m.tries_now = data.triesNow();

		sendReply(m);
	}

	@Override
	public void sendReply(WhatDaq whatDaq, DPMDigitalAlarmScaling data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		final DPM.Reply.DigitalAlarm m = new DPM.Reply.DigitalAlarm();

		m.ref_id = whatDaq.refId;
		m.timestamp = timestamp;
		m.cycle = cycle;

		m.nominal = data.nom();
		m.mask = data.mask();
		m.alarm_enable = data.enabled();
		m.alarm_status = data.inAlarm();
		m.abort = data.abortCapable();
		m.abort_inhibit = data.abortInhibited();
		m.tries_needed = data.triesNeeded();
		m.tries_now = data.triesNow();

		sendReply(m);
	}

	@Override
	public void sendReply(WhatDaq whatDaq, DPMBasicStatusScaling data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		final DPM.Reply.BasicStatus m = new DPM.Reply.BasicStatus();

		m.ref_id = whatDaq.refId;
		m.timestamp = timestamp;
		m.cycle = cycle;

		if (data.hasOn())
			m.on = data.isOn();

		if (data.hasReady())
			m.ready = data.isReady();

		if (data.hasRemote())
			m.remote = data.isRemote();

		if (data.hasPositive())
			m.positive = data.isPositive();

		if (data.hasRamp())
			m.ramp = data.isRamp();

		sendReply(m);
	}

	private final DPM.SettingStatus newStatus(long refId, int status)
	{
		final DPM.SettingStatus s = new DPM.SettingStatus();

		s.ref_id = refId;
		s.status = (short) status;

		return s;
	}

	@Override
	public void sendReply(Collection<SettingStatus> status) throws InterruptedException, IOException, AcnetStatusException
	{
		final DPM.Reply.ApplySettings reply = new DPM.Reply.ApplySettings();

		reply.status = new DPM.SettingStatus[status.size()];

		int ii = 0;
		for (SettingStatus s : status)
			reply.status[ii++] = newStatus(s.refId, s.status());

		sendReply(reply);
	}
}
