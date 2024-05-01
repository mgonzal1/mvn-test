// $Id: DPMListPC.java,v 1.19 2023/11/02 16:36:15 kingc Exp $
package gov.fnal.controls.servers.dpm.protocols.tcp.pc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.concurrent.ConcurrentLinkedQueue;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.service.proto.DPM;

import gov.fnal.controls.servers.dpm.SettingData;
import gov.fnal.controls.servers.dpm.protocols.DPMProtocolHandler;
import gov.fnal.controls.servers.dpm.protocols.tcp.DPMListTCP;
import gov.fnal.controls.servers.dpm.protocols.tcp.RemoteClientTCP;
import gov.fnal.controls.servers.dpm.protocols.acnet.pc.DPMProtocolReplierPC;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

public class DPMListPC extends DPMListTCP implements AcnetErrors, DPM.Request.Receiver
{
	final ConcurrentLinkedQueue<DPM.Reply> replyQ = new ConcurrentLinkedQueue<>();

	class DPMProtocolReplierPCImpl extends DPMProtocolReplierPC
	{
		@Override
		public void sendReply(DPM.Reply reply)
		{
			replyQ.offer(reply);
			wakeup();
		}
	}

	public DPMListPC(DPMProtocolHandler owner, RemoteClientTCP client) throws IOException, AcnetStatusException
	{
		super(owner, client);

		this.protocolReplier = new DPMProtocolReplierPCImpl();

		final DPM.Reply.OpenList m = new DPM.Reply.OpenList();

		m.list_id = id().value();
		sendReply(m);

		this.owner.listCreated(this);
	}

	@Override
	protected void handleRequest(final ByteBuffer buf)
	{
        try {                                                                                                                                                       
           	DPM.Request.unmarshal(buf).deliverTo(this);
        } catch (Exception e) {
			dispose(ACNET_DISCONNECTED);
		}
	}

	synchronized void sendReply(DPM.Reply reply)
	{
		replyQ.offer(reply);
		wakeup();
	}

	@Override
	protected boolean getReply(final ByteBuffer buf)
	{
		try {
			final DPM.Reply reply = replyQ.poll();

			if (reply != null) {
				reply.marshal(buf);
				return true;
			}
		} catch (Exception e) {
			dispose(ACNET_DISCONNECTED);
		}

		return false;
	}

	private void sendReplyNoEx(DPM.Reply rpy)
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
	public void handle(DPM.Request.ServiceDiscovery m)
	{
	}

	@Override
	public void handle(DPM.Request.OpenList m)
	{
	}

	@Override
	public void handle(DPM.Request.Authenticate m)
	{
		authenticate(m.token);
	}

	@Override
	public void handle(DPM.Request.EnableSettings m)
	{
		enableSettings(m.MIC, m.message);
	}

	@Override
	public void handle(DPM.Request.AddToList m)
	{
		addRequest(m.drf_request, m.ref_id);
	}

	@Override
	public void handle(DPM.Request.RemoveFromList m)
	{
		removeRequest(m.ref_id);
	}

	@Override
	public void handle(DPM.Request.ClearList m)
	{
		clear();
	}

	@Override
	public void handle(DPM.Request.StartList m)
	{
		start(m.model);

		final DPM.Reply.StartList rpy = new DPM.Reply.StartList();

		rpy.status = DPM_OK;
		rpy.list_id = id().value();

		replyQ.offer(rpy);
		wakeup();
	}

	@Override
	public void handle(DPM.Request.StopList m)
	{
		stop();
	}

	@Override
	public void handle(DPM.Request.ApplySettings m)
	{
		if (m.raw_array != null) {
			for (DPM.RawSetting s : m.raw_array)
				addSetting(SettingData.create(s.data, s.ref_id));
		}

		if (m.scaled_array != null) {
			for (DPM.ScaledSetting s : m.scaled_array)
				addSetting(SettingData.create(s.data, s.ref_id));
		}

		if (m.text_array != null) {
			for (DPM.TextSetting s : m.text_array)
				addSetting(SettingData.create(s.data, s.ref_id));
		}

		applySettings();
	}
}
