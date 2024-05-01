// $Id: DPMProtocolHandlerGRPC.java,v 1.11 2024/03/20 17:56:00 kingc Exp $
package gov.fnal.controls.servers.dpm.protocols.grpc;

//import java.util.UUID;
import java.io.IOException;
import java.util.logging.Level;
//import org.ietf.jgss.GSSException;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.service.proto.grpc.DPMProto.*;
import gov.fnal.controls.service.proto.grpc.DPMGrpc.*;
import gov.fnal.controls.service.proto.grpc.DeviceInfoProto.*;
import gov.fnal.controls.servers.dpm.DPMSession;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.DPMRequest;
import gov.fnal.controls.servers.dpm.SettingData;
import gov.fnal.controls.servers.dpm.DPMCredentials;
import gov.fnal.controls.servers.dpm.protocols.Protocol;
import gov.fnal.controls.servers.dpm.protocols.HandlerType;
import gov.fnal.controls.servers.dpm.protocols.DPMProtocolHandler;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

import io.grpc.Grpc;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerInterceptor;
import io.grpc.ServerBuilder;
import io.grpc.ServerCallHandler;
import io.grpc.Metadata;
import io.grpc.stub.StreamObserver;
import com.google.protobuf.Empty;
import io.grpc.stub.ServerCallStreamObserver;

import com.google.protobuf.ByteString;

class IPInterceptor implements ServerInterceptor
{
  	@Override
  	public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
    				String ipAddress = call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR).toString();
    				logger.warning("Client IP address = " + ipAddress);

    				return next.startCall(call, headers);
	}
}


class DPMImpl extends DPMImplBase// implements ServerInterceptor
{
	final DPMProtocolHandler handler;

	DPMImpl(DPMProtocolHandler handler)
	{
		this.handler = handler;
	}

	@Override
	public void readDevice(Device device, StreamObserver<Reading> responseObserver) 
	{
		try {
			final DPMListGRPC dpmList = new DPMListGRPC.SingleReply(handler, responseObserver);

			dpmList.addRequest(device.getName(), 0);
			dpmList.start(null);
		} catch (AcnetStatusException e) {
		}
	}

	@Override
	public void startAcquisition(AcquisitionList list, StreamObserver<Reading> responseObserver)
	{
		if (list.getReqList().size() > 0) {
			try {
				System.out.println("gRPC - startAcquisition() with " + list.getReqList().size() + " device(s)");

				//final DPMSession session = DPMSession.get(list.getSessionId());
				final DPMListGRPC dpmList = new DPMListGRPC.MultipleReplies(handler, responseObserver);
				int ii = 0;

				for (final String request : list.getReqList())
					dpmList.addRequest(request, ii++);

				dpmList.start(null);
			} catch (AcnetStatusException e) {
			//} catch (Exception e) {
			}
		} else {
			responseObserver.onCompleted();
		}
	}

	@Override
	public void getDeviceInfo(AcquisitionList list, StreamObserver<DeviceInfoList> responseObserver)
	{
		//if (list.getReqList().size() > 0) {
			try {
				final DPMSession session = DPMSession.get(list.getSessionId());
				final DeviceInfoList.Builder reply = DeviceInfoList.newBuilder();
				//final DPMListGRPC dpmList = new DPMListGRPC(handler, responseObserver);
				//int ii = 0;

				for (final String request : list.getReqList()) {
					final DeviceInfo.Builder dInfo = DeviceInfo.newBuilder();

					try {
						final WhatDaq whatDaq = new WhatDaq(null, 0, (new DPMRequest(request)));
						//final Lookup.DeviceInfo di = Lookup.getDeviceInfo(request.dReq.getName());

						dInfo.setDi(whatDaq.getDeviceIndex());
						dInfo.setName(whatDaq.getDeviceName());

						reply.addEntry(dInfo);
					} catch (Exception e) {
						reply.addEntry(dInfo);
					}
				}

				responseObserver.onNext(reply.build());
			} catch (AcnetStatusException e) {
			} catch (Exception e) {
			}
		//} else {
			responseObserver.onCompleted();
		//}
	}

	@Override
	public void openSession(Empty empty, StreamObserver<SessionReply> responseObserver)
	{
		final SessionReply.Builder sessionReply = SessionReply.newBuilder();
		
		try {
			final DPMSession session = DPMSession.open();

			((ServerCallStreamObserver) responseObserver).setOnCancelHandler(new Runnable() {
				public void run() 
				{
					session.close();
				}
			});

			sessionReply.setSessionId(session.id());
			sessionReply.setServiceName(DPMCredentials.serviceName());

			responseObserver.onNext(sessionReply.build());
		} catch (Exception e) {
			responseObserver.onCompleted();
		}
	}

	@Override
	public void authenticate(AuthRequest authRequest, StreamObserver<AuthReply> responseObserver)
	{
		final AuthReply.Builder authReply = AuthReply.newBuilder(); 

		try {
			final DPMSession session = DPMSession.get(authRequest.getSessionId());
			final byte[] nextToken = session.authenticateStep(authRequest.getToken().toByteArray());

			if (nextToken != null)
				responseObserver.onNext(authReply.setToken(ByteString.copyFrom(nextToken)).build());
			else {
				final Result.Builder status = Result.newBuilder();

				status.setNone(Empty.newBuilder().build());
				responseObserver.onNext(authReply.setResult(status.build()).build());
			}
		} catch (Exception e) {
			responseObserver.onNext(authReply.setResult(Result.newBuilder().setError(e.getMessage())).build());
		}

		responseObserver.onCompleted();
	}

	@Override
	public void applySettings(SettingList settingList, StreamObserver<StatusList> responseObserver)
	{
		try {
			if (settingList.getSessionId().equals("DEADBEEF")) {
				final DPMListGRPC.Settings list = new DPMListGRPC.Settings(handler, null, responseObserver);

				long refId = 0;

				for (Setting setting : settingList.getSettingList()) {
					list.addRequest(setting.getName(), refId);

					final Data data = setting.getData();

					if (data.hasScalar()) {
						list.addSetting(SettingData.create(data.getScalar(), refId));
						logger.log(Level.FINE, "scalar setting " + setting.getName() + " to " + data.getScalar());
					} else if (data.hasScalarArr()) {
						final Data.ScalarArray arr = data.getScalarArr();
						final double[] values = new double[arr.getValueCount()];

						for (int ii = 0; ii < values.length; ii++)
							values[ii] = arr.getValue(ii);	

						list.addSetting(SettingData.create(values, refId));
					} else if (data.hasText()) {
						list.addSetting(SettingData.create(data.getText(), refId));
						logger.log(Level.FINE, "text setting " + setting.getName() + " to " + data.getText());
					} else if (data.hasTextArr()) {
						final Data.TextArray arr = data.getTextArr();
						final String[] values = new String[arr.getValueCount()];

						for (int ii = 0; ii < values.length; ii++)
							values[ii] = arr.getValue(ii);	

						list.addSetting(SettingData.create(values, refId));
					}

					refId++;
				}

				list.applySettings();
				return;
			}

			final DPMSession session = DPMSession.get(settingList.getSessionId());

			final DPMListGRPC.Settings list = new DPMListGRPC.Settings(handler, session, responseObserver);

			long refId = 0;

			for (Setting setting : settingList.getSettingList()) {
				list.addRequest(setting.getName(), refId);

				final Data data = setting.getData();

				if (data.hasScalar()) {
					list.addSetting(SettingData.create(data.getScalar(), refId));
					logger.fine("setting " + setting.getName() + " to " + data.getScalar());
				} else if (data.hasScalarArr()) {
					final Data.ScalarArray arr = data.getScalarArr();
					final double[] values = new double[arr.getValueCount()];

					for (int ii = 0; ii < values.length; ii++)
						values[ii] = arr.getValue(ii);	

					list.addSetting(SettingData.create(values, refId));
				} else if (data.hasText()) {
					list.addSetting(SettingData.create(data.getText(), refId));
				} else if (data.hasTextArr()) {
					final Data.TextArray arr = data.getTextArr();
					final String[] values = new String[arr.getValueCount()];

					for (int ii = 0; ii < values.length; ii++)
						values[ii] = arr.getValue(ii);	

					list.addSetting(SettingData.create(values, refId));
				}

				refId++;
			}

			list.applySettings();
		} catch (Exception e) {
			//final StatusReply.Builder statusReply = AuthReply.newBuilder(); 

			//responseObserver.onNext(statusReply.setResult(Result.newBuilder().setError(e.getMessage())).build());
		}
	}
}

public class DPMProtocolHandlerGRPC extends DPMProtocolHandler implements Runnable
{
	private final Thread thread; 

	private final int port;
	private final Server server;
	
	public DPMProtocolHandlerGRPC(int port) throws IOException
	{
		this.port = port;
		this.server = ServerBuilder.forPort(port).addService(new DPMImpl(this)).build().start();
		this.thread = new Thread(this);
		this.thread.setName("DPMProtocolHandlerGRPC");
		this.thread.start();
	}

	@Override
	public void logStart()
	{
		logger.info("DPM gRPC service now available on port " + port);
	}

    @Override
    public Protocol protocol()
    {                                                                                                                                                                      
        return Protocol.GRPC;
    }

    @Override
    public HandlerType type()
    {
        return HandlerType.Service;
    }

	@Override
	public void run()
	{
		while (true) {
			try {
				server.awaitTermination();
			} catch (Exception e) {
				logger.log(Level.FINE, "Exception during gGRP server termination", e);
			}
		}
	}
}

