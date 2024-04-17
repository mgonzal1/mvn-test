// $Id: ScalingProtocolHandlerPC.java,v 1.13 2024/03/05 17:43:26 kingc Exp $

package gov.fnal.controls.servers.dpm.protocols.acnet.pc;

import gov.fnal.controls.servers.dpm.DPMProtocolReplier;
import gov.fnal.controls.servers.dpm.DPMServer;
import gov.fnal.controls.servers.dpm.DataReplier;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetCancel;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetRequest;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.protocols.HandlerType;
import gov.fnal.controls.servers.dpm.protocols.Protocol;
import gov.fnal.controls.servers.dpm.protocols.acnet.DPMProtocolHandlerAcnet;
import gov.fnal.controls.servers.dpm.scaling.DPMAnalogAlarmScaling;
import gov.fnal.controls.servers.dpm.scaling.DPMBasicStatusScaling;
import gov.fnal.controls.servers.dpm.scaling.DPMDigitalAlarmScaling;
import gov.fnal.controls.service.proto.Scaling;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

public class ScalingProtocolHandlerPC extends DPMProtocolHandlerAcnet implements AcnetErrors, Runnable,
											Scaling.Request.Receiver, DPMProtocolReplier
{
	private final ByteBuffer replyBuf;
	private final LinkedBlockingQueue<AcnetRequest> requestQ;
	private final Scaling.Reply.ServiceDiscovery serviceDiscoveryReply;

	public ScalingProtocolHandlerPC(String name) throws AcnetStatusException
	{
		super(name);

		this.replyBuf = ByteBuffer.allocate(64 * 1024);
		this.requestQ = new LinkedBlockingQueue<>(16);
		this.serviceDiscoveryReply = new Scaling.Reply.ServiceDiscovery();
		this.serviceDiscoveryReply.serviceLocation = connection.getLocalNodeName(); 

		(new Thread(this, "ScalingProtocolHandlerPC")).start();

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
			request.setObject(Scaling.Request.unmarshal(request.data()));
			if (!requestQ.offer(request))
				request.ignore();
		} catch (Exception ignore) { }
	}

	@Override
	public void handle(AcnetCancel c)
	{
	}

	@Override
	public void run()
	{
		while (true) {
			try {
				this.request = requestQ.take();
				((Scaling.Request) request.getObject()).deliverTo(this);
			} catch (InterruptedException e) {
				logger.log(Level.WARNING, "exception handling request", e);
				return;	
			} catch (Exception e) {
				logger.log(Level.WARNING, "exception handling request", e);
			}
		}
	}

	@Override
	public void handle(Scaling.Request.ServiceDiscovery m)
	{
		try {
			if (!DPMServer.restartScheduled()) {
				replyBuf.clear();
				serviceDiscoveryReply.marshal(replyBuf).flip();
				request.sendReply(replyBuf, DPM_OK);
			} else
				request.ignore();
		} catch (Exception ignore) { }
	}

	@Override
	public void handle(Scaling.Request.Scale m)
	{
		try {
			final WhatDaq whatDaq = new WhatDaq(null, m.drf_request);
			final DataReplier replier = DataReplier.get(whatDaq, this);

			if (m.raw != null)
				replier.sendReply(m.raw, 0, 0, 0);
			else if (m.scaled != null)
				replier.sendReply(m.scaled, 0, 0);
		} catch (AcnetStatusException e) {
			sendStatusNoEx(e.status);	
		} catch (Exception e) {
			sendStatusNoEx(DIO_SCALEFAIL);
		}
	}

	private synchronized void sendReply(Scaling.Reply reply) throws IOException, AcnetStatusException
	{
		replyBuf.clear();
		reply.marshal(replyBuf).flip();

		request.sendReply(replyBuf, 0);
	}

	@Override
	public void sendStatusNoEx(int status)
	{
		try {
			sendReply(0, status, 0, 0);
		} catch (Exception ignore) { }
	}

	@Override
    public void sendReply(long refId, int status, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		final Scaling.Reply.Scale reply = new Scaling.Reply.Scale();

		reply.status = (short) status;

		sendReply(reply);
	}

	@Override
	public void sendReply(WhatDaq whatDaq, byte[] data, long timestamp, long cycle) throws IOException, AcnetStatusException
	{
		final Scaling.Reply.Scale reply = new Scaling.Reply.Scale();

		reply.raw = data;

		sendReply(reply);
	}

	@Override
	public void sendReply(WhatDaq whatDaq, double data, long timestamp, long cycle) throws IOException, AcnetStatusException
	{
		final Scaling.Reply.Scale reply = new Scaling.Reply.Scale();

		reply.scaled = new double[] { data };

		sendReply(reply);
	}

	@Override
	public void sendReply(WhatDaq whatDaq, double[] data, long timestamp, long cycle) throws IOException, AcnetStatusException
	{
		final Scaling.Reply.Scale reply = new Scaling.Reply.Scale();

		reply.scaled = data;

		sendReply(reply);
	}

	@Override
	public void sendReply(WhatDaq whatDaq, boolean data, long timestamp, long cycle) throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	@Override
	public void sendReply(WhatDaq whatDaq, double[] values, long[] micros, long seqNo) throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	@Override
	public void sendReply(WhatDaq whatDaq, String data, long timestamp, long cycle) throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	@Override
	public void sendReply(WhatDaq whatDaq, String[] data, long timestamp, long cycle) throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	@Override
	public void sendReply(WhatDaq whatDaq, DPMAnalogAlarmScaling data, long timestamp, long cycle) throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	@Override
	public void sendReply(WhatDaq whatDaq, DPMDigitalAlarmScaling data, long timestamp, long cycle) throws AcnetStatusException
	{ 
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	@Override
	public void sendReply(WhatDaq whatDaq, DPMBasicStatusScaling data, long timestamp, long cycle) throws AcnetStatusException
	{ 
		throw new AcnetStatusException(DIO_NOSCALE);
	}
}
