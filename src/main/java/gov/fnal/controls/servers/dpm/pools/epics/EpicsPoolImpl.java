// $Id: EpicsPoolImpl.java,v 1.11 2024/03/27 21:16:40 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.epics;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.time.Instant;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.acnetlib.Node;

import gov.fnal.controls.service.proto.DPM;

import gov.fnal.controls.servers.dpm.SettingData;
import gov.fnal.controls.servers.dpm.pools.ReceiveData;
import gov.fnal.controls.servers.dpm.pools.PoolType;
import gov.fnal.controls.servers.dpm.pools.PoolInterface;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.scaling.ScalingFactory;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

import org.epics.pva.client.PVAChannel;
import org.epics.pva.data.PVANumber;
import org.epics.pva.data.PVAStructure;
import org.epics.pva.data.PVAStructures;
import org.epics.pva.data.PVADoubleArray;

public class EpicsPoolImpl implements PoolInterface, SettingData.Handler, AcnetErrors
{
	private enum NormativeType
	{
		NTScalar("epics:nt/NTScalar:1.0"),
		NTScalarArray("epics:nt/NTScalarArray:1.0"),
		NTEnum("epics:nt/NTEnum:1.0"),
		NTAcnetDevice("fermi:nt/AcnetDevice:1.0"),
		NTAcnetStatus("fermi:nt/NTAcnetStatus:1.0"),
		NTBlank(""),
		NTUnknown("unknown");

		final String name;

		NormativeType(String name)
		{
			this.name = name;
		}

		private static HashMap<String, NormativeType> types = new HashMap<>();

		static {
			for (NormativeType t : NormativeType.values())
				types.put(t.name, t);
		}

		public static NormativeType get(String name)
		{
			NormativeType t = types.get(name);

			return t == null ? NormativeType.NTUnknown : t;
		}
	}

	private class PVListener implements EpicsListener, AcnetErrors
	{
		private final WhatDaq whatDaq;
		private boolean printedStack = false;

		PVListener(WhatDaq whatDaq)
		{
			this.whatDaq = whatDaq;
		}

		private void receiveAcnetDevice(ReceiveData r, PVAStructure data)
		{
			switch (whatDaq.property) {
			 case READING:
			 	{
					final PVAStructure reading = data.get("Reading");	

					r.receiveData(((PVADoubleArray) reading.get("value")).get(), getTimestampMillis(data), 0);
				}
				break;

			 case SETTING:
			 	{
					final PVAStructure setting = data.get("Setting");

					r.receiveData(((PVANumber) setting.get("value")).getNumber().doubleValue(), getTimestampMillis(data), 0);
				}
				break;

			 case STATUS:
			 	{
					final PVAStructure basicStatus = data.get("BasicStatus");

					r.receiveData(((PVANumber) basicStatus.get("value")).getNumber().intValue(), getTimestampMillis(data), 0);
				}
				break;

			 case ANALOG:
			 	{
					r.receiveStatus(DIO_NOTYET, System.currentTimeMillis(), 0);
				}
				break;

			 case DIGITAL:
			 	{
					r.receiveStatus(DIO_NOTYET, System.currentTimeMillis(), 0);
				}
				break;
			}
		}

		private final long getTimestampMillis(PVAStructure data)
		{
			try {
				data = data.get("timeStamp");

				final Number sec = ((PVANumber) data.get("secondsPastEpoch")).getNumber();
				final Number nsec = ((PVANumber) data.get("nanoseconds")).getNumber();

				return Instant.ofEpochSecond(sec.longValue(), nsec.longValue()).toEpochMilli();
			} catch (Exception ignore) { }

			return System.currentTimeMillis();
		}
		
		public void handleData(PVAChannel channel, BitSet changes, BitSet overruns, PVAStructure data)
		{
			final ReceiveData r = whatDaq.getReceiveData();

			try {
				switch (NormativeType.get(data.getStructureName())) {
				 case NTAcnetDevice:
					receiveAcnetDevice(r, data);
					break;

				 case NTAcnetStatus:
					r.receiveStatus(((PVANumber) data.get("value")).getNumber().intValue(), 
										System.currentTimeMillis(), 0);
					break;

				 case NTScalar:
					r.receiveData(((PVANumber) data.get("value")).getNumber().doubleValue(), getTimestampMillis(data), 0);
					break;

				 case NTEnum:
					r.receiveEnum(PVAStructures.getEnum(data.get("value")), 
									((PVANumber) ((PVAStructure) data.get("value")).get("index")).getNumber().intValue(), 
									getTimestampMillis(data), 0);
					break;

				 case NTBlank:
					receiveAcnetDevice(r, data);
					break;

				 default:
					r.receiveStatus(DBM_NOPROP, System.currentTimeMillis(), 0);
					break;
				}
			} catch (Exception e) {
				r.receiveStatus(DIO_INCOMPLETE_DATA, System.currentTimeMillis(), 0);
				if (!printedStack) {
					printedStack = true;
					logger.log(Level.WARNING, "exception handling data for " + whatDaq, e);
				}
			}
		}
	}

	public static void init()
	{
	}

	public static void nodeUp(Node node)
	{
	}

	private class PutListener extends PVListener
	{
		PutListener(WhatDaq whatDaq)
		{
			super(whatDaq);
		}
	}

	private final ArrayList<EpicsRequest> requests = new ArrayList<>(512);

	public EpicsPoolImpl()
	{
		// Remove EPICS beacon warnings

		final Filter filter = new Filter() {
			@Override
			public boolean isLoggable(LogRecord record)
			{
				return !"handleBeacon".equals(record.getSourceMethodName());
			}
		};

       	Logger.getLogger("org.epics.pva").setFilter(filter) ;
	}

	@Override
	public void addRequest(WhatDaq whatDaq)
	{
		try {
			requests.add(new EpicsRequest(whatDaq.daqName, new PVListener(whatDaq)));
		} catch (AcnetStatusException e) {
			whatDaq.getReceiveData().receiveStatus(e.status, System.currentTimeMillis(), 0);
		}
	}

	@Override
	public PoolType type()
	{
		return PoolType.EPICS;
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
			//final Object newValue = whatDaq.getScaling().scale(setting, 0, setting.length);
			final Object newValue = ScalingFactory.get(whatDaq).scale(setting, 0);
			final EpicsRequest eReq = new EpicsRequest(whatDaq.daqName, new PutListener(whatDaq), newValue);
			
			requests.add(eReq);
		} catch (AcnetStatusException e) {
			whatDaq.getReceiveData().receiveStatus(e.status, System.currentTimeMillis(), 0);
		}
	}

	@Override
	public void handle(WhatDaq whatDaq, double setting)
	{
		try {
			final EpicsRequest eReq = new EpicsRequest(whatDaq.daqName, new PutListener(whatDaq), new Double(setting));
		
			requests.add(eReq);
		} catch (AcnetStatusException e) {
			whatDaq.getReceiveData().receiveStatus(e.status, System.currentTimeMillis(), 0);
		}
	}

	@Override
	public void handle(WhatDaq whatDaq, double[] setting)
	{
		try {
			final EpicsRequest eReq = new EpicsRequest(whatDaq.daqName, new PutListener(whatDaq), new Double(setting[0]));
		
			requests.add(eReq);
		} catch (AcnetStatusException e) {
			whatDaq.getReceiveData().receiveStatus(e.status, System.currentTimeMillis(), 0);
		}
	}

	@Override
	public void handle(WhatDaq whatDaq, String setting)
	{
		try {
			final EpicsRequest eReq = new EpicsRequest(whatDaq.daqName, new PutListener(whatDaq), setting);

			requests.add(eReq);
		} catch (AcnetStatusException e) {
			whatDaq.getReceiveData().receiveStatus(e.status, System.currentTimeMillis(), 0);
		}
	}

	@Override
	public void handle(WhatDaq whatDaq, String[] setting)
	{
		try {
			final EpicsRequest eReq = new EpicsRequest(whatDaq.daqName, new PutListener(whatDaq), setting[0]);

			requests.add(eReq);
		} catch (AcnetStatusException e) {
			whatDaq.getReceiveData().receiveStatus(e.status, System.currentTimeMillis(), 0);
		}
	}

	@Override
	public void processRequests()
	{
		for (EpicsRequest r : requests) {
			if (r.newValue != null)
				PVAPool.put(r);
			else if (r.isSingleReply)
				PVAPool.get(r);
			else
				PVAPool.subscribe(r);
		}
	}

	@Override
	public void cancelRequests()
	{
		for (EpicsRequest r : requests)
			PVAPool.unsubscribe(r);

		requests.clear();
	}

	@Override
	public ArrayList<WhatDaq> requests()
	{
		ArrayList<WhatDaq> requests = new ArrayList<>();

		for (EpicsRequest r : this.requests)
			requests.add(((PVListener) r.listener).whatDaq);

		return requests;
	}

	@Override
	public String dumpPool()
	{
		return PVAPool.dump();
	}
}

