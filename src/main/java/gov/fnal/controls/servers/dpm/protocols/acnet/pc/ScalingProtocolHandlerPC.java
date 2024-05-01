// $Id: ScalingProtocolHandlerPC.java,v 1.16 2024/04/01 15:33:47 kingc Exp $

package gov.fnal.controls.servers.dpm.protocols.acnet.pc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.logging.Level;
import java.sql.SQLException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;

import gov.fnal.controls.service.proto.Scaling;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetCancel;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetRequest;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.DPMServer;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.pools.DeviceCache;
import gov.fnal.controls.servers.dpm.DPMProtocolReplier;
import gov.fnal.controls.servers.dpm.DataReplier;
import gov.fnal.controls.servers.dpm.SettingStatus;
import gov.fnal.controls.servers.dpm.protocols.Protocol;
import gov.fnal.controls.servers.dpm.protocols.HandlerType;
import gov.fnal.controls.servers.dpm.protocols.acnet.DPMProtocolHandlerAcnet;
import gov.fnal.controls.servers.dpm.scaling.DPMBasicStatusScaling;
import gov.fnal.controls.servers.dpm.scaling.DPMAnalogAlarmScaling;
import gov.fnal.controls.servers.dpm.scaling.DPMDigitalAlarmScaling;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

public class ScalingProtocolHandlerPC extends DPMProtocolHandlerAcnet implements AcnetErrors, Runnable, DPMProtocolReplier
{
	private final ByteBuffer replyBuf;
	private final LinkedBlockingQueue<AcnetRequest> requestQ;
	private final Scaling.Reply.ServiceDiscovery serviceDiscoveryReply;
	private Future task;

	public ScalingProtocolHandlerPC(String name) throws AcnetStatusException
	{
		super(name);

		this.replyBuf = ByteBuffer.allocate(64 * 1024);
		this.requestQ = new LinkedBlockingQueue<>(256);
		this.serviceDiscoveryReply = new Scaling.Reply.ServiceDiscovery();
		this.serviceDiscoveryReply.serviceLocation = connection.getLocalNodeName(); 
		this.task = DPMListPC.executor.submit(this);

		//(new Thread(this, "ScalingProtocolHandlerPC")).start();

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

			if (task.isDone())
				this.task = DPMListPC.executor.submit(this);
		} catch (Exception e) {
			logger.log(Level.FINE, "Scale exception", e);		

			try {	
				request.ignore();
			} catch (Exception ignore) { }
		}
	}

	@Override
	public void handle(AcnetCancel c)
	{
	}

	@Override
	public void run()
	{
		//DPM.Request m;
		AcnetRequest request;

		while ((request = requestQ.poll()) != null) {
			try {
				//((Scaling.Request) request.getObject()).deliverTo(this);
				final Scaling.Request scalingRequest = (Scaling.Request) request.getObject();

				if (scalingRequest instanceof Scaling.Request.ServiceDiscovery)
					handle(request, (Scaling.Request.ServiceDiscovery) scalingRequest);
				else
					handle(request, (Scaling.Request.Scale) scalingRequest);
			} catch (Exception e) {
				logger.log(Level.FINE, "Scale exception", e);		
				sendStatusNoEx(DIO_SCALEFAIL);
			}
		}
	}
/*
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
*/

	synchronized void handle(AcnetRequest request, Scaling.Request.ServiceDiscovery m)
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

	void handle(AcnetRequest request, Scaling.Request.Scale m)
	{
		try {
			if (logger.isLoggable(Level.FINER)) {
				if (m.raw != null)
					logger.log(Level.FINER, "Raw->Scaled Scaling request for '" + m.drf_request + "'");
				else if (m.scaled != null)
					logger.log(Level.FINER, "Scaled->Raw Scaling request for '" + m.drf_request + "'");
				else
					logger.log(Level.FINER, "Bad Scaling request for '" + m.drf_request + "'");
			}

			DeviceCache.add(m.drf_request);

			final WhatDaq whatDaq = new WhatDaq(null, m.drf_request);
			final DataReplier replier = DataReplier.get(whatDaq, this);

			this.request = request;

			if (m.raw != null)
				replier.sendReply(m.raw, 0, 0, 0);
			else if (m.scaled != null)
				replier.sendReply(m.scaled, 0, 0);
			else
				sendStatusNoEx(DIO_SCALEFAIL);
		} catch (AcnetStatusException e) {
			logger.log(Level.FINER, "exception handling scale request", e);
			sendStatusNoEx(e.status);	
		} catch (SQLException e) {
			sendStatusNoEx(SQL_GENERIC_ERROR);	
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
