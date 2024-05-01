// $Id: DPMProtocolHandlerPC.java,v 1.20 2024/04/01 15:33:47 kingc Exp $

package gov.fnal.controls.servers.dpm.protocols.acnet.pc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.concurrent.LinkedBlockingQueue;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.service.proto.DPM;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetRequest;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

import gov.fnal.controls.servers.dpm.DPMList;
import gov.fnal.controls.servers.dpm.DPMServer;
import gov.fnal.controls.servers.dpm.protocols.Protocol;
import gov.fnal.controls.servers.dpm.protocols.HandlerType;
import gov.fnal.controls.servers.dpm.protocols.acnet.DPMProtocolHandlerAcnet;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

class AuthenticateHolder extends DPM.Request.Authenticate
{
	final AcnetRequest request;

	AuthenticateHolder(DPM.Request.Authenticate original, AcnetRequest request)
	{
		this.list_id = original.list_id;
		this.token = original.token;
		this.request = request;
	}
}

class EnableSettingsHolder extends DPM.Request.EnableSettings
{
	final AcnetRequest request;

	public EnableSettingsHolder(DPM.Request.EnableSettings original, AcnetRequest request)
	{
		this.list_id = original.list_id;
		this.MIC = original.MIC;
		this.message = original.message;
		this.request = request;
	}
}

public class DPMProtocolHandlerPC extends DPMProtocolHandlerAcnet implements AcnetErrors, DPM.Request.Receiver
{
	protected final ByteBuffer replyBuf;
	private final ServiceDiscoveryThread serviceDiscoveryThread;

	private class ServiceDiscoveryThread extends Thread
	{
		final LinkedBlockingQueue<AcnetRequest> requestQ;
		final ByteBuffer buf;
		final DPM.Reply.ServiceDiscovery msg;

		ServiceDiscoveryThread() throws AcnetStatusException
		{
			this.requestQ = new LinkedBlockingQueue<>(64);
			this.buf = ByteBuffer.allocate(16 * 1024);
			this.msg = new DPM.Reply.ServiceDiscovery();
			this.msg.serviceLocation = connection.getLocalNodeName(); 
			this.setName("Service discovery");
			this.start();
		}

		@Override
		public void run()
		{
			while (true) {
				try {
					AcnetRequest request = requestQ.take();

					msg.load = (short) DPMServer.loadPercentage();

					if (msg.load > 1)
						Thread.sleep(msg.load);

					buf.clear();
					msg.marshal(buf).flip();

					do {
						request.sendReply(buf, DPM_OK);
						buf.flip();
					} while ((request = requestQ.poll()) != null);
				} catch (InterruptedException e) {
					logger.log(Level.WARNING, "interrupt in service discovery thread", e);
				} catch (AcnetStatusException e) {
					if (e.status != ACNET_NO_SUCH) 
						logger.log(Level.FINE, "exception handling service discovery message", e);
				} catch (Exception e) {
					logger.log(Level.WARNING, "exception handling service discovery message", e);
				}
			}
		}
	}

	public DPMProtocolHandlerPC(String name) throws AcnetStatusException
	{
		super(name);

		this.replyBuf = ByteBuffer.allocate(64 * 1024);
		this.serviceDiscoveryThread = new ServiceDiscoveryThread();

		handleRequests(this);
	}

	@Override
	public Protocol protocol()
	{
		return Protocol.PC;
	}

	@Override
	public HandlerType type()
	{
		return HandlerType.Service;
	}

	@Override
	public void handle(AcnetRequest request)
	{
		try {
			this.request = request;
			DPM.Request.unmarshal(request.data()).deliverTo(this);
		} catch (Exception e) {
			logger.log(Level.FINE, "unmarshal exception", e); 
		}
	}

	private final void sendLastReply(DPM.Reply rpy) throws IOException, AcnetStatusException
	{
		replyBuf.clear();
		rpy.marshal(replyBuf).flip();
		request.sendLastReply(replyBuf, DPM_OK);
	}

	private final void sendReply(DPM.Reply rpy) throws IOException, AcnetStatusException
	{
		replyBuf.clear();
		rpy.marshal(replyBuf).flip();
		request.sendReply(replyBuf, DPM_OK);
	}

	private final String clientName() throws AcnetStatusException
	{
		return logger.isLoggable(Level.FINE) ? request.clientName() : 
									String.format("0x%x", request.client());
	}

	private final void sendLastReplyNoEx(DPM.Reply rpy)
	{
		try {
			sendLastReply(rpy);
		} catch (Exception ignore) { }
	}

	private final void sendLastStatusNoEx(int status)
	{
		final DPM.Reply.Status rpy = new DPM.Reply.Status();

		rpy.ref_id = 0;
		rpy.status = (short) status;
		sendLastReplyNoEx(rpy);
	}

	private final DPMListPC getActiveList(int listId) throws AcnetStatusException
	{
		final DPMList list = list(listId);
		
		if (list instanceof DPMListPC)
			return (DPMListPC) list;
		
		throw new AcnetStatusException(DPM_NO_SUCH_LIST);
	}

	@Override
	public void handle(DPM.Request.ServiceDiscovery m)
	{
		try {
			if (DPMList.remaining() == 0 || DPMServer.restartScheduled())
				request.ignore();
			else if (DPMServer.debug()) {
				if (request.isMulticast()) {
					request.ignore();
				} else {
					if (serviceDiscoveryThread.requestQ.offer(request))
						logger.fine("Unicast ServiceDiscovery from:" + clientName());
				}
			} else if (serviceDiscoveryThread.requestQ.offer(request))
				logger.finer("Multicast ServiceDiscovery from:" + clientName());
			else
				request.ignore();
		} catch (Exception e) {
			logger.log(Level.WARNING, "exception handling service discovery", e);
		}
	}

	@Override
	public void handle(DPM.Request.OpenList m)
	{
		try {
			final DPMListPC list = new DPMListPC(this, request);
			final DPM.Reply.OpenList reply = new DPM.Reply.OpenList();

			reply.list_id = list.id().value();

			list.handleMessage(m);
			sendReply(reply);
		} catch (IOException e) {
			logger.log(Level.WARNING, "exception handling open list message", e);
		} catch (AcnetStatusException e) {
			sendLastStatusNoEx(e.status);
		}
	}

	@Override
	public void handle(DPM.Request.Authenticate m)
	{
		try {
			getActiveList(m.list_id).handleMessage(new AuthenticateHolder(m, request));	
		} catch (AcnetStatusException e) {
			logger.log(Level.FINER, "exception handling authenticate message", e);
			sendLastStatusNoEx(e.status);
		}
	}

	@Override
	public void handle(DPM.Request.EnableSettings m)
	{
		try {
			getActiveList(m.list_id).handleMessage(new EnableSettingsHolder(m, request));
		} catch (AcnetStatusException e) {
			sendLastStatusNoEx(e.status);
		}
	}

	@Override
	public void handle(DPM.Request.AddToList m)
	{
		DPM.Reply.AddToList rpy = new DPM.Reply.AddToList();

		rpy.status = DPM_OK;
		rpy.ref_id = m.ref_id;

		try {
			getActiveList(m.list_id).handleMessage(m);
			sendLastReply(rpy);
		} catch (IOException e) {
			logger.log(Level.WARNING, "exception handling add to list", e);
		} catch (AcnetStatusException e) {
			rpy.status = (short) e.status;
			sendLastReplyNoEx(rpy);
		}
	}

	@Override
	public void handle(DPM.Request.RemoveFromList m)
	{
		DPM.Reply.RemoveFromList rpy = new DPM.Reply.RemoveFromList();

		rpy.status = DPM_OK;
		rpy.ref_id = m.ref_id;

		try {
			getActiveList(m.list_id).handleMessage(m);
			sendLastReply(rpy);
		} catch (IOException e) {
			logger.log(Level.WARNING, "exception handling remove from list", e);
		} catch (AcnetStatusException e) {
			rpy.status = (short) e.status;
			sendLastReplyNoEx(rpy);
		}
	}

	@Override
	public void handle(DPM.Request.ClearList m)
	{
		DPM.Reply.ListStatus rpy = new DPM.Reply.ListStatus();

		rpy.status = DPM_OK;
		rpy.list_id = m.list_id;

		try {
			getActiveList(m.list_id).handleMessage(m);
			sendLastReply(rpy);
		} catch (IOException e) {
			logger.log(Level.WARNING, "exception handling clear list", e);
		} catch (AcnetStatusException e) {
			rpy.status = (short) e.status;
			sendLastReplyNoEx(rpy);
		}
	}

	@Override
	public void handle(DPM.Request.StartList m)
	{
		DPM.Reply.StartList rpy = new DPM.Reply.StartList();

		rpy.status = DPM_OK;
		rpy.list_id = m.list_id;

		try {
			final DPMListPC list = getActiveList(m.list_id);

			list.handleMessage(m);
			sendLastReply(rpy);
		} catch (IOException e) {
			logger.log(Level.WARNING, "exception handling start list", e);
		} catch (AcnetStatusException e) {
			rpy.status = (short) e.status;
			sendLastReplyNoEx(rpy);
		}
	}

	@Override
	public void handle(DPM.Request.ApplySettings m)
	{
		DPM.Reply.Status rpy = new DPM.Reply.Status();

		rpy.status = DPM_OK;

		try {
			getActiveList(m.list_id).handleMessage(m);
			sendLastReply(rpy);	
		} catch (IOException e) {
			logger.log(Level.WARNING, "exception handling apply settings", e);
		} catch (AcnetStatusException e) {
			rpy.status = (short) e.status;
			sendLastReplyNoEx(rpy);
		}
	}

	@Override
	public void handle(DPM.Request.StopList m)
	{
		DPM.Reply.Status rpy = new DPM.Reply.Status();

		rpy.status = DPM_OK;

		try {
			getActiveList(m.list_id).handleMessage(m);
			sendLastReply(rpy);
		} catch (IOException e) {
			logger.log(Level.WARNING, "exception handling stop list", e);
		} catch (AcnetStatusException e) {
			rpy.status = (short) e.status;
			sendLastReplyNoEx(rpy);
		}
	}
}

