// $Id: DPMListPC.java,v 1.26 2024/11/19 22:34:44 kingc Exp $
package gov.fnal.controls.servers.dpm.protocols.acnet.pc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.concurrent.LinkedBlockingQueue;

import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetRequest;
import gov.fnal.controls.service.proto.DPM;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.SettingData;
import gov.fnal.controls.servers.dpm.protocols.DPMProtocolHandler;
import gov.fnal.controls.servers.dpm.protocols.acnet.DPMListAcnet;

import static gov.fnal.controls.servers.dpm.DPMServer.debug;
import static gov.fnal.controls.servers.dpm.DPMServer.logger;

class DPMListPC extends DPMListAcnet implements Runnable, DPM.Request.Receiver
{
	final static ExecutorService executor = Executors.newWorkStealingPool();

	class DPMProtocolReplierPCImpl extends DPMProtocolReplierPC
	{
		private final AcnetRequest request;
		private final ByteBuffer replyBuf;

		DPMProtocolReplierPCImpl(AcnetRequest request)
		{
			this.request = request;
			this.replyBuf = ByteBuffer.allocate(64 * 1024);

		}

		@Override
		public synchronized void sendReply(DPM.Reply reply) throws IOException, AcnetStatusException
		{
			replyBuf.clear();
			reply.marshal(replyBuf).flip();

			request.sendReply(replyBuf, 0);
		}
	}

	final LinkedBlockingQueue<DPM.Request> msgQ;
	Future task;

	public DPMListPC(DPMProtocolHandler owner, AcnetRequest request) throws AcnetStatusException
	{
		super(owner, request);

		this.msgQ = new LinkedBlockingQueue<>();
		this.protocolReplier = new DPMProtocolReplierPCImpl(request);
		this.task = executor.submit(this);
	}

	@Override
	public void run()
	{
		DPM.Request m;

		while ((m = msgQ.poll()) != null) {
			try {
				m.deliverTo(this);
			} catch (Exception e) {
				dispose(DPM_INTERNAL_ERROR);
				logger.log(Level.WARNING, String.format("%s list - exception: %s", id(), e.toString()));		
			}
		}
	}

	final void handleMessage(DPM.Request m)
	{
		msgQ.add(m);

		if (task.isDone())
			this.task = executor.submit(this);
	}
	
	@Override
	public boolean dispose(int status)
	{
		disposedStatus = status;

		try {
			request.sendLastStatus(disposedStatus);
		} catch (Exception ignore) { }

		super.dispose(disposedStatus);

		logger.log(Level.FINE, String.format("%s disposed", id()));

		return true;
	}

	@Override
	public void handle(DPM.Request.ServiceDiscovery m)
	{
	}

	@Override
	public void handle(DPM.Request.OpenList m)
	{
		logger.log(Level.FINE, String.format("%s OpenList from:%-6s", id(), clientHostName()));
	}

	@Override
	public void handle(DPM.Request.Authenticate m)
	{
		authenticate(m.token, new DPMProtocolReplierPCImpl(((AuthenticateHolder) m).request));
	}

	@Override
	public void handle(DPM.Request.EnableSettings m)
	{
		enableSettings(m.MIC, m.message, new DPMProtocolReplierPCImpl(((EnableSettingsHolder) m).request));
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
