// $Id: DPMListJSON.java,v 1.19 2023/11/02 16:36:15 kingc Exp $
package gov.fnal.controls.servers.dpm.protocols.tcp.json;
 
import java.util.Collection;
import java.util.logging.Level;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

import gov.fnal.controls.servers.dpm.ListId;
import gov.fnal.controls.servers.dpm.SettingStatus;
import gov.fnal.controls.servers.dpm.DPMProtocolReplier;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.protocols.DPMProtocolHandler;
import gov.fnal.controls.servers.dpm.protocols.tcp.DPMListTCP;
import gov.fnal.controls.servers.dpm.protocols.tcp.RemoteClientTCP;
import gov.fnal.controls.servers.dpm.scaling.DPMBasicStatusScaling;
import gov.fnal.controls.servers.dpm.scaling.DPMAnalogAlarmScaling;
import gov.fnal.controls.servers.dpm.scaling.DPMDigitalAlarmScaling;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

public class DPMListJSON extends DPMListTCP implements AcnetErrors, DPMProtocolReplier
{
	static final Charset UTF8 = Charset.forName("UTF-8");

	static class Reply
	{
		void marshal(ByteBuffer buf)
		{
			final GsonBuilder b = new GsonBuilder();

			b.setPrettyPrinting();
			Gson gson = b.create();

			String s = gson.toJson(this, this.getClass());

			buf.put(s.getBytes(UTF8));
		}

		static class OpenList extends Reply
		{
			int listId;
		}

		static class ListStatus extends Reply
		{
			int listId;
			int status;
		}

		static class Status extends Reply
		{
			long refId;
			long timestamp;
			long cycle;
			int status;
		}

        static class DeviceInfo extends Reply
		{
            long refId;
            int di;
            java.lang.String name;
            java.lang.String description;
            java.lang.String units;
            short format_hint;
		}

		static class Raw extends Reply
		{
			long refId;
			long timestamp;
			long cycle;
			byte[] data;
		}

		static class Scalar extends Reply
		{
			long refId;
			long timestamp;
			long cycle;
			int status;
			double data;
		}

		static class ScalarArray extends Reply
		{
			long refId;
			long timestamp;
			long cycle;
            int status;
			double[] data;
		}

		static class Text extends Reply
		{
			long refId;
			long timestamp;
			long cycle;
			int status;
			String data;
		}

		static class TextArray extends Reply
		{
			long refId;
			long timestamp;
			long cycle;
			int status;
			String[] data;
		}

        static class BasicStatus extends Reply
		{
            long refId;
            long timestamp;
            long cycle;
            boolean on;
            boolean ready;
            boolean remote;
            boolean positive;
            boolean ramp;
		}

        static class DigitalAlarm extends Reply
		{
            long refId;
            long timestamp;
            long cycle;
            int nominal;
            int mask;
            boolean alarm_enable;
            boolean alarm_status;
            boolean abort;
            boolean abort_inhibit;
            int tries_needed;
            int tries_now;
		}

        static class AnalogAlarm extends Reply
		{
            long refId;
            long timestamp;
            long cycle;
            double minimum;
            double maximum;
            boolean alarm_enable;
            boolean alarm_status;
            boolean abort;
            boolean abort_inhibit;
            int tries_needed;
            int tries_now;
		}

        static class TimedScalarArray extends Reply
		{
            long refId;
            long timestamp;
            long cycle;
            int status;
            double[] data;
            long[] micros;
		}

        static class Authenticate extends Reply
		{
            String serviceName;
            byte[] token;
		}

        static class ApplySettings extends Reply
		{
            SettingStatus[] status;
		}
	}

	final ConcurrentLinkedQueue<Reply> replyQ = new ConcurrentLinkedQueue<>();

	public DPMListJSON(DPMProtocolHandler owner, RemoteClientTCP client) throws IOException, AcnetStatusException
	{
		super(owner, client);

		this.protocolReplier = this;

		final Reply.OpenList m = new Reply.OpenList();

		m.listId = id().value();
		sendReply(m);

		this.owner.listCreated(this);
	}

	private final void addJsonElement(JsonElement je, long refId)
	{
		if (je.isJsonPrimitive())
			addRequest(je.getAsString(), refId);
		else
			addRequest(je.getAsJsonObject().getAsJsonPrimitive("request").getAsString(), refId);
	}

	@Override
	protected void handleRequest(final ByteBuffer buf) throws IOException
	{
		final String json = UTF8.decode(buf).toString();

		logger.log(Level.FINE, "json: '" + json + "'");

		final JsonElement root = JsonParser.parseString(json);

		long refId = 0;

		if (root.isJsonArray()) {
			for (JsonElement je : root.getAsJsonArray())
				addJsonElement(je, refId++);	
		} else if (root.isJsonObject()) {
			for (JsonElement je : root.getAsJsonObject().getAsJsonArray("requests"))
				addJsonElement(je, refId++);	
		} else if (root.isJsonPrimitive())
			addJsonElement(root, refId++);

		start(null);
	}

	@Override
	protected boolean getReply(final ByteBuffer buf) throws IOException
	{
		try {
			final Reply reply = replyQ.poll();

			if (reply != null) {
				reply.marshal(buf);
				return true;
			}
		} catch (Exception e) {
			dispose(ACNET_DISCONNECTED);
		}

		return false;
	}

	private void sendReply(Reply reply)
	{
		replyQ.offer(reply);
		wakeup();
	}

	private void sendReplyNoEx(Reply rpy)
	{
		try {
			sendReply(rpy);
		} catch (Exception ignore) { }
	}

	@Override
	public boolean dispose(int status)
	{
		if (super.dispose(status)) {
			logger.finer("dispose queue size:" + replyQ.size());
			return true;
		}

		return false;
	}

	@Override
	synchronized public void sendReply(ListId id, int status)
	{
		final Reply.ListStatus m = new Reply.ListStatus();

		m.listId = id().value();
		m.status = (short) 0;

		sendReply(m);
	}

	@Override
    public void sendReply(WhatDaq whatDaq)
    {
        final Reply.DeviceInfo m = new Reply.DeviceInfo();

        m.refId = whatDaq.refId;
        m.di = whatDaq.dInfo.di;
        m.name = whatDaq.dInfo.name;
        m.description = whatDaq.dInfo.description;
        m.units = whatDaq.getUnits();

		sendReply(m);
    }

	@Override
	public void sendReply(WhatDaq whatDaq, byte[] data, long timestamp, long cycle)
	{
		final Reply.Raw m = new Reply.Raw();

		m.refId = whatDaq.refId;
		m.timestamp = timestamp;
		m.cycle = cycle;
		m.data = data;

		sendReply(m);
	}

	@Override
	public void sendReply(WhatDaq whatDaq, boolean data, long timestamp, long cycle)
	{
		sendReply(whatDaq, data ? 1.0 : 0.0, timestamp, cycle);
	}

	@Override
	public void sendReply(WhatDaq whatDaq, double data, long timestamp, long cycle)
	{
		final Reply.Scalar m = new Reply.Scalar();

		m.refId = whatDaq.refId;
		m.timestamp = timestamp;
		m.cycle = cycle;
		m.data = data;

		sendReply(m);
	}

	@Override
	public void sendReply(WhatDaq whatDaq, double[] data, long timestamp, long cycle)
	{
		final Reply.ScalarArray m = new Reply.ScalarArray();	

		m.refId = whatDaq.refId;
		m.timestamp = timestamp;
		m.cycle = cycle;
		m.data = data;

		sendReply(m);
	}

	@Override
	public void sendReply(WhatDaq whatDaq, double[] values, long[] micros, long seqNo)
	{
		final Reply.TimedScalarArray m = new Reply.TimedScalarArray();  

		m.refId = whatDaq.refId;
		m.cycle = seqNo;
		m.timestamp = System.currentTimeMillis();

		if (values != null) {
			m.data = values;
			m.micros = micros;
		}

		sendReply(m);
	}

	@Override
	public void sendReply(WhatDaq whatDaq, String data, long timestamp, long cycle)
	{
		final Reply.Text m = new Reply.Text();

		m.refId = whatDaq.refId;
		m.timestamp = timestamp;
		m.cycle = cycle;
		m.data = data;

		sendReply(m);
	}

	@Override
	public void sendReply(WhatDaq whatDaq, String[] data, long timestamp, long cycle)
	{
		final Reply.TextArray m = new Reply.TextArray();	

		m.refId = whatDaq.refId;
		m.timestamp = timestamp;
		m.cycle = cycle;
		m.data = data;

		sendReply(m);
	}

	@Override
	public void sendReply(WhatDaq whatDaq, DPMAnalogAlarmScaling data, long timestamp, long cycle) throws IOException, AcnetStatusException
	{
		final Reply.AnalogAlarm m = new Reply.AnalogAlarm();

		m.refId = whatDaq.refId;
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
	public void sendReply(WhatDaq whatDaq, DPMDigitalAlarmScaling data, long timestamp, long cycle) throws IOException, AcnetStatusException
	{
		final Reply.DigitalAlarm m = new Reply.DigitalAlarm();

		m.refId = whatDaq.refId;
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
	public void sendReply(WhatDaq whatDaq, DPMBasicStatusScaling data, long timestamp, long cycle) throws IOException, AcnetStatusException
	{
		final Reply.BasicStatus m = new Reply.BasicStatus();

		m.refId = whatDaq.refId;
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
}
