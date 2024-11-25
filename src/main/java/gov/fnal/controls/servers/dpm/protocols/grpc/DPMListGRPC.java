// $Id: DPMListGRPC.java,v 1.16 2024/11/19 22:34:44 kingc Exp $
package gov.fnal.controls.servers.dpm.protocols.grpc;
 
import java.util.Date;
import java.util.logging.Level;
import java.util.HashMap;
import java.util.Collection;
import java.io.IOException;
import java.net.InetAddress;

import org.ietf.jgss.GSSException;

import gov.fnal.controls.servers.dpm.scaling.DPMBasicStatusScaling;
import gov.fnal.controls.servers.dpm.scaling.DPMAnalogAlarmScaling;
import gov.fnal.controls.servers.dpm.scaling.DPMDigitalAlarmScaling;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.service.proto.grpc.DPMProto.*;

import gov.fnal.controls.servers.dpm.DPMSession;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.DPMList;
import gov.fnal.controls.servers.dpm.SettingData;
import gov.fnal.controls.servers.dpm.SettingStatus;
import gov.fnal.controls.servers.dpm.DataReplier;
import gov.fnal.controls.servers.dpm.DPMProtocolReplier;
import gov.fnal.controls.servers.dpm.protocols.DPMProtocolHandler;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.stub.ServerCallStreamObserver;
import com.google.protobuf.ByteString;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;


public abstract class DPMListGRPC extends DPMList implements AcnetErrors, DPMProtocolReplier, Runnable
{
	class OnCancelHandler implements Runnable
	{
		@Override
		public void run()
		{
			dispose();
		}
	}

	final ServerCallStreamObserver<Reading> replier;

	public DPMListGRPC(DPMProtocolHandler owner, StreamObserver<Reading> replier) throws AcnetStatusException
	{
		super(owner);

		this.replier = (ServerCallStreamObserver<Reading>) replier;
		this.replier.setOnCancelHandler(new OnCancelHandler());
		this.replier.setOnReadyHandler(this);
		this.protocolReplier = this;
		this.owner.listCreated(this);
	}

	abstract void sendReply(WhatDaq whatDaq, Reading.Builder reply);

	@Override
	public InetAddress fromHostAddress()
	{
		return null;
	}

	@Override
    public void sendReply(WhatDaq whatDaq) throws InterruptedException, IOException, AcnetStatusException { }

	@Override
	public void sendReply(WhatDaq whatDaq, DPMAnalogAlarmScaling scaling, long timestamp, long cycle) { }
		
	@Override
	public void sendReply(WhatDaq whatDaq, DPMDigitalAlarmScaling scaling, long timestamp, long cycle) { }

	@Override
	public void sendReply(WhatDaq whatDaq, DPMBasicStatusScaling scaling, long timestamp, long cycle) { }

	@Override
	public void sendReply(WhatDaq whatDaq, byte[] data, long timestamp, long cycle)
	{
		final Reading.Builder reply = Reading.newBuilder().setIndex((int) whatDaq.refId()).setTimestamp(timestamp);
		
		sendReply(whatDaq, reply.setData(Data.newBuilder().setRaw(ByteString.copyFrom(data))));
	}

	@Override
	public void sendReply(WhatDaq whatDaq, boolean value, long timestamp, long cycle)
	{
		final Reading.Builder reply = Reading.newBuilder().setIndex((int) whatDaq.refId()).setTimestamp(timestamp);

		sendReply(whatDaq, reply.setData(Data.newBuilder().setScalar(value ? 1.0 : 0.0)));
	}

	@Override
	public void sendReply(WhatDaq whatDaq, double value, long timestamp, long cycle)
	{
		final Reading.Builder reply = Reading.newBuilder().setIndex((int) whatDaq.refId()).setTimestamp(timestamp);

		sendReply(whatDaq, reply.setData(Data.newBuilder().setScalar(value)));
	}

	@Override
	public void sendReply(WhatDaq whatDaq, double[] values, long timestamp, long cycle)
	{
		final Reading.Builder reply = Reading.newBuilder().setIndex((int) whatDaq.refId()).setTimestamp(timestamp);

		final Data.ScalarArray.Builder arr = Data.ScalarArray.newBuilder();
		
		for (final double value : values)
			arr.addValue(value);

		sendReply(whatDaq, reply.setData(Data.newBuilder().setScalarArr(arr)));
	}

	@Override
	public void sendReply(WhatDaq whatDaq, double[] values, long[] micros, long seqNo)
	{
		final Reading.Builder reply = Reading.newBuilder().setIndex((int) whatDaq.refId());
		final Data.Builder data = Data.newBuilder();

		for (int ii = 0; ii < values.length; ii++) {
			reply.setTimestamp(micros[ii] / 1000);
			data.setScalar(values[ii]);
			reply.setData(data);

			try {
				sendReply(whatDaq, reply);
			} catch (Exception e) {
				e.printStackTrace();
				dispose(0);
				return;
			}
		}
	}

	@Override
	public void sendReply(WhatDaq whatDaq, String value, long timestamp, long cycle)
	{
		final Reading.Builder reply = Reading.newBuilder().setIndex((int) whatDaq.refId()).setTimestamp(timestamp);

		sendReply(whatDaq, reply.setData(Data.newBuilder().setText(value)));
	}

	@Override
	public void sendReply(WhatDaq whatDaq, String[] values, long timestamp, long cycle)
	{
		final Reading.Builder reply = Reading.newBuilder().setIndex((int) whatDaq.refId()).setTimestamp(timestamp);
		final Data.TextArray.Builder arr = Data.TextArray.newBuilder();
		
		for (final String value : values)
			arr.addValue(value);

		sendReply(whatDaq, reply.setData(Data.newBuilder().setTextArr(arr)));
	}

	@Override
	public String clientHostName()
	{
		return "";
	}

	@Override
	public boolean dispose(int status)
	{
		try {
			replier.onCompleted();
		} catch (Exception ignore) { }

		if (super.dispose(status)) {
			return true;
		}
		return false;
	}

	static class MultipleReplies extends DPMListGRPC
	{
		long droppedReplyCount;

		MultipleReplies(DPMProtocolHandler owner, StreamObserver<Reading> replier) throws AcnetStatusException
		{
			super(owner, replier);

			this.droppedReplyCount = 0;
			this.replier.setOnCancelHandler(new OnCancelHandler());
			this.replier.setOnReadyHandler(this);
			this.owner.listCreated(this);
		}

		@Override
		synchronized public void run()
		{
			// Used for the OnReadyHandler()
			notify();
		}

		void sendReply(WhatDaq whatDaq, Reading.Builder reply) 
		{
			if (!replier.isReady()) {
				if (whatDaq.getOption(WhatDaq.Option.FLOW_CONTROL)) {
					try {
						synchronized (this) {
							wait();
						}
					} catch (InterruptedException e) {
						dispose();
					}
				} else {
					logger.log(Level.WARNING, String.format("%s - slow client, dropping replies", id()));
					droppedReplyCounter.incrementAndGet();
					return;
				}
			}
			replyCounter.incrementAndGet();
			replier.onNext(reply.build());	
		}
	}

	static class SingleReply extends DPMListGRPC
	{
		SingleReply(DPMProtocolHandler owner, StreamObserver<Reading> replier) throws AcnetStatusException
		{
			super(owner, replier);
		}

		@Override
		public void run() { }

		@Override
		void sendReply(WhatDaq whatDaq, Reading.Builder reply) 
		{
			replier.onNext(reply.build());	
			replier.onCompleted();
			dispose(0);
		}
	}

	static class Settings extends DPMList implements DPMProtocolReplier, AcnetErrors, Runnable
	{
		final ServerCallStreamObserver<StatusList> replier;

		public Settings(DPMProtocolHandler owner, DPMSession dpmSession, StreamObserver<StatusList> replier) throws AcnetStatusException, GSSException
		{
			super(owner);

			this.replier = (ServerCallStreamObserver<StatusList>) replier;
			this.replier.setOnCancelHandler(this);
			this.protocolReplier = this;
			this.owner.listCreated(this);
		}

		@Override
		public InetAddress fromHostAddress()
		{
			return null;
		}

		@Override
		public void sendReply(Collection<SettingStatus> status)
		{
			final StatusList.Builder statusList = StatusList.newBuilder();

			for (SettingStatus s : status)
				statusList.addStatus(s.status());

			replier.onNext(statusList.build());
			replier.onCompleted();
			
			dispose();
		}

		@Override
		public String clientHostName()
		{
			return "";
		}

		@Override
		public void run()
		{
			dispose();
		}
	}
}
