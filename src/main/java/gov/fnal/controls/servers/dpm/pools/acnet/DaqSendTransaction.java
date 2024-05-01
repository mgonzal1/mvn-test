// $Id: DaqSendTransaction.java,v 1.17 2024/04/11 19:18:44 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.ArrayList;
import java.util.logging.Level;
//import java.util.logging.Logger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetInterface;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetReply;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetConnection;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetReplyHandler;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetRequestContext;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.acnetlib.NodeFlags;

import gov.fnal.controls.servers.dpm.pools.WhatDaq;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

abstract class DaqSendTransaction implements NodeFlags, AcnetReplyHandler, AcnetErrors
{
	class ReplyInfo {
		final WhatDaq whatDaq;
		int lastStatus;

		ReplyInfo(WhatDaq whatDaq)
		{
			this.whatDaq = whatDaq;
			this.lastStatus = 0;
		}
	}
	static final ClientReport clientReport = new ClientReport("ERRRPT");
	static final int MaxRetries = 3;

	final DaqRequestList reqList;
	final AcnetConnection acnetConnection;
	volatile AcnetRequestContext context;
	ArrayList<ReplyInfo> replyRequests;
	int transmitCount;
	int retryCount;

	abstract AcnetConnection getConnection();
	abstract ByteBuffer createRequest(ByteBuffer buf);
	abstract String getTaskName();
	abstract void handleReply(ByteBuffer data);

	DaqSendTransaction(DaqRequestList reqList)
	{
		this.reqList = reqList;
		this.acnetConnection = getConnection();
		this.context = new AcnetRequestContext();
		this.transmitCount = 0;
		this.retryCount = 0;
	}

	void cancel()
	{
		context.cancelNoEx();
	}

	synchronized void transmit()
	{
		context.cancelNoEx();

		replyRequests = new ArrayList<>(reqList.requests.size());

		for (WhatDaq whatDaq : reqList.requests)
			replyRequests.add(new ReplyInfo(whatDaq));

		transmitCount++;

		try {
			//final ByteBuffer buf = ByteBuffer.allocate(reqList.daqDefs.requestSize(reqList.requests.size())).order(ByteOrder.LITTLE_ENDIAN);
			final ByteBuffer buf = ByteBuffer.allocate(reqList.reqSize).order(ByteOrder.LITTLE_ENDIAN);

			context = acnetConnection.sendRequest(reqList.node.value(), getTaskName(), reqList.event.isRepetitive(), 
													createRequest(buf), (int) reqList.timeout, this);
		} catch (AcnetStatusException e) {
			logger.log(Level.FINE, "DaqSendTransaction.transmit exception", e); 
			context.cancelNoEx();
		} catch (Exception e) {
			logger.log(Level.WARNING, "DaqSendTransaction.transmit exception", e); 
			context.cancelNoEx();
		}
	}

	boolean isTimeout(AcnetReply r)
	{
		final int s = r.status();

		//return s == ACNET_REQTMO || s == ACNET_PEND || s == ACNET_REPLY_TIMEOUT;
		return s == ACNET_REQTMO || s == ACNET_REPLY_TIMEOUT;
	}

	void handleTimeout()
	{
		completeWithStatus(ACNET_UTIME);

		if (retryCount < MaxRetries) {
			retryCount++;

			if (!reqList.node.is(OUT_OF_SERVICE) && !reqList.node.is(OBSOLETE)) {
				//logger.log(Level.FINE, reqList.node + " " + reqList.event + ",rpySize: " + reqList.replySize() +
				//							", retransmitting msg " + retryCount);
			}

			transmit();
		}
	}

	@Override
	public void handle(AcnetReply r)
	{
		if (isTimeout(r)) {
			handleTimeout();
			return;
		}

		if (r.status() == ACNET_NO_TASK) {
			completeWithStatus(ACNET_NODE_DOWN);
			return;
		}

		if (r.status() == ACNET_PEND)
			return;

		if (r.status() != 0) {
			completeWithStatus(r.status());
			return;
		}

		retryCount = 0;

		final ByteBuffer data = r.data().order(ByteOrder.LITTLE_ENDIAN);

		//System.out.println("status: " + r.status() + " len: " + r.length() + " ev: " + reqList.event + " ftd: " + reqList.event.ftd());
		//System.out.println("rem: " + data.remaining() + " ryqsz: " + reqList.replySize());

		if (data.remaining() == reqList.replySize())
			handleReply(data);
		else {
			//while (data.remaining() > 0)
			//	System.out.printf("%02x ", data.get());

			//System.out.println();

			completeWithStatus(ACNET_TRUNC_REPLY);
			context.cancelNoEx();
		}
	}

	synchronized final void completeWithStatus(int status)
	{
		for (ReplyInfo rInfo : replyRequests) {
			final WhatDaq whatDaq = rInfo.whatDaq;
			
			if (retryCount == 0) {
				whatDaq.getReceiveData().receiveStatus(status);
				clientReport.reportError(whatDaq.dipi(), status, reqList.isSetting);
			}
		}

		reqList.listCompletion.completed(status);
	}

 	@Override
	public String toString()
	{
		return "DaqSendTransaction: " + reqList.node
				+ reqList.requests.size() + "\n\treplySize=" + reqList.replySize()
				+ " retryCount=" + retryCount;

	}
}

class DaqSendTransaction16 extends DaqSendTransaction
{
	static final AcnetConnection connection = AcnetInterface.open("GETS16");

	DaqSendTransaction16(DaqRequestList reqList)
	{
		super(reqList);
	}

	@Override
	AcnetConnection getConnection()
	{
		return connection;
	}

	@Override
	String getTaskName()
	{
		return reqList.isSetting ? "SETDAT" : "RETDAT";
	}

	@Override
	ByteBuffer createRequest(ByteBuffer buf)
	{
		if (reqList.isSetting) {
			buf.putShort((short) 1);
			buf.putShort((short) reqList.requests.size());
		} else {
			buf.putShort((short) reqList.replySize()); 
			buf.putShort((short) reqList.requests.size());
			buf.putShort((short) reqList.event.ftd());
		}

		for (WhatDaq whatDaq : reqList.requests) {
			buf.putInt(whatDaq.dipi());
			buf.put(whatDaq.ssdn());
			buf.putShort((short) whatDaq.length());
			buf.putShort((short) whatDaq.offset());

			if (reqList.isSetting) {
				buf.put(whatDaq.setting(), 0, whatDaq.length());
		
				if ((whatDaq.length() & 1) > 0)
					buf.put((byte) 0);
			}
		}

		buf.flip();

		//logger.log(Level.FINER, "DaqSendTransaction16: bufSize:" + buf.remaining());

		return buf;
	}

	@Override
	void handleReply(ByteBuffer data)
	{
		final long now = System.currentTimeMillis();
		int completedStatus = 0;

		for (ReplyInfo rInfo : replyRequests) {
			final WhatDaq whatDaq = rInfo.whatDaq;
			final int status = data.getShort();

			if (status != 0)
				completedStatus = status;

			if (reqList.isSetting)
				whatDaq.getReceiveData().receiveStatus(status, now, 0);
			else {
				final int pos = data.position();

				if (status == 0)
					whatDaq.getReceiveData().receiveData(data, now, 0);
				else
					whatDaq.getReceiveData().receiveStatus(status, now, 0);

				data.position(pos + whatDaq.getLength());

				if (whatDaq.lengthIsOdd())
					data.get();
			}

			if (status != 0 && status != rInfo.lastStatus)
				clientReport.reportError(whatDaq.dipi(), status, reqList.isSetting);

			rInfo.lastStatus = status;
		}

		reqList.listCompletion.completed(completedStatus);
	}
}

class DaqSendTransaction32 extends DaqSendTransaction //implements NodeFlags, AcnetReplyHandler, AcnetErrors//, Daq32Defs
{
	static final AcnetConnection connectionR = AcnetInterface.open("GETSR").setQueueReplies();
	static final AcnetConnection connection1 = AcnetInterface.open("GETS1").setQueueReplies();
	static final AcnetConnection connectionS = AcnetInterface.open("GETSS").setQueueReplies();

	//int returnCount = 0;

	DaqSendTransaction32(DaqRequestList reqList)
	{
		super(reqList);
		//this.reqList = reqList;
		//this.context = new AcnetRequestContext();

	}

	@Override
	AcnetConnection getConnection()
	{
		if (reqList.isSetting)
			return connectionS;
		else if (!reqList.event.isRepetitive())
			return connection1;
		
		return connectionR;
	}
	
	@Override
	String getTaskName()
	{
		return reqList.isSetting ? "SETS32" : "GETS32";
	}

	@Override
	ByteBuffer createRequest(ByteBuffer buf)
	{
		//final ByteBuffer buf = ByteBuffer.allocate(daqDefs.requestSize(reqList.requests.size())).order(ByteOrder.LITTLE_ENDIAN);

		buf.put((byte) 1).put((byte) 1).put((byte) 0);
		buf.put((byte) (reqList.node.value() >> 8));
		buf.put((byte) (reqList.node.value() & 0xff));
		buf.put((byte) (reqList.isSetting ? 1 : 0));
		buf.put((byte) 0);
		buf.put((byte) (reqList.event.isRepetitive() ? 1 : 0));
		buf.putInt(reqList.replySize());
		buf.putShort((short) reqList.requests.size());
		buf.putShort((short) reqList.event.ftd());

		final byte[] eventBytes = reqList.event.toString().getBytes();
		final int lsb = eventBytes.length & 1;

		buf.putShort((short) (eventBytes.length + lsb));

		buf.put(eventBytes);
		if (lsb > 0)
			buf.put((byte) ' ');

		for (WhatDaq whatDaq : reqList.requests) {
			buf.putInt(whatDaq.dipi());
			buf.put(whatDaq.ssdn());
			buf.putInt(whatDaq.length());
			buf.putInt(whatDaq.offset());

			if (reqList.isSetting) {
				buf.put(whatDaq.setting(), 0, whatDaq.length());
		
				if ((whatDaq.length() & 1) > 0)
					buf.put((byte) 0);
			}
		}

		buf.flip();

		//logger.log(Level.FINER, "DaqSendTransaction32: bufSize:" + buf.remaining());

		return buf;
	}

	@Override
	// public void handle(AcnetReply r)
	void handleReply(ByteBuffer data)
	{
	/*
		if (isTimeout(r)) {
			replyTimeout();
			return;
		}

		if (r.status() == ACNET_NO_TASK) {
			completeWithStatus(ACNET_NODE_DOWN);
			return;
		}	

		final ByteBuffer data = r.data().order(ByteOrder.LITTLE_ENDIAN);
		*/


		//if (data.remaining() == reqList.replySize()) {
			final int globalStatus = data.getShort();
			int completedStatus = 0;

			data.getLong();
		
			final long cycleTS = data.getLong();
			final long collectTS = data.getLong();
			final long replyTS = data.getLong();

			for (ReplyInfo rInfo : replyRequests) {
				final WhatDaq whatDaq = rInfo.whatDaq;
				final int status = data.getShort();

				if (status != 0)
					completedStatus = status;

				if (reqList.isSetting) {
					whatDaq.getReceiveData().receiveStatus(status == 0 ? globalStatus : status, collectTS, cycleTS);
				} else {
					final int pos = data.position();

					if (status != 0)
						whatDaq.getReceiveData().receiveStatus(status, collectTS, cycleTS);
					else if (globalStatus != 0)
						whatDaq.getReceiveData().receiveStatus(globalStatus, collectTS, cycleTS);
					else
						whatDaq.getReceiveData().receiveData(data, collectTS, cycleTS);

					data.position(pos + whatDaq.getLength());

					if (whatDaq.lengthIsOdd())
						data.get();
				}

				if (status != 0 && status != rInfo.lastStatus)
					clientReport.reportError(whatDaq.dipi(), status, reqList.isSetting);

				rInfo.lastStatus = status;
			}

			reqList.listCompletion.completed(completedStatus);
		//} else {
		//	completeWithStatus(ACNET_TRUNC_REPLY);
		//	context.cancelNoEx();
		//}
	}
}
