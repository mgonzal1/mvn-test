// $Id: FTPPoolImpl.java,v 1.17 2024/03/27 20:59:23 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.Set;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.nio.ByteBuffer;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.acnetlib.Node;

import gov.fnal.controls.servers.dpm.events.DataEvent;
import gov.fnal.controls.servers.dpm.events.DeltaTimeEvent;
import gov.fnal.controls.servers.dpm.SettingData;
import gov.fnal.controls.servers.dpm.scaling.Scaling;
import gov.fnal.controls.servers.dpm.scaling.ScalingFactory;

import gov.fnal.controls.servers.dpm.pools.PoolUser;
import gov.fnal.controls.servers.dpm.pools.PoolType;
import gov.fnal.controls.servers.dpm.pools.PoolInterface;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.pools.ReceiveData;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

public class FTPPoolImpl implements PoolInterface, SettingData.Handler, AcnetErrors
{
	private final Vector<WhatDaq> whatDaqCC = new Vector<>(1);
	private final ArrayList<WhatDaq> requests = new ArrayList<>();
	private final Set<FTPPool> ftpPools = new HashSet<>();
	private final AcnetPoolImpl pool = new AcnetPoolImpl();
	private final PoolUser user = new PoolUser() { };
	
	private ClassCode getClassCode(WhatDaq whatDaq)
	{
		whatDaqCC.clear();
		whatDaqCC.add(whatDaq);
		List<ClassCode> c = PlotClassCode.getPlotClassCode(whatDaqCC);

		return c.get(0);		
	}

	public static void init()
	{
	}

	@Override
	public PoolType type()
	{
		return PoolType.ACNET;
	}

	public static void nodeUp(Node node)
	{
	}

	@Override
	public void addRequest(WhatDaq whatDaq)
	{
		try {
			final ClassCode classCode = getClassCode(whatDaq);
			final FTPScope scope = new FTPScope(whatDaq.getEventFrequency());
			final ReceiveData callback = whatDaq.getReceiveData();

			logger.fine(" CLASSCODE: " + classCode + " scope: " + scope);

			if (classCode.ftpFromPool()) {
				whatDaq.setEvent(scope.ftpCollectionEvent(classCode.maxContinuousRate()));
				whatDaq.setReceiveData(new ReceivePoolPlotData(callback, whatDaq));
				pool.addRequest(whatDaq);
			} else {
				final FTPRequest ftpReq = new FTPRequest(whatDaq, classCode, scope, callback);
				final FTPPool ftpPool = FTPPool.get(ftpReq);

				whatDaq.setUser(user);
				ftpPool.insert(ftpReq);
				ftpPools.add(ftpPool);
			}
		} catch (Exception e) {
			whatDaq.getReceiveData().receiveStatus(ACNET_NXE, System.currentTimeMillis(), 0);
		}

		requests.add(whatDaq);
	}

	@Override
	public void addSetting(WhatDaq whatDaq, SettingData setting) throws AcnetStatusException
	{
		setting.deliverTo(whatDaq, this);
	}

	@Override
	public void handle(WhatDaq whatDaq, byte[] setting)
	{
		try {
			whatDaq.setSetting(setting);
			requests.add(whatDaq);
		} catch (AcnetStatusException e) {
			whatDaq.getReceiveData().receiveStatus(e.status, System.currentTimeMillis(), 0);
		} catch (Exception e) {
			whatDaq.getReceiveData().receiveStatus(ACNET_NXE, System.currentTimeMillis(), 0);
		}
	}

	@Override
	public void handle(WhatDaq whatDaq, double setting)
	{
		try {
			//whatDaq.setSetting(setting);
			whatDaq.setSetting(ScalingFactory.get(whatDaq).unscale(setting, whatDaq.length()));
			requests.add(whatDaq);
		} catch (AcnetStatusException e) {
			whatDaq.getReceiveData().receiveStatus(e.status, System.currentTimeMillis(), 0);
		} catch (Exception e) {
			whatDaq.getReceiveData().receiveStatus(DIO_SCALEFAIL, System.currentTimeMillis(), 0);
		}
	}

	@Override
	public void handle(WhatDaq whatDaq, double[] setting)
	{
		try {
			//whatDaq.setSettingData(setting);
			whatDaq.setSetting(ScalingFactory.get(whatDaq).unscale(setting, whatDaq.defaultLength()));
			requests.add(whatDaq);
		} catch (AcnetStatusException e) {
			whatDaq.getReceiveData().receiveStatus(e.status, System.currentTimeMillis(), 0);
		} catch (Exception e) {
			whatDaq.getReceiveData().receiveStatus(DIO_SCALEFAIL, System.currentTimeMillis(), 0);
		}
	}

	@Override
	public void handle(WhatDaq whatDaq, String setting)
	{
		try {
			//whatDaq.setSettingData(setting);
			whatDaq.setSetting(ScalingFactory.get(whatDaq).unscale(setting, whatDaq.length()));
			requests.add(whatDaq);
		} catch (AcnetStatusException e) {
			whatDaq.getReceiveData().receiveStatus(e.status, System.currentTimeMillis(), 0);
		} catch (Exception e) {
			whatDaq.getReceiveData().receiveStatus(DIO_SCALEFAIL, System.currentTimeMillis(), 0);
		}
	}

	@Override
	public void handle(WhatDaq whatDaq, String[] setting)
	{
		try {
			//whatDaq.setSettingData(setting);
			whatDaq.setSetting(ScalingFactory.get(whatDaq).unscale(setting, whatDaq.defaultLength()));
			requests.add(whatDaq);
		} catch (AcnetStatusException e) {
			whatDaq.getReceiveData().receiveStatus(e.status, System.currentTimeMillis(), 0);
		} catch (Exception e) {
			whatDaq.getReceiveData().receiveStatus(DIO_SCALEFAIL, System.currentTimeMillis(), 0);
		}
	}

	@Override
	public void processRequests()
	{
		for (FTPPool p : ftpPools)  {
			p.process(true);
		}

		pool.processRequests();
	}

	@Override
	public void cancelRequests()
	{
		for (FTPPool p : ftpPools) {
			p.cancel(user, 0);
			p.process(true);
		}

		pool.cancelRequests();
	}

	@Override
	public ArrayList<WhatDaq> requests()
	{
		return requests;
	}

	@Override
	public String dumpPool()
	{
		return "";
	}

	private class ReceivePoolPlotData implements ReceiveData, AcnetErrors
	{
		final ReceiveData callback;
		final WhatDaq whatDaq;
		final long micros[] = new long[128];
		final double values[] = new double[128];
		final Scaling scaling;
		//final DPMReadSetScaling scaling;

		int pointCount = 0;
		long lastSend = System.currentTimeMillis();

		ReceivePoolPlotData(ReceiveData callback, WhatDaq whatDaq)
		{
			this.callback = callback;
			this.whatDaq = whatDaq;
			//this.scaling = DPMReadSetScaling.get(whatDaq);
			this.scaling = ScalingFactory.get(whatDaq);
		}

		@Override
		public void receiveData(ByteBuffer buf, long timestamp, long cycle)
		{
			final long now = System.currentTimeMillis();

			try {
				micros[pointCount] = timestamp * 1000;
				//values[pointCount] = whatDaq.getScaled(data, offset); 
				//values[pointCount] = scaling.rawToCommon(data, offset); 
				//byte[] data = new byte[whatDaq.length()];

				values[pointCount] = scaling.scale(buf); 

				pointCount++;

				if (pointCount == micros.length || (now - lastSend) >= 335) {
					//callback.plotData(whatDaq, micros[0] / 1000, context, error, 
					//					pointCount, Arrays.copyOf(micros, pointCount), null, 
					//					Arrays.copyOf(values, pointCount));
					callback.plotData(micros[0] / 1000, 0, pointCount, Arrays.copyOf(micros, pointCount), null, Arrays.copyOf(values, pointCount));
					lastSend = now;
					pointCount = 0;
				}
			} catch (AcnetStatusException e) {
				//callback.plotData(null, System.currentTimeMillis(), null, e.status, 0, null, null, null);
				callback.plotData(now, e.status, 0, null, null, null);
			} catch (Exception e) {
				callback.plotData(now, ACNET_NXE, 0, null, null, null);
				logger.log(Level.WARNING, "exception delivering plot data", e);
			}
		}

		@Override
		public void receiveStatus(int status, long timestamp, long cycle)
		{
			callback.plotData(timestamp, status, 0, null, null, null);
		}

		@Override
		//public void receiveData(WhatDaq whatDaq, int error, int offset, byte[] data, 
		//							long timestamp, CollectionContext context)
		//public void receiveData(int error, int offset, byte[] data, long timestamp)
		public void receiveData(byte[] data, int offset, long timestamp, long cycle)
		{
			final long now = System.currentTimeMillis();

			try {
				micros[pointCount] = timestamp * 1000;
				//values[pointCount] = whatDaq.getScaled(data, offset); 
				//values[pointCount] = scaling.rawToCommon(data, offset); 
				values[pointCount] = scaling.scale(data, offset); 

				pointCount++;

				if (pointCount == micros.length || (now - lastSend) >= 335) {
					//callback.plotData(whatDaq, micros[0] / 1000, context, error, 
					//					pointCount, Arrays.copyOf(micros, pointCount), null, 
					//					Arrays.copyOf(values, pointCount));
					callback.plotData(micros[0] / 1000, 0, pointCount, Arrays.copyOf(micros, pointCount), null, Arrays.copyOf(values, pointCount));
					lastSend = now;
					pointCount = 0;
				}
			} catch (AcnetStatusException e) {
				//callback.plotData(null, System.currentTimeMillis(), null, e.status, 0, null, null, null);
				callback.plotData(now, e.status, 0, null, null, null);
			} catch (Exception e) {
				callback.plotData(now, ACNET_NXE, 0, null, null, null);
				logger.log(Level.WARNING, "exception delivering plot data", e);
			}
		}
	}
}
